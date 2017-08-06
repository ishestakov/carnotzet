package com.github.swissquote.carnotzet.core;

import java.nio.file.Path;

public interface ResourcesManagerBuilder {
	ResourcesManagerBuilder withResourcesPath(Path resourcesPath);
	ResourcesManagerBuilder withTopLevelModulePath(Path topLevelModulePath);
	ResourcesManager build();
}
