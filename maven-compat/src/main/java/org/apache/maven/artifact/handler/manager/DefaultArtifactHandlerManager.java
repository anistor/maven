package org.apache.maven.artifact.handler.manager;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.LegacyArtifactHandlerAdapter;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Deprecated
@Component(role=ArtifactHandlerManager.class)
public class DefaultArtifactHandlerManager
    implements ArtifactHandlerManager
{
    @Requirement
    private RepositorySystem repositorySystem;
    
    public ArtifactHandler getArtifactHandler( String type )
    {
        return new LegacyArtifactHandlerAdapter( repositorySystem.getArtifactHandler( type ) );
    }    
}
