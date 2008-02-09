package org.apache.maven.project.factory;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolver;

import java.io.Reader;

/** @author Jason van Zyl */
public class CoordinateModelReaderSource
    implements ModelReaderSource
{
    private Coordinate coordinate;
    
    private ArtifactResolver artifactResolver;

    public CoordinateModelReaderSource( Coordinate coordinate, ArtifactResolver artifactResolver )
    {
        this.coordinate = coordinate;

        this.artifactResolver = artifactResolver;
    }

    public Reader getReader()
        throws ModelReaderSourceException
    {
        
        // Now we get into the business of looking for the POMs locally and retrieving them
        // if they cannot be found ...

        // check local

        return null;
    }
}
