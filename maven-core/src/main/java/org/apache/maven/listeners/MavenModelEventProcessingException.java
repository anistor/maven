package org.apache.maven.listeners;

public class MavenModelEventProcessingException
    extends Exception
{
    public MavenModelEventProcessingException( String arg0, Throwable arg1 )
    {
        super( arg0, arg1 );
    }

    public MavenModelEventProcessingException( String arg0 )
    {
        super( arg0 );
    }

    public MavenModelEventProcessingException( Throwable arg0 )
    {
        super( arg0 );
    }    
}
