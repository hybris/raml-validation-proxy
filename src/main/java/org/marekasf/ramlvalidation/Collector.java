package org.marekasf.ramlvalidation;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.list.SetUniqueList;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class Collector extends Verticle
{
	// uri : method : attr : set<value>
	private Map<String, Map<String, Map<String, SetUniqueList<String>>>> _collector;

	@Override
	public void start()
	{

		_collector = (Map) UriKeyMap.urlOnly();

		vertx.eventBus().registerHandler("restart", this::restart);
		vertx.eventBus().registerHandler("proxy_log", this::report);
		vertx.eventBus().registerHandler("record", this::record);
	}

	private void report(final Message e)
	{
		vertx.eventBus().send("raml_resources", true, (Message<JsonArray> event) -> {
			merge(event.body().toList());
			e.reply(new JsonObject(ImmutableMap.copyOf((Map) _collector)));
		});
	}

	private void merge(final List<String> ramlAttributes)
	{
		// System.out.println("Merging with ramlAttributes");

		System.out.println("Collector uris:");
		keySet().forEach(p -> System.out.println("   " + p));
		System.out.println();

		System.out.println("Raml uris:");
		ramlAttributes.forEach(p -> System.out.println("   " + p));
		System.out.println();

		final List<ImmutablePair<String, Pattern>> patterns = ramlAttributes.stream() //
				.map(a -> ImmutablePair.of(a, Pattern.compile(createRegexPattern(a)))).collect(Collectors.toList());

		// System.out.println("Patterns:");
		// patterns.forEach(p -> System.out.println("   " + p.getValue().pattern()));
		// System.out.println();

		final UriKeyMap tmp = UriKeyMap.urlOnly();
		keySet().stream() //
				.map(k -> patterns.stream() //
						.map(p -> isMatching(p.getValue(), k) ? ImmutablePair.of(k, p.getKey()) : ObjectUtils.NULL) //
						.filter(o -> o != ObjectUtils.NULL) //
						.findFirst()) //
				.filter(o -> o.isPresent()) //
				.map(o -> (ImmutablePair) o.get()) //
				.reduce(tmp, //
						(UriKeyMap acc, ImmutablePair v) -> {
							if (acc != tmp) {
								throw new IllegalArgumentException("acc!=tmp > "  + acc + " >> " + tmp);
							}
							return merge(acc, v.getKey().toString(), v.getValue().toString());
						},
						(UriKeyMap acc1, UriKeyMap acc2) -> tmp);

		replace(tmp);
	}

	private void replace(final Map tmp)
	{
		// System.out.println("Replacing collector with URIs: ");
		// tmp.keySet().forEach(p -> System.out.println("   " + p));
		// System.out.println();

		_collector.clear();
		((Map<String, Object>) tmp).entrySet().forEach(e -> _collector.put(e.getKey(), (Map) e.getValue()));
	}

	private Set<String> keySet()
	{
		return ImmutableSet.copyOf(_collector.keySet());
	}

	private boolean isMatching(final Pattern p, final String k)
	{
		final boolean matches = p.matcher(k).matches();
		// System.out.println(" Match : " + matches + " # " + p.pattern() + " > " + k);
		return matches;
	}

	public String createRegexPattern(final String ramlUrl)
	{
		final String regex = ramlUrl.replaceAll("\\{[^/]+\\}", "[^/]+");
		// System.out.println(" > regex " + regex + "  > " + ramlUrl);
		return regex;
	}

	private UriKeyMap merge(final UriKeyMap acc, final String key, final String ramlKey)
	{
		// System.out.println("Merge ");
		// System.out.println("    - " + key);
		// System.out.println("    - " + ramlKey);

		if (!acc.containsKey(ramlKey))
		{
			acc.put(ramlKey, UriKeyMap.noUri(_collector.get(key)));
		}
		else
		{
			merge((UriKeyMap) getMap(acc, ramlKey), _collector.get(key));
		}

		return acc;
	}

	private UriKeyMap merge(final UriKeyMap dst, final Map src)
	{
		if (dst == src) {
			return dst;
		}

		((Map<String, Object>) src).entrySet().forEach((Map.Entry e) -> {
			if (e.getValue() instanceof Map)
			{
				dst.put((String) e.getKey(), merge((UriKeyMap) getMap(dst, (String) e.getKey()), (Map) e.getValue()));
			}
			else
			{
				getSet((Map) dst, (String) e.getKey()).addAll((Collection) e.getValue());
			}
		});

		return dst;
	}

	private void record(final Message<JsonObject> e)
	{
		final JsonObject body = e.body();

		final Map attributeMap = (Map) getMap(getMap(_collector, body.getString("uri")), body.getString("method"));
		final JsonObject attributes = body.getObject("attribute");

		attributes.getFieldNames().stream().filter(attr -> !attr.endsWith(".body")).forEach(attr -> getSet(attributeMap, attr)
				.add(attributes.getValue(attr).toString()));
	}

	private SetUniqueList<String> getSet(final Map<String, SetUniqueList<String>> map, final String attr)
	{
		if (!map.containsKey(attr))
		{
			map.put(attr, SetUniqueList.setUniqueList(new LinkedList<>()));
		}
		return map.get(attr);
	}

	private Map<String, ?> getMap(final Map<String, ?> map, final String key)
	{
		if (!map.containsKey(key))
		{
			((Map) map).put(key, UriKeyMap.noUri());
		}
		return (Map) map.get(key);
	}

	private void restart(final Message<Boolean> e)
	{
		_collector.clear();
	}
}
