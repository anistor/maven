package org.apache.maven.artifact.deployer;

@Deprecated
public class ArtifactDeploymentException
    extends org.apache.maven.repository.legacy.deployer.ArtifactDeploymentException
{
    public ArtifactDeploymentException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public ArtifactDeploymentException( String message )
    {
        super( message );
    }

    public ArtifactDeploymentException( Throwable cause )
    {
        super( cause );
    }    
}
