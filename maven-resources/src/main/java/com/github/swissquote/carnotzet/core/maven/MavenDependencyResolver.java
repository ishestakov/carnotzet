package com.github.swissquote.carnotzet.core.maven;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolvedArtifact;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinate;

import com.github.swissquote.carnotzet.core.CarnotzetModule;
import com.github.swissquote.carnotzet.core.CarnotzetModuleId;
import com.github.swissquote.carnotzet.core.DependencyResolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MavenDependencyResolver implements DependencyResolver {

	private static final Function<MavenCoordinate, CarnotzetModuleId> moduleIdTransformer = (coordinate) ->
			new CarnotzetModuleId(coordinate.getGroupId(), coordinate.getArtifactId(), coordinate.getVersion());

	private final Function<CarnotzetModuleId, String> moduleNameProvider;

	@Override
	public List<CarnotzetModule> resolve(CarnotzetModuleId topLevelModuleId) {
		List<CarnotzetModule> result = new ArrayList<>();

		String topLevelModuleName = moduleNameProvider.apply(topLevelModuleId);

		//We trust that shrinkwrap resolver returns the order we expect
		List<MavenResolvedArtifact> resolvedDependencies = Arrays.stream(Maven.configureResolver()//.workOffline()
				.resolve(topLevelModuleId.getGroupId() + ":" + topLevelModuleId.getArtifactId() + ":" + topLevelModuleId.getVersion())
				.withTransitivity().asResolvedArtifact())
				.filter((artifact) -> moduleNameProvider.apply(moduleIdTransformer.apply(artifact.getCoordinate())) != null)
				.collect(Collectors.toList());

		log.debug("Resolved dependencies using shrinkwrap : " + resolvedDependencies);

		for (MavenResolvedArtifact artifact : resolvedDependencies) {
			MavenCoordinate coordinate = artifact.getCoordinate();
			String moduleName = moduleNameProvider.apply(moduleIdTransformer.apply(coordinate));
			CarnotzetModuleId moduleId = new CarnotzetModuleId(coordinate.getGroupId(), coordinate.getArtifactId(), coordinate.getVersion());
			CarnotzetModule module = CarnotzetModule.builder()
					.id(moduleId)
					.name(moduleName)
					.topLevelModuleName(topLevelModuleName)
					.build();

			result.add(0, module);
		}
		return result;
	}

}

