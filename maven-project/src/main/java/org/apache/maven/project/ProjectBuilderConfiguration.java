package org.apache.maven.project;

import org.apache.maven.artifact.repository.ArtifactRepository;

import java.util.Date;
import java.util.Properties;

public interface ProjectBuilderConfiguration
{

    ArtifactRepository getLocalRepository();

    Properties getUserProperties();

    Properties getExecutionProperties();

    ProjectBuilderConfiguration setLocalRepository( ArtifactRepository localRepository );

    ProjectBuilderConfiguration setUserProperties( Properties userProperties );

    ProjectBuilderConfiguration setExecutionProperties( Properties executionProperties );

    Date getBuildStartTime();

    ProjectBuilderConfiguration setBuildStartTime( Date buildStartTime );

}
