package com.github.swissquote.carnotzet.core.maven;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import com.github.swissquote.carnotzet.core.CarnotzetDefinitionException;
import com.github.swissquote.carnotzet.core.CarnotzetModuleId;

import org.jboss.shrinkwrap.resolver.api.maven.PackagingType;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinate;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class that can be used to describe the root maven artifact for creating a carnotzet
 */
@Value
@AllArgsConstructor
public class CarnotzetModuleCoordinates implements MavenCoordinate {

	private final String groupId;
	private final String artifactId;
	private final String version;

	@Override
	public PackagingType getPackaging() {
		return PackagingType.JAR;
	}

	@Override
	public PackagingType getType() {
		return PackagingType.JAR;
	}

	@Override
	public String getClassifier() {
		return null;
	}

	@Override
	public String toCanonicalForm() {
		return groupId + ":" + artifactId + ":jar:" + version;
	}

	public CarnotzetModuleId getModuleId() {
		return new CarnotzetModuleId(this.getGroupId(), this.getArtifactId(), this.getVersion());
	}

	public static CarnotzetModuleCoordinates fromPom(Path pom) {
		Model result;
		try {
			BufferedReader in = new BufferedReader(Files.newBufferedReader(pom, StandardCharsets.UTF_8));
			MavenXpp3Reader reader = new MavenXpp3Reader();
			result = reader.read(in);
		}
		catch (XmlPullParserException | IOException e) {
			throw new CarnotzetDefinitionException(e);
		}
		String groupId = result.getGroupId();
		String version = result.getVersion();
		if (groupId == null) {
			groupId = result.getParent().getGroupId();
		}
		if (version == null) {
			version = result.getParent().getVersion();
		}
		return new CarnotzetModuleCoordinates(groupId, result.getArtifactId(), version);
	}

}
