/*
 * Copyright 2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */
package jmh.mbr.junit5.execution;

import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import jmh.mbr.junit5.descriptor.AbstractBenchmarkDescriptor;
import jmh.mbr.junit5.descriptor.BenchmarkClassDescriptor;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;

/**
 * Provides {@link ExtensionContext} for a {@link BenchmarkClassDescriptor}.
 */
class ExtensionContextProvider implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(ConditionEvaluator.class);

	private final EngineExecutionListener listener;
	private final ConfigurationParameters configurationParameters;
	private final Map<AbstractBenchmarkDescriptor, ExtensionContext> contextMap = new LinkedHashMap<>();

	private ExtensionContextProvider(EngineExecutionListener listener, ConfigurationParameters configurationParameters) {
		this.listener = listener;
		this.configurationParameters = configurationParameters;
	}

	/**
	 * Creates a new {@link ExtensionContextProvider}.
	 *
	 * @param listener must not be {@literal null}.
	 * @param configurationParameters must not be {@literal null}.
	 * @return the new {@link ExtensionContextProvider}.
	 */
	static ExtensionContextProvider create(EngineExecutionListener listener, ConfigurationParameters configurationParameters) {
		return new ExtensionContextProvider(listener, configurationParameters);
	}

	ExtensionContext getExtensionContext(Optional<TestDescriptor> benchmarkClassDescriptor) {

		return benchmarkClassDescriptor.filter(BenchmarkClassDescriptor.class::isInstance)
				.map(BenchmarkClassDescriptor.class::cast)
				.map(this::getExtensionContext)
				.orElse(null);
	}

	ExtensionContext getExtensionContext(BenchmarkClassDescriptor descriptor) {
		return contextMap.computeIfAbsent(descriptor, key -> key.getExtensionContext(null, listener, configurationParameters));
	}

	@Override
	public void close() {

		for (ExtensionContext value : contextMap.values()) {

			if (!(value instanceof Closeable)) {
				continue;
			}

			try {
				((Closeable) value).close();
			} catch (IOException e) {
				logger.error(e, () -> String.format("Cannot close context [%s]", value));
			}
		}
	}
}
