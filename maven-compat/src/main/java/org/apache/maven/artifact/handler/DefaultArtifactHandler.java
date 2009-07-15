package org.apache.maven.artifact.handler;

import org.codehaus.plexus.component.annotations.Component;

@Component(role=ArtifactHandler.class) 
public class DefaultArtifactHandler
    implements ArtifactHandler
{
    private String extension;

    private String type;

    private String classifier;

    private String directory;

    private String packaging;

    private boolean includesDependencies;

    private String language;

    private boolean addedToClasspath;

    public DefaultArtifactHandler()
    {
    }

    public DefaultArtifactHandler( String type )
    {
        this.type = type;
    }

    public String getExtension()
    {
        if ( extension == null )
        {
            extension = type;
        }
        return extension;
    }

    public String getType()
    {
        return type;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public String getDirectory()
    {
        if ( directory == null )
        {
            directory = getPackaging() + "s";
        }
        return directory;
    }

    public String getPackaging()
    {
        if ( packaging == null )
        {
            packaging = type;
        }
        return packaging;
    }

    public boolean isIncludesDependencies()
    {
        return includesDependencies;
    }

    public String getLanguage()
    {
        if ( language == null )
        {
            language = "none";
        }

        return language;
    }

    public boolean isAddedToClasspath()
    {
        return addedToClasspath;
    }
}