package org.apache.maven.artifact.manager;

import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.repository.legacy.WagonConfigurationException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Deprecated
@Component(role=WagonManager.class) 
public class DefaultWagonManager
    implements WagonManager
{
    @Requirement
    private RepositorySystem repositorySystem;
    
    public Wagon getWagon( String protocol )
        throws UnsupportedProtocolException
    {
        return repositorySystem.getWagon( protocol );
    }

    public Wagon getWagon( Repository repository )
        throws UnsupportedProtocolException, WagonConfigurationException
    {
        return repositorySystem.getWagon( repository );
    }   
}
