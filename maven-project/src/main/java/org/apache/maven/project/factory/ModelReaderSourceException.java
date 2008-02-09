package org.apache.maven.project.factory;

/** @author Jason van Zyl */
public class ModelReaderSourceException
    extends Exception
{
    public ModelReaderSourceException( String s )
    {
        super( s );
    }

    public ModelReaderSourceException( String s,
                                        Throwable throwable )
    {
        super( s, throwable );
    }

    public ModelReaderSourceException( Throwable throwable )
    {
        super( throwable );
    }
}
