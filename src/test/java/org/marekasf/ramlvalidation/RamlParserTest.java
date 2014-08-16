package org.marekasf.ramlvalidation;

import static org.fest.assertions.Assertions.assertThat;

import java.util.regex.Pattern;

import org.junit.Test;
import org.vertx.java.core.json.JsonObject;

public class RamlParserTest
{

	@Test
	public void shouldParse()
	{
		final RamlParser comparer = new RamlParser();

		comparer.setRamlResource("http://localhost:8080/api-console/raml/api/document-repository-service.raml");

		System.out.println(new JsonObject(comparer.loadRaml()).encodePrettily());
	}

	@Test
	public void regexUriTest()
	{
		final Pattern p = Pattern.compile("/[^/]+/bb/[^/]+/dd");
		assertThat(p.matcher("/xx/bb/xx/dd").matches()).isTrue();
		assertThat(p.matcher("/333/bb/Xasasa./e").matches()).isFalse();

	}

	@Test
	public void shouldCreateRegex()
	{
		final String regexPattern = new Collector().createRegexPattern("/a/{path}/a/{param}/xxx");
		System.out.println(regexPattern);

		final Pattern p = Pattern.compile(regexPattern);

		assertThat(p.matcher("/a/xxx/a/bbdd/xxx").matches()).isTrue();
		assertThat(p.matcher("/a/xxx/b/bbdd/xxx").matches()).isFalse();
	}
}
