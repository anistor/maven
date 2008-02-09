package org.apache.maven.project.factory;

import org.apache.maven.project.MavenProject;
import org.apache.maven.project.factory.ModelReaderSource;
import org.apache.maven.project.factory.ModelReaderSourceException;

/** @author Jason van Zyl */
public interface ProjectFactory
{
    MavenProject build( ModelReaderSource source )
        throws ModelReaderSourceException, ModelReadingException;
}
