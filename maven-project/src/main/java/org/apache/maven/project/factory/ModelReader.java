package org.apache.maven.project.factory;

import org.apache.maven.model.Model;
import org.apache.maven.project.factory.ModelReaderSource;
import org.apache.maven.project.factory.ModelReaderSourceException;

/** @author Jason van Zyl */
public interface ModelReader
{
    Model read( ModelReaderSource source )
        throws ModelReaderSourceException, ModelReadingException;
}
