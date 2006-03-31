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

    // ----------------------------------------------------------------------------
    // ArtifactRepository
    // ----------------------------------------------------------------------------

    public ArtifactRepository createLocalRepository( File directory,
                                                     boolean offline,
                                                     boolean updateSnapshots,
                                                     String globalChecksumPolicy )
    {
        String localRepositoryUrl = directory.getAbsolutePath();

        if ( !localRepositoryUrl.startsWith( "file:" ) )
        {
            localRepositoryUrl = "file://" + localRepositoryUrl;
        }

        return createRepository( "local", localRepositoryUrl, false, true, ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN );
    }

    protected ArtifactRepository createRepository( String repositoryId,
                                                   String repositoryUrl,
                                                   boolean offline,
                                                   boolean updateSnapshots,
                                                   String globalChecksumPolicy )
    {
        ArtifactRepository localRepository = new DefaultArtifactRepository( repositoryId, repositoryUrl, repositoryLayout );

        boolean snapshotPolicySet = false;

        if ( offline )
        {
            snapshotPolicySet = true;
        }

        if ( !snapshotPolicySet && updateSnapshots )
        {
            artifactRepositoryFactory.setGlobalUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS );
        }

        artifactRepositoryFactory.setGlobalChecksumPolicy( globalChecksumPolicy  );

        return localRepository;
    }


    // ----------------------------------------------------------------------------
    // Settings
    // ----------------------------------------------------------------------------

    public Settings buildSettings( File userSettingsPath,
                                   File globalSettingsPath,
                                   boolean interactive,
                                   boolean offline,
                                   boolean usePluginRegistry,
                                   Boolean pluginUpdateOverride )
        throws SettingsConfigurationException
    {
        Settings settings;

        try
        {
                settings = settingsBuilder.buildSettings( userSettingsPath, globalSettingsPath );
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
