package org.apache.maven.artifact.repository;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;

@Deprecated
public interface ArtifactRepository
{
    String pathOf( Artifact artifact );

    String pathOfRemoteRepositoryMetadata( ArtifactMetadata artifactMetadata );

    String pathOfLocalRepositoryMetadata( ArtifactMetadata metadata, ArtifactRepository repository );

    String getUrl();

    void setUrl( String url );

    String getBasedir();

    String getProtocol();

    String getId();

    void setId( String id );

    ArtifactRepositoryPolicy getSnapshots();

    void setSnapshotUpdatePolicy( ArtifactRepositoryPolicy policy );

    ArtifactRepositoryPolicy getReleases();

    void setReleaseUpdatePolicy( ArtifactRepositoryPolicy policy );

    ArtifactRepositoryLayout getLayout();

    void setLayout( ArtifactRepositoryLayout layout );

    String getKey();
    
    // New interface methods for the repository system. 
    
    Artifact find( Artifact artifact );    
}
