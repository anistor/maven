package org.apache.maven.project.factory;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.io.FileNotFoundException;

/** @author Jason van Zyl */
public class FileModelReaderSource
    implements ModelReaderSource
{
    private File project;

    public FileModelReaderSource( File project )
    {
        this.project = project;
    }

    public Reader getReader()
        throws ModelReaderSourceException
    {
        try
        {
            return new FileReader( project );
        }
        catch ( FileNotFoundException e )
        {
            throw new ModelReaderSourceException( "The project specified does not exist: " + project );
        }
    }
}
