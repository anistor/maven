package org.apache.maven.project.factory;

import java.io.Reader;

/** @author Jason van Zyl */
public interface ModelReaderSource
{
    Reader getReader()
        throws ModelReaderSourceException;
}
