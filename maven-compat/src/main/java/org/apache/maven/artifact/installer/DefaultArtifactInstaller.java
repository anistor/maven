package org.apache.maven.artifact.installer;

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryAdapter;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Deprecated
@Component(role = ArtifactInstaller.class)
public class DefaultArtifactInstaller
    implements ArtifactInstaller
{
    @Requirement
    private RepositorySystem repositorySystem;
    
    public void install( File source, Artifact artifact, ArtifactRepository localRepository )
        throws ArtifactInstallationException
    {
        try
        {
            repositorySystem.install( source, artifact, new ArtifactRepositoryAdapter( localRepository ) );
        }
        catch ( org.apache.maven.repository.legacy.installer.ArtifactInstallationException e )
        {
            throw new ArtifactInstallationException( e );
        }
    }
}
