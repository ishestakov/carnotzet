package com.github.swissquote.carnotzet.core.maven;

import java.nio.file.Path;

import com.github.swissquote.carnotzet.core.ResourcesManager;
import com.github.swissquote.carnotzet.core.ResourcesManagerBuilder;

public class MavenResourcesManagerBuilder implements ResourcesManagerBuilder {

	private Path resourcesRoot;
	private Path topLevelModulePath;

	@Override
	public ResourcesManagerBuilder withResourcesPath(Path resourcesPath) {
		this.resourcesRoot = resourcesPath;
		return this;
	}

	@Override
	public ResourcesManagerBuilder withTopLevelModulePath(Path topLevelModulePath) {
		this.topLevelModulePath = topLevelModulePath;
		return this;
	}

	@Override
	public ResourcesManager build() {
		return new MavenResourcesManager(resourcesRoot, topLevelModulePath);
	}
}
