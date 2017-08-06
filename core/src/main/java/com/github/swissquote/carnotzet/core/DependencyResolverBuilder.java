package com.github.swissquote.carnotzet.core;

import java.util.function.Function;

public interface DependencyResolverBuilder {
	DependencyResolverBuilder withModuleNameTransformFunction(Function<CarnotzetModuleId, String> transformation);
	DependencyResolver build();

}
