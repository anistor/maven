package org.apache.maven.artifact.repository.layout;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.component.annotations.Component;

@Deprecated
@Component(role=ArtifactRepositoryLayout.class,hint="default")
public class DefaultRepositoryLayout
    implements ArtifactRepositoryLayout
{
}
