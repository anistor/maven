package org.apache.maven.artifact.repository;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayoutAdapter;
import org.apache.maven.artifact.repository.layout.LegacyArtifactRepositoryLayoutAdapter;

public class LegacyArtifactRepositoryAdapter
    implements ArtifactRepository
{
    private org.apache.maven.repository.legacy.repository.ArtifactRepository repository;

    public LegacyArtifactRepositoryAdapter( org.apache.maven.repository.legacy.repository.ArtifactRepository repository )
    {
        this.repository = repository;
    }

    public Artifact find( Artifact artifact )
    {
        return repository.find( artifact );
    }

    public String getBasedir()
    {
        return repository.getBasedir();
    }

    public String getId()
    {
        return repository.getId();
    }

    public String getKey()
    {
        return repository.getKey();
    }

    public ArtifactRepositoryLayout getLayout()
    {
        return new LegacyArtifactRepositoryLayoutAdapter( repository.getLayout() );
    }

    public String getProtocol()
    {
        return repository.getProtocol();
    }

    public ArtifactRepositoryPolicy getReleases()
    {
        return new LegacyArtifactRepositoryPolicyAdapter( repository.getReleases() );
    }

    public ArtifactRepositoryPolicy getSnapshots()
    {
        return new LegacyArtifactRepositoryPolicyAdapter( repository.getSnapshots() );
    }

    public String getUrl()
    {
        return repository.getUrl();
    }

    public String pathOf( Artifact artifact )
    {
        return repository.pathOf( artifact );
    }

    public String pathOfLocalRepositoryMetadata( ArtifactMetadata metadata, ArtifactRepository repository )
    {
        return repository.pathOfLocalRepositoryMetadata( metadata, repository );        
    }

    public String pathOfRemoteRepositoryMetadata( ArtifactMetadata artifactMetadata )
    {
        return repository.pathOfRemoteRepositoryMetadata( artifactMetadata );
    }

    public void setId( String id )
    {
        repository.setId( id );
    }

    public void setLayout( ArtifactRepositoryLayout layout )
    {
        repository.setLayout( new ArtifactRepositoryLayoutAdapter( layout ) );
    }

    public void setReleaseUpdatePolicy( ArtifactRepositoryPolicy policy )
    {
        repository.setReleaseUpdatePolicy( new ArtifactRepositoryPolicyAdapter( policy ) );
    }

    public void setSnapshotUpdatePolicy( ArtifactRepositoryPolicy policy )
    {
        repository.setSnapshotUpdatePolicy( new ArtifactRepositoryPolicyAdapter( policy ) );
    }

    public void setUrl( String url )
    {
        repository.setUrl( url );
    }
}
