package org.apache.maven.artifact.deployer;

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryAdapter;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Deprecated
@Component(role=ArtifactDeployer.class)
public class DefaultArtifactDeployer
    implements ArtifactDeployer
{
    @Requirement
    private RepositorySystem repositorySystem;

    public void deploy( File source, Artifact artifact, ArtifactRepository deploymentRepository, ArtifactRepository localRepository )
        throws ArtifactDeploymentException
    {
        try
        {
            repositorySystem.deploy( source, artifact, new ArtifactRepositoryAdapter( deploymentRepository ), new ArtifactRepositoryAdapter( localRepository ) );
        }
        catch ( org.apache.maven.repository.legacy.deployer.ArtifactDeploymentException e )
        {
            throw new ArtifactDeploymentException( e );
        }
    }
}
