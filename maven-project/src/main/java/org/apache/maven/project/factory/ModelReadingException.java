package org.apache.maven.project.factory;

/** @author Jason van Zyl */
public class ModelReadingException
    extends Exception
{
    public ModelReadingException( String s )
    {
        super( s );
    }

    public ModelReadingException( String s,
                                   Throwable throwable )
    {
        super( s, throwable );
    }

    public ModelReadingException( Throwable throwable )
    {
        super( throwable );
    }
}
