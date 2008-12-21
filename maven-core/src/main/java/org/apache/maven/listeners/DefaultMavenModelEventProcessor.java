package org.apache.maven.listeners;

import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.shared.model.ModelEventListener;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Component(role = MavenModelEventProcessor.class)
public class DefaultMavenModelEventProcessor
    implements MavenModelEventProcessor
{
    @Requirement(role = ModelEventListener.class)
    List<MavenModelEventListener> listeners;

    public void processModelContainers( MavenSession session )
        throws MavenModelEventProcessingException
    {        
        for( MavenModelEventListener listener : listeners )
        {
            listener.processModelContainers( session );
        }
    }        
}
