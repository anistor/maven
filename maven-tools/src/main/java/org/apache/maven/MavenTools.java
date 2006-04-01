package org.apache.maven;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.settings.Settings;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;

import java.io.File;
import java.util.List;

/**
 * @author Jason van Zyl
 */
public interface MavenTools
{
    static String ROLE = MavenTools.class.getName();

    ArtifactRepository createLocalRepository( File localRepositoryPath,
                                              boolean offline,
                                              boolean updateSnapshots,
                                              String globalChecksumPolicy );

    ArtifactRepository createRepository( String repositoryId,
                                         String repositoryUrl,
                                         boolean offline,
                                         boolean updateSnapshots,
                                         String globalChecksumPolicy );


    Settings buildSettings( File userSettingsPath,
                            File globalSettingsPath,
                            boolean interactive,
                            boolean offline,
                            boolean usePluginRegistry,
                            Boolean pluginUpdateOverride )
        throws SettingsConfigurationException;

    List buildArtifactRepositories( List repositories )
        throws InvalidRepositoryException;

    ArtifactRepository buildDeploymentArtifactRepository( DeploymentRepository repo )
        throws InvalidRepositoryException;

    ArtifactRepository buildArtifactRepository( Repository repo )
        throws InvalidRepositoryException;

}
