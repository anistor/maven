package org.apache.maven.project;

import org.apache.maven.artifact.repository.ArtifactRepository;

import java.util.Date;
import java.util.Properties;

public class DefaultProjectBuilderConfiguration
    implements ProjectBuilderConfiguration
{

    private ArtifactRepository localRepository;

    private Properties userProperties;

    private Properties executionProperties = System.getProperties();

    private Date buildStartTime;

    public DefaultProjectBuilderConfiguration()
    {
    }

    public ProjectBuilderConfiguration setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
        return this;
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public ProjectBuilderConfiguration setUserProperties( Properties userProperties )
    {
        this.userProperties = userProperties;
        return this;
    }

    public Properties getUserProperties()
    {
        if ( userProperties == null )
        {
            userProperties = new Properties();
        }

        return userProperties;
    }

    public Properties getExecutionProperties()
    {
        return executionProperties;
    }

    public ProjectBuilderConfiguration setExecutionProperties( Properties executionProperties )
    {
        this.executionProperties = executionProperties;
        return this;
    }

    public Date getBuildStartTime()
    {
        return buildStartTime;
    }

    public ProjectBuilderConfiguration setBuildStartTime( Date buildStartTime )
    {
        this.buildStartTime = buildStartTime;
        return this;
    }

}
