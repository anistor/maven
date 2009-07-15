package org.apache.maven.artifact.installer;

@Deprecated
public class ArtifactInstallationException
    extends org.apache.maven.repository.legacy.installer.ArtifactInstallationException
{
    public ArtifactInstallationException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public ArtifactInstallationException( String message )
    {
        super( message );
    }

    public ArtifactInstallationException( Throwable cause )
    {
        super( cause );
    }
}
