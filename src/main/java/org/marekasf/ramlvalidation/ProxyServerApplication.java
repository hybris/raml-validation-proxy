package org.marekasf.ramlvalidation;

import java.net.URLClassLoader;

import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.PlatformLocator;
import org.vertx.java.platform.PlatformManager;
import org.vertx.java.platform.Verticle;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

public class ProxyServerApplication extends Verticle
{
	private static volatile PlatformManager pm = null;

	public static void main(String[] args) throws Exception
	{
		startProxy();
		System.in.read(); // prevent JVM to stop
		stopProxy();
	}

	public static void stopProxy()
	{
		if (pm != null)
		{
			pm.stop();
		}
		pm = null;
	}

	private static void startProxy()
	{
		if (pm == null)
		{
			pm = PlatformLocator.factory.createPlatformManager();
			JsonObject config;

			try
			{
				config = new JsonObject(Resources.toString(Resources.getResource("/ramlvalidation_config.json"), Charsets.UTF_8));
			}
			catch (Exception e)
			{
				config = null;
			}

			pm.deployVerticle(ProxyServerApplication.class.getCanonicalName(), config,
					((URLClassLoader) ProxyServerApplication.class.getClassLoader()).getURLs(), 1, null, null);
		}
	}

	@Override
	public void start()
	{
		this.getContainer().deployVerticle(RamlReporter.class.getCanonicalName(), container.config());
		this.getContainer().deployVerticle(RamlParser.class.getCanonicalName(), container.config());
		this.getContainer().deployVerticle(Collector.class.getCanonicalName(), container.config());
		this.getContainer().deployVerticle(ProxyServer.class.getCanonicalName(), container.config());
	}
}
