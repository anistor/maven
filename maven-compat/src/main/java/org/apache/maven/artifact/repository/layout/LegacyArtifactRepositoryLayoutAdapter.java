package org.apache.maven.artifact.repository.layout;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryAdapter;

public class LegacyArtifactRepositoryLayoutAdapter
    implements ArtifactRepositoryLayout
{
    private org.apache.maven.repository.legacy.repository.layout.ArtifactRepositoryLayout layout;
    
    public LegacyArtifactRepositoryLayoutAdapter( org.apache.maven.repository.legacy.repository.layout.ArtifactRepositoryLayout layout )
    {
        this.layout = layout;
    }

    public String getId()
    {
        return layout.getId();
    }

    public String pathOf( Artifact artifact )
    {
        return layout.pathOf( artifact );
    }

    public String pathOfRemoteRepositoryMetadata( ArtifactMetadata metadata )
    {
        return layout.pathOfRemoteRepositoryMetadata( metadata );        
    }

    public String pathOfLocalRepositoryMetadata( ArtifactMetadata metadata, ArtifactRepository repository )
    {
        return layout.pathOfLocalRepositoryMetadata( metadata, new ArtifactRepositoryAdapter( repository ) );
    }
}
