/*
 * Copyright 2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */
package jmh.mbr.core.model;

import jmh.mbr.core.model.BenchmarkParameters.BenchmarkArgument;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openjdk.jmh.annotations.Benchmark;

/**
 * Factory to create a {@link BenchmarkDescriptor} from a benchmark class.
 */
public class BenchmarkDescriptorFactory {

	private final Class<?> benchmarkClass;

	private BenchmarkDescriptorFactory(Class<?> benchmarkClass) {
		this.benchmarkClass = benchmarkClass;
	}

	public static BenchmarkDescriptorFactory create(Class<?> benchmarkClass) {

		Objects.requireNonNull(benchmarkClass, "Benchmark class must not be null");

		return new BenchmarkDescriptorFactory(benchmarkClass);
	}

	/**
	 * @return the {@link BenchmarkDescriptor} for the underlying {@link Class}.
	 */
	public BenchmarkClass createDescriptor() {

		List<BenchmarkDescriptor> children = getBenchmarkMethods(it -> it.isAnnotationPresent(Benchmark.class)).map(it -> {

			if (it.isParametrized()) {

				List<BenchmarkFixture> fixtures = createFixtures(it);

				return new ParametrizedBenchmarkMethod(it, fixtures);
			}

			return it;
		}).collect(Collectors.toList());

		return BenchmarkClass.create(benchmarkClass, children);
	}

	/**
	 * Creates {@link BenchmarkFixture} for a parametrized {@link BenchmarkMethod}.
	 *
	 * @param method the {@link BenchmarkMethod} to inspect.
	 * @return list of fixtures if parameterized. Empty list if the method is not parametrized.
	 */
	public List<BenchmarkFixture> createFixtures(BenchmarkMethod method) {

		List<StateClass> stateClasses = new ArrayList<>();

		if (StateClass.isParametrized(method.getDeclaringClass())) {
			stateClasses.add(StateClass.create(method.getDeclaringClass()));
		}

		List<StateClass> argumentStateClasses = Arrays.stream(method.getParameters())//
				.map(Parameter::getType) //
				.filter(StateClass::isParametrized) //
				.map(StateClass::create) //
				.collect(Collectors.toList());

		stateClasses.addAll(argumentStateClasses);

		Collection<BenchmarkArgument> arguments = BenchmarkParameters.discover(stateClasses);
		Iterator<BenchmarkArgument> iterator = arguments.iterator();

		return iterator.hasNext() ? createFixtures(iterator.next(), iterator) : Collections.emptyList();
	}

	private List<BenchmarkFixture> createFixtures(BenchmarkArgument argument, Iterator<BenchmarkArgument> iterator) {

		List<BenchmarkFixture> fixtures = new ArrayList<>(argument.getParameterCount());

		for (String parameter : argument.getParameters()) {
			fixtures.add(BenchmarkFixture.create(argument.getName(), parameter));
		}

		while (iterator.hasNext()) {
			fixtures = enhance(fixtures, iterator.next());
		}

		return fixtures;

	}

	private List<BenchmarkFixture> enhance(List<BenchmarkFixture> fixtures, BenchmarkArgument argument) {

		List<BenchmarkFixture> enhanced = new ArrayList<>(fixtures.size() * argument.getParameterCount());

		for (BenchmarkFixture fixture : fixtures) {

			for (String parameter : argument.getParameters()) {
				enhanced.add(fixture.enhance(argument.getName(), parameter));
			}
		}

		return enhanced;
	}

	public Optional<BenchmarkMethod> getBenchmarkMethod(String name, Class<?>... parameterTypes) {

		return getBenchmarkMethods(it -> it.getName().equals(name) && it.getParameterCount() == parameterTypes.length
				&& Arrays.equals(parameterTypes, it.getParameterTypes())).findFirst();
	}

	public BenchmarkMethod getRequiredBenchmarkMethod(String name, Class<?>... parameterTypes) {
		return getBenchmarkMethod(name, parameterTypes)
				.orElseThrow(() -> new IllegalArgumentException("Cannot find method " + name));
	}

	private Stream<BenchmarkMethod> getBenchmarkMethods(Predicate<Method> filter) {

		return Stream.concat(Arrays.stream(benchmarkClass.getMethods()), Arrays.stream(benchmarkClass.getDeclaredMethods())) //
				.filter(filter) //
				.distinct() //
				.map(BenchmarkMethod::new);
	}
}
