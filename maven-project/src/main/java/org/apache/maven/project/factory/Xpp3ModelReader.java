package org.apache.maven.project.factory;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.factory.ModelReaderSource;
import org.apache.maven.project.factory.ModelReaderSourceException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;

/** @author Jason van Zyl */
public class Xpp3ModelReader
    implements ModelReader
{
    private MavenXpp3Reader reader;

    public Model read( ModelReaderSource source )
        throws ModelReaderSourceException, ModelReadingException
    {
        try
        {
            return reader.read( source.getReader() );
        }
        catch ( IOException e )
        {
            return null;
        }
        catch ( XmlPullParserException e )
        {
            return null;
        }
    }
}
