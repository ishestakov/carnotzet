package com.github.swissquote.carnotzet.core.maven;

import java.util.function.Function;

import com.github.swissquote.carnotzet.core.CarnotzetModuleId;
import com.github.swissquote.carnotzet.core.DependencyResolver;
import com.github.swissquote.carnotzet.core.DependencyResolverBuilder;

public class MavenDependencyResolverBuilder implements DependencyResolverBuilder {

	private Function<CarnotzetModuleId, String> moduleNameTransformation;

	@Override
	public DependencyResolverBuilder withModuleNameTransformFunction(Function<CarnotzetModuleId, String> transformation) {
		moduleNameTransformation = transformation;
		return this;
	}

	@Override
	public DependencyResolver build() {
		return new MavenDependencyResolver(moduleNameTransformation);
	}
}
