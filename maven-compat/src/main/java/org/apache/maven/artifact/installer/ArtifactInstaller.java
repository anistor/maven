package org.apache.maven.artifact.installer;

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

@Deprecated
public interface ArtifactInstaller
{
    void install( File source, Artifact artifact, ArtifactRepository localRepository )
        throws ArtifactInstallationException;
}
