/*
 * Copyright 2018-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */
package jmh.mbr.junit5.descriptor;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.EngineExecutionListener;

/**
 * {@link ExtensionContext} for a {@link BenchmarkClassDescriptor}.
 */
class BenchmarkClassExtensionContext extends AbstractExtensionContext<BenchmarkClassDescriptor> {

	BenchmarkClassExtensionContext(ExtensionContext parent, EngineExecutionListener engineExecutionListener, BenchmarkClassDescriptor testDescriptor, ConfigurationParameters configurationParameters) {
		super(parent, engineExecutionListener, testDescriptor, configurationParameters);
	}

	@Override
	public Optional<AnnotatedElement> getElement() {
		return Optional.of(getBenchmarkDescriptor().getJavaClass());
	}

	@Override
	public Optional<Class<?>> getTestClass() {
		return Optional.of(getBenchmarkDescriptor().getJavaClass());
	}

}
