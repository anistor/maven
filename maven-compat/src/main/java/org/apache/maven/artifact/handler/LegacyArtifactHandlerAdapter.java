package org.apache.maven.artifact.handler;

public class LegacyArtifactHandlerAdapter
    implements ArtifactHandler
{
    private org.apache.maven.repository.legacy.handler.ArtifactHandler handler;
    
    public LegacyArtifactHandlerAdapter( org.apache.maven.repository.legacy.handler.ArtifactHandler handler )
    {
        this.handler = handler;
    }

    public String getClassifier()
    {
        return handler.getClassifier();
    }

    public String getDirectory()
    {
        return handler.getDirectory();
    }

    public String getExtension()
    {
        return handler.getExtension();
    }

    public String getLanguage()
    {
        return handler.getLanguage();
    }

    public String getPackaging()
    {
        return handler.getPackaging();
    }

    public boolean isAddedToClasspath()
    {
        return handler.isAddedToClasspath();
    }

    public boolean isIncludesDependencies()
    {
        return handler.isIncludesDependencies();
    }
}
