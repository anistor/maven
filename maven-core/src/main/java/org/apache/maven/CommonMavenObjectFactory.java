package org.apache.maven;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.settings.Settings;

import java.io.File;

/**
 * @author Jason van Zyl
 */
public interface CommonMavenObjectFactory
{
    static String ROLE = CommonMavenObjectFactory.class.getName();

    ArtifactRepository createLocalRepository( File localRepositoryPath,
                                              boolean offline,
                                              boolean updateSnapshots,
                                              String globalChecksumPolicy );

    public Settings buildSettings( String userSettingsPath,
                                   boolean interactive,
                                   boolean offline,
                                   boolean usePluginRegistry,
                                   Boolean pluginUpdateOverride )
        throws SettingsConfigurationException;
}
