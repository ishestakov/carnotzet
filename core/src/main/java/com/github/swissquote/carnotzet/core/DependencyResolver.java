package com.github.swissquote.carnotzet.core;

import java.util.List;

public interface DependencyResolver {
	List<CarnotzetModule> resolve(CarnotzetModuleId topLevelModuleId);
}
