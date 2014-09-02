package org.marekasf.ramlvalidation;

import java.net.URLClassLoader;
import java.util.concurrent.Semaphore;

import org.apache.commons.cli.CommandLine;
import org.marekasf.ramlvalidation.commanline.ArgumentsParser;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.PlatformLocator;
import org.vertx.java.platform.PlatformManager;
import org.vertx.java.platform.Verticle;

import com.fasterxml.jackson.databind.util.JSONPObject;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;

public class ProxyServerApplication extends Verticle
{
	private static volatile PlatformManager pm;

	public static final Semaphore semaphore = new Semaphore(0);

	public static void main(final String[] args) throws Exception
	{
		final ArgumentsParser parser = new ArgumentsParser(args);
		if (!parser.isUsageRequested())
		{
			startProxy(getConfiguration(parser));
			waitForStopEvent();
			stopProxy();
		}
		else
		{
			parser.printUsage();
		}
	}

	private static JsonObject getConfiguration(final ArgumentsParser parser)
	{
		if (parser.isConfigurationProvided())
		{
			return parser.getConfiguration();
		}
		else
		{
			return prepareClassPathConfiguration();
		}
	}

	/**
	 * Hack for allowing easily stopping of server in CI environment.
	 */
	private static void waitForStopEvent()
	{
		try
		{
			semaphore.acquire();
		}
		catch (final InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	public static void stopProxy()
	{
		if (pm != null)
		{
			pm.stop();
		}
		pm = null;
	}

	private static void startProxy(final JsonObject configuration)
	{
		if (pm == null)
		{
			pm = PlatformLocator.factory.createPlatformManager();
			pm.deployVerticle(ProxyServerApplication.class.getCanonicalName(), configuration,
					((URLClassLoader) ProxyServerApplication.class.getClassLoader()).getURLs(), 1, null, null);
		}
	}

	private static JsonObject prepareClassPathConfiguration()
	{
		try
		{
			return new JsonObject(Resources.toString(Resources.getResource("ramlvalidation_config.json"), Charsets.UTF_8));
		}
		catch (final Exception e)
		{
			System.out.println("Configuration resource could not be loaded. Default will be used.");
		}
		return null;
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
