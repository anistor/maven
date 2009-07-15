package org.apache.maven.artifact.repository.layout;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;

@Deprecated
public interface ArtifactRepositoryLayout
{
    String ROLE = ArtifactRepositoryLayout.class.getName();

    String getId();
    
    String pathOf( Artifact artifact );

    String pathOfLocalRepositoryMetadata( ArtifactMetadata metadata, ArtifactRepository repository );

    String pathOfRemoteRepositoryMetadata( ArtifactMetadata metadata );
    
}
