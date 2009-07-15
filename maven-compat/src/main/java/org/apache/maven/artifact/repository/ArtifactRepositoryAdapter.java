package org.apache.maven.artifact.repository;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayoutAdapter;
import org.apache.maven.artifact.repository.layout.LegacyArtifactRepositoryLayoutAdapter;
import org.apache.maven.repository.legacy.repository.ArtifactRepositoryPolicy;
import org.apache.maven.repository.legacy.repository.layout.ArtifactRepositoryLayout;

public class ArtifactRepositoryAdapter
    implements org.apache.maven.repository.legacy.repository.ArtifactRepository
{
    private ArtifactRepository repository;

    public ArtifactRepositoryAdapter( ArtifactRepository repository )
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
        return new ArtifactRepositoryLayoutAdapter( repository.getLayout() );
    }

    public String getProtocol()
    {
        return repository.getProtocol();
    }

    public ArtifactRepositoryPolicy getReleases()
    {
        return new ArtifactRepositoryPolicyAdapter( repository.getReleases() );
    }

    public ArtifactRepositoryPolicy getSnapshots()
    {
        return new ArtifactRepositoryPolicyAdapter( repository.getSnapshots() );
    }

    public String getUrl()
    {
        return repository.getUrl();
    }

    public String pathOf( Artifact artifact )
    {
        return repository.pathOf( artifact );
    }

    public String pathOfLocalRepositoryMetadata( ArtifactMetadata metadata, org.apache.maven.repository.legacy.repository.ArtifactRepository repository )
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
        repository.setLayout( new LegacyArtifactRepositoryLayoutAdapter( layout ) );
    }

    public void setReleaseUpdatePolicy( ArtifactRepositoryPolicy policy )
    {
        repository.setReleaseUpdatePolicy( new LegacyArtifactRepositoryPolicyAdapter( policy ) );
    }

    public void setSnapshotUpdatePolicy( ArtifactRepositoryPolicy policy )
    {
        repository.setSnapshotUpdatePolicy( new LegacyArtifactRepositoryPolicyAdapter( policy ) );
    }

    public void setUrl( String url )
    {
        repository.setUrl( url );
    }
}
