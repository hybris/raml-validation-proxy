package org.marekasf.ramlvalidation;

import java.util.HashMap;
import java.util.Map;

public class UriKeyMap extends HashMap<String, Object>
{
	protected final boolean acceptUri;

	public UriKeyMap(Map map, final boolean acceptUri)
	{
		super(map);
		this.acceptUri = acceptUri;
	}

	public UriKeyMap(boolean acceptUri)
	{
		this.acceptUri = acceptUri;
	}

	public static UriKeyMap urlOnly()
	{
		return new UriKeyMap(true);
	}

	public static UriKeyMap noUri(Map map)
	{
		return new UriKeyMap(map, false);
	}

	public static UriKeyMap noUri()
	{
		return new UriKeyMap(false);
	}

	@Override
	public Object put(final String key, final Object value)
	{
		if (key.startsWith("/"))
		{
			if (!acceptUri)
			{
				throw new IllegalArgumentException("invalid non uri key " + key);
			}
		}
		else if (acceptUri)
		{
			throw new IllegalArgumentException("invalid uri key " + key);
		}

		if (value instanceof Map && !(value instanceof UriKeyMap)) {
			throw new IllegalArgumentException("invalid map value " + value);
		}

		if (value instanceof UriKeyMap && ((UriKeyMap)value).acceptUri) {
			throw new IllegalArgumentException("invalid uri map value " + value);
		}

		return super.put(key, value);
	}
}
