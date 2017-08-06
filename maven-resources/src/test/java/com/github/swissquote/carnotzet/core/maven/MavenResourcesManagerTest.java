package com.github.swissquote.carnotzet.core.maven;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.swissquote.carnotzet.core.CarnotzetModule;

public class MavenResourcesManagerTest {

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	@Test
	public void override_file() throws IOException {
		// Given
		URL url = Thread.currentThread().getContextClassLoader().getResource("example_override");
		File example = new File(url.getPath());
		Path resources = temp.newFolder().toPath();
		FileUtils.copyDirectory(example, resources.toFile());
		MavenResourcesManager manager = new MavenResourcesManager(resources, null);
		List<CarnotzetModule> modules = Arrays.asList(
				CarnotzetModule.builder().name("service3").build(),
				CarnotzetModule.builder().name("service2").build(),
				CarnotzetModule.builder().name("service1").build()
		);

		// When
		manager.resolveResources(modules);

		// Then
		Assert.assertThat(readFile(resources, "resolved/service3/files/injected.by.service1"), Is.is("service1"));
		Assert.assertThat(readFile(resources, "resolved/service3/files/injected.by.service2"), Is.is("service2"));
		Assert.assertThat(readFile(resources, "resolved/service3/files/injected.by.service3"), Is.is("service3"));
		Assert.assertThat(readFile(resources, "resolved/service3/files/overridden.by.service1"), Is.is("service1"));
		Assert.assertThat(readFile(resources, "resolved/service3/files/overridden.by.service2"), Is.is("service2"));
		Assert.assertThat(readFile(resources, "resolved/service3/files/injected.by.service2.and.overridden.by.service1"), Is.is("service1"));
		Assert.assertThat(readFile(resources, "resolved/service3/files/injected.by.service2.and.overridden.by.service1"), Is.is("service1"));
		Assert.assertThat(readFile(resources, "resolved/service3/files/subfolder/subfolder.injected.by.service1"), Is.is("service1"));

	}

	private String readFile(Path root, String path) throws IOException {
		return new String(Files.readAllBytes(root.resolve(path)), "UTF-8");
	}

	@Test
	public void merge_files() throws IOException {
		// Given
		URL url = Thread.currentThread().getContextClassLoader().getResource("example_merge");
		File example = new File(url.getPath());
		Path resources = temp.newFolder().toPath();
		FileUtils.copyDirectory(example, resources.toFile());
		MavenResourcesManager manager = new MavenResourcesManager(resources, null);
		List<CarnotzetModule> modules = Arrays.asList(
				CarnotzetModule.builder().name("service3").build(),
				CarnotzetModule.builder().name("service2").build(),
				CarnotzetModule.builder().name("service1").build()
		);

		// When
		manager.resolveResources(modules);

		// Then
		Properties service3config = new Properties();
		service3config.load(Files.newInputStream(resources.resolve("resolved/service3/files/config.properties")));
		Assert.assertThat(service3config.getProperty("overridden.from.service2"), Is.is("service2value"));
		Assert.assertThat(service3config.getProperty("overridden.from.service1"), Is.is("service1value"));
		Assert.assertThat(service3config.getProperty("added.from.service3"), Is.is("service3value"));
		Assert.assertThat(service3config.getProperty("added.from.service2"), Is.is("service2value"));
		Assert.assertThat(service3config.getProperty("added.from.service1"), Is.is("service1value"));
		Assert.assertThat(service3config.getProperty("added.from.service2.and.overridden.from.service1"), Is.is("service1value"));

		Properties service3carnotzet = new Properties();
		service3carnotzet.load(Files.newInputStream(resources.resolve("resolved/service3/carnotzet.properties")));
		Assert.assertThat(service3carnotzet.getProperty("docker.image"), Is.is("service3"));
		Assert.assertThat(service3carnotzet.getProperty("network.aliases"), Is.is("my-service3"));

	}

}
