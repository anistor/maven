package org.apache.maven;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.settings.RuntimeInfo;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;

/**
 * A small utility that provides a common place for creating object instances that are used frequently in Maven:
 * ArtifactRepositories, Artifacts .... A facade for all factories we have lying around. This could very well
 * belong somewhere else but is here for the maven-embedder-refactor.
 *
 * @author Jason van Zyl
 */
public class DefaultCommonMavenObjectFactory
    implements CommonMavenObjectFactory
{
    private ArtifactRepositoryLayout repositoryLayout;

    private ArtifactRepositoryFactory artifactRepositoryFactory;

    private MavenSettingsBuilder settingsBuilder;

    public ArtifactRepository createLocalRepository( File localRepositoryPath,
                                                     boolean offline,
                                                     boolean updateSnapshots,
                                                     String globalChecksumPolicy )
    {
        String localRepositoryUrl = localRepositoryPath.getAbsolutePath();

        if ( !localRepositoryUrl.startsWith( "file:" ) )
        {
            localRepositoryUrl = "file://" + localRepositoryUrl;
        }

        ArtifactRepository localRepository = new DefaultArtifactRepository( "local", localRepositoryUrl, repositoryLayout );

        boolean snapshotPolicySet = false;

        if ( offline )
        {
            snapshotPolicySet = true;
        }

        if ( !snapshotPolicySet && updateSnapshots )
        {
            artifactRepositoryFactory.setGlobalUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS );
        }

        artifactRepositoryFactory.setGlobalChecksumPolicy( globalChecksumPolicy );

        return localRepository;
    }

    public Settings buildSettings( String userSettingsPath,
                                   boolean interactive,
                                   boolean offline,
                                   boolean usePluginRegistry,
                                   Boolean pluginUpdateOverride )
        throws SettingsConfigurationException
    {
        Settings settings = null;

        try
        {
            if ( userSettingsPath != null )
            {
                File userSettingsFile = new File( userSettingsPath );

                if ( userSettingsFile.exists() && !userSettingsFile.isDirectory() )
                {
                    settings = settingsBuilder.buildSettings( userSettingsFile );

                    System.out.println( "settings local repository = " + settings.getLocalRepository() );
                }
                else
                {
                    System.out.println( "WARNING: Alternate user settings file: " + userSettingsPath +
                        " is invalid. Using default path." );
                }
            }

            if ( settings == null )
            {
                settings = settingsBuilder.buildSettings();
            }
        }
        catch ( IOException e )
        {
            throw new SettingsConfigurationException( "Error reading settings file", e );
        }
        catch ( XmlPullParserException e )
        {
            throw new SettingsConfigurationException( e.getMessage(), e.getDetail(), e.getLineNumber(),
                                                      e.getColumnNumber() );
        }

        if ( offline )
        {
            settings.setOffline( true );
        }

        settings.setInteractiveMode( interactive );

        settings.setUsePluginRegistry( usePluginRegistry );

        RuntimeInfo runtimeInfo = new RuntimeInfo( settings );

        runtimeInfo.setPluginUpdateOverride( pluginUpdateOverride );

        settings.setRuntimeInfo( runtimeInfo );

        return settings;
    }
}
