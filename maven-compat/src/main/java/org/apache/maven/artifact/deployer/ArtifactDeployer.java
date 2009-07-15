package org.apache.maven.artifact.deployer;

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

@Deprecated
public interface ArtifactDeployer
{
    void deploy( File source, Artifact artifact, ArtifactRepository deploymentRepository, ArtifactRepository localRepository )
        throws ArtifactDeploymentException;
}
