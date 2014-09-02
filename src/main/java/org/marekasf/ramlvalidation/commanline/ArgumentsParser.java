/*
 * [y] hybris Platform
 *
 * Copyright (c) 2000-2014 hybris AG
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of hybris
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with hybris.
 */
package org.marekasf.ramlvalidation.commanline;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.vertx.java.core.json.JsonObject;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

/**
 * Command line parser allowing easily configuration of raml validation proxy;
 */
public class ArgumentsParser
{

	public static final String TARGET_HOST = "target.host";
	public static final String TARGET_PORT = "target.port";
	public static final String PROXY_PORT = "proxy.port";
	public static final String RAML_RESOURCE = "raml.resource";
	public static final String IGNORED_RESOURCES = "ignored.resources";

	public static final String CMD_TARGET_HOST = "targetHost";
	public static final String CMD_TARGET_PORT = "targetPort";
	public static final String CMD_PROXY_PORT = "proxyPort";
	public static final String CMD_RAML_RESOURCE = "ramlResource";
	public static final String CMD_IGNORED_RESOURCES = "ignoredResources";
	public static final String CMD_HELP = "help";

	private static final Options options;

	static
	{
		options = new Options();
		options.addOption(new Option(CMD_HELP, "Display usage info"));

		options.addOption(OptionBuilder.withArgName("url").hasArg().withDescription(
				"Target host to which proxy will forward calls").create(CMD_TARGET_HOST));

		options.addOption(OptionBuilder.withArgName("port").hasArg().withDescription(
				"Target port to which proxy will forward calls").create(CMD_TARGET_PORT));

		options.addOption(OptionBuilder.withArgName("port").hasArg().withDescription("Port on which proxy will listen").create(
				CMD_PROXY_PORT));

		options.addOption(OptionBuilder.withArgName("url").hasArg().withDescription(
				"Url to raml resource definition which will be validated").create(CMD_RAML_RESOURCE));

		options.addOption(OptionBuilder.withArgName("ignored").hasArg().withDescription(
				"Comma separated list of all resources which will be ignored").create(CMD_IGNORED_RESOURCES));
	}

	private final String[] arguments;
	private final Optional<CommandLine> commandLine;

	/**
	 * Constructor with command line arguments which will parse command line.
	 *
	 * @param commandLine provided during startup of application.
	 */
	public ArgumentsParser(final String[] commandLine)
	{
		this.arguments = commandLine;
		this.commandLine = Optional.ofNullable(parse());
	}

	private CommandLine parse()
	{
		final CommandLineParser parser = new BasicParser();
		try
		{
			return parser.parse(options, arguments);
		}
		catch (final ParseException e)
		{
			// TODO change it to LOG and add proper error handling
			System.err.println(e.getMessage());
			return null;
		}
	}

	/**
	 * Returns configuration as JsonObject.
	 *
	 * @return configuration as JsonObject.
	 */
	public JsonObject getConfiguration()
	{
		return commandLine.map(cmdLine -> buildJsonObject(cmdLine)).orElse(new JsonObject());
	}

	private JsonObject buildJsonObject(final CommandLine commandLine)
	{
		final Map<String, Object> properties = new HashMap<>();
		if (commandLine.hasOption(CMD_TARGET_HOST))
		{
			properties.put(TARGET_HOST, commandLine.getOptionValue(CMD_TARGET_HOST));
		}
		if (commandLine.hasOption(CMD_TARGET_PORT))
		{
			properties.put(TARGET_PORT, commandLine.getOptionValue(CMD_TARGET_PORT));
		}
		if (commandLine.hasOption(CMD_PROXY_PORT))
		{
			properties.put(PROXY_PORT, commandLine.getOptionValue(CMD_PROXY_PORT));
		}
		if (commandLine.hasOption(CMD_RAML_RESOURCE))
		{
			properties.put(RAML_RESOURCE, commandLine.getOptionValue(CMD_RAML_RESOURCE));
		}
		if (commandLine.hasOption(CMD_IGNORED_RESOURCES))
		{
			properties.put(IGNORED_RESOURCES, splitIntoList(commandLine.getOptionValue(CMD_IGNORED_RESOURCES)));
		}
		return new JsonObject(properties);
	}

	private List<String> splitIntoList(final String line)
	{
		final Iterable<String> split = Splitter.on(",").omitEmptyStrings().trimResults().split(line);
		return Lists.newArrayList(split);
	}

	public void printUsage()
	{
		final HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("RAML verification", options);
	}

	/**
	 * Checks if usage message should be displayed.
	 *
	 * @return <code>true</code> if help message should be displayed.
	 */
	public boolean isUsageRequested()
	{
		return commandLine.map((cmd) -> cmd.hasOption(CMD_HELP)).orElse(false);
	}


	/**
	 * Checks if command line contains configuration. It is enough to have one parameters configured to assume that
	 * configuration was provided.
	 *
	 * @return <code>true</code> if configuration has been provided. Otherwise default or configuration from classpath will be provided.
	 */
	public boolean isConfigurationProvided()
	{
	 	return commandLine.map((cmd) -> cmd.getArgs().length > 0 && !isUsageRequested()).orElse(false);
	}
}
