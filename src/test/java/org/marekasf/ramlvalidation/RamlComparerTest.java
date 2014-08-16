package org.marekasf.ramlvalidation;

import org.junit.Test;
import org.vertx.java.core.json.JsonObject;

public class RamlComparerTest
{

	@Test
	public void shouldParse()
	{
		final RamlComparer comparer = new RamlComparer();

		comparer.setRamlResource("http://localhost:8080/api-console/raml/api/document-repository-service.raml");

		System.out.println(new JsonObject(comparer.restart(null)).encodePrettily());
	}
}
