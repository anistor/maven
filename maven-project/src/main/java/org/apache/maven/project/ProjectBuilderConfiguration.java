package org.apache.maven.project;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.profiles.ProfileManager;

import java.util.Date;
import java.util.Properties;

public interface ProjectBuilderConfiguration
{

    ArtifactRepository getLocalRepository();

    ProfileManager getGlobalProfileManager();

    Properties getUserProperties();

    Properties getExecutionProperties();

    Date getBuildStartTime();

    ProjectBuilderConfiguration setGlobalProfileManager( ProfileManager globalProfileManager );

    ProjectBuilderConfiguration setLocalRepository( ArtifactRepository localRepository );

    ProjectBuilderConfiguration setUserProperties( Properties userProperties );

    ProjectBuilderConfiguration setExecutionProperties( Properties executionProperties );

    ProjectBuilderConfiguration setBuildStartTime( Date buildStartTime );

}
