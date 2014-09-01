package org.marekasf.ramlvalidation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.collections4.list.SetUniqueList;
import org.raml.model.Raml;
import org.raml.model.Resource;
import org.raml.parser.visitor.RamlDocumentBuilder;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import com.google.common.collect.ImmutableSet;

public class RamlParser extends Verticle
{
	// uri : method : attr : set<value>
	private final Map<String, Map<String, Map<String, SetUniqueList<String>>>> collector = new HashMap<>();

	private String ramlResource;

	private boolean ramlLoaded = false;

	@Override
	public void start()
	{
		vertx.eventBus().registerHandler("restart", this::restart);
		vertx.eventBus().registerHandler("raml_log", this::report);
		vertx.eventBus().registerHandler("raml_resources", this::resourcesList);

		ramlResource = getContainer().config().getString("raml.resource",
				"http://localhost:8080/api-console/raml/api/document-repository-service.raml");

	}

	private void resourcesList(final Message<Boolean> message)
	{
		vertx.runOnContext(event -> {
			loadRaml();
			message.reply(new JsonArray(new LinkedList<>(collector.keySet())));
		});
	}

	private void restart(final Message<Boolean> e)
	{
		collector.clear();
		ramlLoaded = false;
	}

	public Map loadRaml()
	{
		if (!ramlLoaded)
		{

			final Raml raml = new RamlDocumentBuilder().build(ramlResource);

			processResources(raml.getResources().values().stream());

			ImmutableSet.copyOf(collector.keySet()).stream().filter(k -> collector.get(k).isEmpty()).forEach(collector::remove);

			ramlLoaded = true;
		}
		return collector;
	}

	private void processResources(final Stream<Resource> resourceStream)
	{
		resourceStream.forEach(r -> {
			register(r);
			if (r.getResources() != null && !r.getResources().isEmpty())
			{
				processResources(r.getResources().values().stream());
			}
		});
	}

	private void register(final Resource resource)
	{
		final Map<String, ?> methods = getMap(collector, resource.getUri());

		final List<String> rIs = resource.getIs();

		resource.getActions().values().stream().forEach(a -> {
			final Map<String, ?> attributes = getMap(methods, a.getType().toString());

			// a.getIs().forEach(i -> addValue(attributes, "request.is." + i, "true"));
			// rIs.forEach(i -> addValue(attributes, "request.is." + i, "true"));

			a.getQueryParameters().entrySet().forEach(q -> addValue(attributes, "request.query." + q.getKey(),
					q.getValue().getExample(), q.getValue().getDefaultValue()));

			a.getResponses().entrySet().forEach(r -> {
				addValue(attributes, "response.status", r.getKey());
				r.getValue().getHeaders().entrySet().forEach(h -> addValue(attributes, "response.header." + h.getKey(),
						h.getValue().getExample(), h.getValue().getDefaultValue()));
			});

			a.getHeaders().entrySet().forEach(h -> addValue(attributes, "request.header." + h.getKey(),
					h.getValue().getExample(),
					h.getValue().getDefaultValue()));
		});
	}

	private void addValue(final Map map, final String attr, final String... val)
	{
		if (!map.containsKey(attr))
		{
			map.put(attr, SetUniqueList.setUniqueList(new LinkedList<>()));
		}
		Arrays.stream(val).filter(Objects::nonNull).forEach(v -> ((List) map.get(attr)).add(v.trim()));
	}

	private Map<String, ?> getMap(final Map<String, ?> map, final String key)
	{
		if (!map.containsKey(key))
		{
			((Map) map).put(key, new HashMap<>());
		}
		return (Map) map.get(key);
	}

	private void report(final Message e)
	{
		vertx.runOnContext(event -> {
			loadRaml();
			e.reply(new JsonObject((Map) collector));
		});
	}

	public void setRamlResource(final String ramlResource)
	{
		this.ramlResource = ramlResource;
	}
}
