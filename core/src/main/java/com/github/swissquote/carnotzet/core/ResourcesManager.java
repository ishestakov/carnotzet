package com.github.swissquote.carnotzet.core;

import java.nio.file.Path;
import java.util.List;

public interface ResourcesManager {
	Path getModuleResourcesPath(CarnotzetModule module);

	void extractResources(List<CarnotzetModule> modules);

	void resolveResources(List<CarnotzetModule> modules);

	void copyModuleResources(CarnotzetModuleId moduleId, Path moduleResourcesPath);

	Path getResourcesRoot();
}
