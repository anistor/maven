package org.apache.maven.artifact.repository.layout;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.LegacyArtifactRepositoryAdapter;

public class ArtifactRepositoryLayoutAdapter
    implements org.apache.maven.repository.legacy.repository.layout.ArtifactRepositoryLayout
{
    private ArtifactRepositoryLayout layout;
    
    public ArtifactRepositoryLayoutAdapter( ArtifactRepositoryLayout layout )
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

    public String pathOfLocalRepositoryMetadata( ArtifactMetadata metadata, org.apache.maven.repository.legacy.repository.ArtifactRepository repository )
    {
        return layout.pathOfLocalRepositoryMetadata( metadata, new LegacyArtifactRepositoryAdapter( repository ) );
    }
}
