package org.apache.maven.artifact.repository;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.repository.legacy.repository.ArtifactRepository;
import org.apache.maven.repository.legacy.repository.ArtifactRepositoryPolicy;
import org.apache.maven.repository.legacy.repository.layout.ArtifactRepositoryLayout;
import org.codehaus.plexus.component.annotations.Component;

@Deprecated
@Component(role = ArtifactRepositoryFactory.class)
public class DefaultArtifactRepositoryFactory
    extends org.apache.maven.repository.legacy.repository.DefaultArtifactRepositoryFactory
    implements ArtifactRepositoryFactory
{
}
