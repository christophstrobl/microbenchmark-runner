/*
 * Copyright 2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */
package jmh.mbr.junit5.descriptor;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.engine.execution.ExtensionValuesStore;
import org.junit.jupiter.engine.execution.NamespaceAwareStore;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.reporting.ReportEntry;

/**
 * Abstract base class for {@link ExtensionContext}. This class implements {@link AutoCloseable} to close {@link java.io.Closeable} {@link ExtensionValuesStore value stores}.
 *
 * @param <T>
 */
abstract class AbstractExtensionContext<T extends TestDescriptor> implements ExtensionContext, AutoCloseable {

	private final ExtensionContext parent;
	private final EngineExecutionListener engineExecutionListener;
	private final T benchmarkDescriptor;
	private final Set<String> tags;
	private final ConfigurationParameters configurationParameters;
	private final ExtensionValuesStore valuesStore;

	AbstractExtensionContext(ExtensionContext parent, EngineExecutionListener engineExecutionListener, T benchmarkDescriptor,
							 ConfigurationParameters configurationParameters) {

		Preconditions.notNull(benchmarkDescriptor, "TestDescriptor must not be null");
		Preconditions.notNull(configurationParameters, "ConfigurationParameters must not be null");

		this.parent = parent;
		this.engineExecutionListener = engineExecutionListener;
		this.benchmarkDescriptor = benchmarkDescriptor;
		this.configurationParameters = configurationParameters;
		this.valuesStore = createStore(parent);

		// @formatter:off
		this.tags = benchmarkDescriptor.getTags().stream()
				.map(TestTag::getName)
				.collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), Collections::unmodifiableSet));
		// @formatter:on
	}

	private ExtensionValuesStore createStore(ExtensionContext parent) {
		ExtensionValuesStore parentStore = null;
		if (parent != null) {
			parentStore = ((AbstractExtensionContext<?>) parent).valuesStore;
		}
		return new ExtensionValuesStore(parentStore);
	}

	@Override
	public void close() {
		this.valuesStore.closeAllStoredCloseableValues();
	}

	@Override
	public String getUniqueId() {
		return getBenchmarkDescriptor().getUniqueId().toString();
	}

	@Override
	public String getDisplayName() {
		return getBenchmarkDescriptor().getDisplayName();
	}

	@Override
	public void publishReportEntry(Map<String, String> values) {
		this.engineExecutionListener.reportingEntryPublished(this.benchmarkDescriptor, ReportEntry.from(values));
	}

	@Override
	public Optional<ExtensionContext> getParent() {
		return Optional.ofNullable(this.parent);
	}

	@Override
	public ExtensionContext getRoot() {
		if (this.parent != null) {
			return this.parent.getRoot();
		}
		return this;
	}

	protected T getBenchmarkDescriptor() {
		return this.benchmarkDescriptor;
	}

	@Override
	public Store getStore(Namespace namespace) {
		Preconditions.notNull(namespace, "Namespace must not be null");
		return new NamespaceAwareStore(this.valuesStore, namespace);
	}

	@Override
	public Set<String> getTags() {
		return new LinkedHashSet<>(this.tags);
	}

	@Override
	public Optional<String> getConfigurationParameter(String key) {
		return this.configurationParameters.get(key);
	}
}
