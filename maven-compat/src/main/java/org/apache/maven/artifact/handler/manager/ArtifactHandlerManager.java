package org.apache.maven.artifact.handler.manager;

import org.apache.maven.artifact.handler.ArtifactHandler;

@Deprecated
public interface ArtifactHandlerManager
{
    String ROLE = ArtifactHandlerManager.class.getName();

    ArtifactHandler getArtifactHandler( String type );
}
