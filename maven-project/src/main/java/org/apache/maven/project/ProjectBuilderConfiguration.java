package org.apache.maven.project;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.profiles.ProfileManager;

import java.util.Date;
import java.util.Properties;
import java.util.List;

public interface ProjectBuilderConfiguration
{

    ArtifactRepository getLocalRepository();

    ProfileManager getGlobalProfileManager();

    Properties getUserProperties();

    Properties getExecutionProperties();

    ProjectBuilderConfiguration setGlobalProfileManager( ProfileManager globalProfileManager );

    ProjectBuilderConfiguration setLocalRepository( ArtifactRepository localRepository );

    ProjectBuilderConfiguration setUserProperties( Properties userProperties );

    ProjectBuilderConfiguration setExecutionProperties( Properties executionProperties );

    Date getBuildStartTime();

    ProjectBuilderConfiguration setBuildStartTime( Date buildStartTime );

    List<String> getActiveProfileIds();

    void setActiveProfileIds(List<String> activeProfileIds);

}
