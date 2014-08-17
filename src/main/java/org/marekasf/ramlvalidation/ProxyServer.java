package org.marekasf.ramlvalidation;

import java.io.Serializable;

import org.vertx.java.core.MultiMap;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class ProxyServer extends Verticle
{

	@Override
	public void start()
	{

		System.out.println("Starting Vert.x proxy Server");

		final JsonObject config = getContainer().config();
		final HttpClient client = vertx.createHttpClient().setHost(config.getString("target.host", "localhost")).setPort(
				config.getInteger("target.port", 8080));

		RouteMatcher rm = new RouteMatcher();

		rm.get("/raml-validation-proxy/restart", req -> {
			vertx.eventBus().publish("restart", true);
			req.response().end("restarted");
		});

		rm.get("/raml-validation-proxy/proxy_log.json", req -> vertx.eventBus().send("proxy_log", true,
						(Message<JsonObject> event) -> req.response().end(event.body().encodePrettily())));

		rm.get("/raml-validation-proxy/raml_log.json", req -> vertx.eventBus().send("raml_log", true,
						(Message<JsonObject> event) -> req.response().end(event.body().encodePrettily())));

		rm.get("/raml-validation-proxy/raml_report.json", req -> vertx.eventBus().send("raml_report", true,
				(Message<JsonObject> event) -> req.response().end(event.body().encodePrettily())));

		rm.all(".*", req -> {
			final HttpClientRequest cReq = client.request(req.method(), req.uri(), cRes -> {
				req.response().setStatusCode(cRes.statusCode());
				send(req, "response.status", cRes.statusCode());
				req.response().headers().set(cRes.headers());
				send(req, "response.header", cRes.headers());
				req.response().setChunked(true);

				cRes.dataHandler(data -> {
					send(req, "response.body", data.copy().toString());
					req.response().write(data);
				});

				cRes.endHandler(e -> req.response().end());
			});

			send(req, "request.query", req.params());
			cReq.headers().set(req.headers());
			send(req, "request.header", req.headers());
			cReq.setChunked(true);
			req.dataHandler(data -> {
				send(req, "request.body", data.copy().toString());
				cReq.write(data);
			});
			req.endHandler(e -> {
				cReq.end();
			});
		});

		vertx.createHttpServer().requestHandler(rm).listen(config.getInteger("proxy.port", 8081));
	}

	private void send(final HttpServerRequest req, final String name, final MultiMap headers)
	{
		headers.entries().stream().forEach(entry -> send(req, name + "." + entry.getKey(), entry.getValue()));
	}

	private void send(final HttpServerRequest req, final String name, final Serializable data)
	{
		vertx.eventBus().send("record", new JsonObject().putString("method", req.method()).putString("uri",
				req.absoluteURI().getPath()).putObject("attribute", new JsonObject().putValue(name, data)));
	}
}
