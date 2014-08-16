package org.marekasf.ramlvalidation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.list.SetUniqueList;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import javafx.util.Pair;

public class Collector extends Verticle
{
	// uri : method : attr : set<value>
	private Map<String, Map<String, Map<String, SetUniqueList<String>>>> collector;

	@Override
	public void start()  {

		collector = new HashMap<>();

		vertx.eventBus().registerHandler("restart", this::restart);
		vertx.eventBus().registerHandler("proxy_report", this::report);
		vertx.eventBus().registerHandler("record", this::record);
		vertx.eventBus().registerHandler("proxy_report_merge", this::merge);
	}

	private void merge(final Message<List<String>> message)
	{
		final List<String> ramlAttributes = message.body();

		final List<ImmutablePair<String, Pattern>> patterns = ramlAttributes.stream() //
				.map(a ->
						ImmutablePair.of(a, Pattern.compile(a.replaceAll("\\{\\}", "")))).collect(Collectors.toList());

		Map tmp = (Map) collector.keySet().stream()
				.map(k ->  patterns.stream()
								.map(p -> p.getValue().matcher(k).matches() ? ImmutablePair.of(k, p.getKey()) : null).findFirst())
				.filter(Objects::nonNull)
				.filter(o -> o.isPresent())
				.map(o -> o.get())
				.map(o -> !o.getKey().equals(o.getValue()))
				.reduce(new HashMap(),
						(Map acc, ImmutablePair v) -> merge(acc, v.getKey().toString(), v.getValue().toString()),
						(Map acc1, Map acc2) -> {acc1.putAll(acc2); return acc1;}
					   );

		collector = tmp;
	}

	private Map merge(final Map acc, final String key, final String ramlKey)
	{
		if (!acc.containsKey(ramlKey))
		{
			acc.put(ramlKey, collector.get(key));
		}
		else
		{
			// FIXME MERGE !!!
		}

		return acc;
	}

	private void record(final Message<JsonObject> e)
	{
		final JsonObject body = e.body();

		final Map attributeMap = (Map)getMap(getMap(collector, body.getString("uri")), body.getString("method"));
		final JsonObject attributes = body.getObject("attribute");

		attributes.getFieldNames().stream().filter(attr -> !attr.endsWith(".body"))
				.forEach(attr -> getSet(attributeMap, attr).add(attributes.getValue(attr).toString()));
	}

	private SetUniqueList<String> getSet(final Map<String, SetUniqueList<String>> map, final String attr)
	{
		if (!map.containsKey(attr)) {
			map.put(attr, SetUniqueList.setUniqueList(new LinkedList<>()));
		}
		return map.get(attr);
	}

	private Map<String, ?> getMap(final Map<String, ?> map, final String key)
	{
		if (!map.containsKey(key)) {
			((Map)map).put(key, new HashMap<>());
		}
		return (Map) map.get(key);
	}

	private void report(final Message e)
	{
		e.reply(new JsonObject((Map) collector));
	}

	private void restart(final Message<Boolean> e)
	{
		collector.clear();
	}


}
