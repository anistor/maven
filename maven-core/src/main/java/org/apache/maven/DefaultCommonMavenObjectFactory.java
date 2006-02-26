package org.apache.maven;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;

import java.io.File;

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
}
