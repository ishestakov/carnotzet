package com.github.swissquote.carnotzet.core;

import static java.nio.file.Files.exists;
import static java.nio.file.Files.walk;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents an environment definition as a set of executable applications with their configuration.
 */
@Slf4j
public class Carnotzet {

	@Getter
	private final CarnotzetConfig config;

	@Getter
	private final String topLevelModuleName;

	@Getter
	private final Pattern moduleFilterPattern;

	private List<CarnotzetModule> modules;

	private final DependencyResolver resolver;

	private final ResourcesManager resourceManager;

	private final String defaultContainerRegistry;

	private final List<String> propFileNames;

	public Carnotzet(CarnotzetConfig config) {
		log.debug("Creating new carnotzet with config [{}]", config);
		this.config = config;

		String filterPattern = "(.*)-carnotzet";
		if (config.getModuleFilterPattern() != null) {
			filterPattern = config.getModuleFilterPattern();
		}
		moduleFilterPattern = Pattern.compile(filterPattern);
		if (moduleFilterPattern.matcher("").groupCount() != 1) {
			throw new CarnotzetDefinitionException("moduleFilterPattern must have exactly 1 capture group");
		}

		this.topLevelModuleName = getModuleName(config.getTopLevelModuleId());

		Path resourcesPath = config.getResourcesPath();
		if (resourcesPath == null) {
			resourcesPath = Paths.get("/tmp/carnotzet_" + System.nanoTime());
		}
		this.resourceManager = lookupResourcesManager(config, resourcesPath);

		if (config.getDefaultDockerRegistry() != null) {
			this.defaultContainerRegistry = config.getDefaultDockerRegistry();
		} else {
			this.defaultContainerRegistry = "docker.io";
		}

		if (config.getPropFileNames() != null) {
			this.propFileNames = config.getPropFileNames();
		} else {
			this.propFileNames = Arrays.asList("carnotzet.properties");
		}

		resolver = lookupDependencyResolver();

	}

	private DependencyResolver lookupDependencyResolver() {
		ServiceLoader<DependencyResolverBuilder> builderLoader = ServiceLoader.load(DependencyResolverBuilder.class);
		if (builderLoader.iterator().hasNext()) {
			return builderLoader.iterator().next().withModuleNameTransformFunction(this::getModuleName).build();
		}
		throw new IllegalStateException("No service for DependencyResolverBuilder was provided");
	}

	private ResourcesManager lookupResourcesManager(CarnotzetConfig config, Path resourcesPath) {
		ServiceLoader<ResourcesManagerBuilder> resourcesManagerBuilderLoader = ServiceLoader.load(ResourcesManagerBuilder.class);
		if (resourcesManagerBuilderLoader.iterator().hasNext()) {
			return resourcesManagerBuilderLoader.iterator().next().withResourcesPath(resourcesPath)
					.withTopLevelModulePath(config.getTopLevelModuleResourcesPath()).build();
		}
		throw new IllegalStateException("No service for ResourcesManagerBuilder was provided");
	}

	public List<CarnotzetModule> getModules() {
		if (modules == null) {
			log.debug("resolving module dependencies");
			modules = resolver.resolve(config.getTopLevelModuleId());
			log.debug("extracting module resources");
			resourceManager.extractResources(modules);
			log.debug("resolving module resources overrides and merges");
			resourceManager.resolveResources(modules);
			log.debug("configuring modules");
			modules = configureModules(modules);

			if (config.getExtensions() != null) {
				for (CarnotzetExtension feature : config.getExtensions()) {
					log.debug("Extension [{}] enabled", feature.getClass().getSimpleName());
					modules = feature.apply(this);
				}
			}
		}
		return modules;
	}

	private List<CarnotzetModule> configureModules(List<CarnotzetModule> modules) {
		return modules.stream().map(this::configureModule).collect(toList());
	}

	private CarnotzetModule configureModule(CarnotzetModule module) {
		CarnotzetModule.CarnotzetModuleBuilder result = module.toBuilder();
		Map<String, String> properties = readPropertiesFiles(module);
		result.properties(properties);

		// Default convention
		String imageName = defaultContainerRegistry + "/" + module.getName() + ":" + module.getId().getVersion();

		// Allow custom image through configuration
		if (properties.containsKey("docker.image")) {
			imageName = properties.get("docker.image");
		}

		// Allow configuration based disabling of docker container (config only module)
		if ("none".equals(imageName)) {
			imageName = null;
		}
		result.imageName(imageName);
		if (properties.containsKey("docker.entrypoint")) {
			result.dockerEntrypoint(properties.get("docker.entrypoint"));
		}
		if (properties.containsKey("docker.cmd")) {
			result.dockerCmd(properties.get("docker.cmd"));
		}
		result.dockerVolumes(getFileVolumes(module));
		result.dockerEnvFiles(getEnvFiles(module));

		return result.build();
	}

	private Map<String, String> readPropertiesFiles(CarnotzetModule module) {
		Map<String, String> result = new HashMap<>();
		for (String fileName : propFileNames) {
			Path filePath = getModuleResourcesPath(module).resolve(fileName);
			if (filePath.toFile().exists()) {
				try {
					Properties props = new Properties();
					props.load(Files.newInputStream(filePath));
					result.putAll((Map) props);
				}
				catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		}
		return result;
	}

	public Path getModuleResourcesPath(CarnotzetModule module) {
		return resourceManager.getModuleResourcesPath(module);
	}

	private Set<String> getEnvFiles(CarnotzetModule module) {
		Set<String> envFiles = new HashSet<>();
		Path envFilesRoot = getModuleResourcesPath(module).resolve("env");
		if (!exists(envFilesRoot)) {
			return Collections.emptySet();
		}
		try {
			envFiles.addAll(walk(envFilesRoot).filter(p -> p.toFile().isFile()).map(Path::toString).collect(toList()));
		}
		catch (IOException e) {
			log.error(String.format("Error while reading env files for module: %s", module.getName()), e);
		}

		return envFiles.isEmpty() ? null : envFiles;
	}

	private Set<String> getFileVolumes(CarnotzetModule module) {
		Map<String, String> result = new HashMap<>();
		Path toMount = getModuleResourcesPath(module).resolve("files");
		if (!Files.exists(toMount)) {
			return Collections.emptySet();
		}
		try {
			Files.walk(toMount).forEach((p) -> {
				if (p.toFile().isFile()) {
					result.put(p.toString(),
							new File(p.toString().substring(p.toString().indexOf("/files/") + "files/".length())).getAbsolutePath());
				}
			});
		}
		catch (IOException e) {
			log.error(String.format("Error while reading files to mount for module:%s", module.getName()), e);
		}

		return result.isEmpty() ? Collections.emptySet() : result.entrySet().stream().map(
				entry -> String.format("%s:%s", entry.getKey(), entry.getValue()))
				.collect(Collectors.toSet());
	}

	public Path getResourcesFolder() {
		return resourceManager.getResourcesRoot();
	}

	public String getModuleName(CarnotzetModuleId module) {
		Matcher m = moduleFilterPattern.matcher(module.getArtifactId());
		if (m.find()) {
			return m.group(1);
		}
		return null;
	}

}
