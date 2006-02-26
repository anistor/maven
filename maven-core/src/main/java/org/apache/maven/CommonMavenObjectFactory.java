package org.apache.maven;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.io.File;

/**
 * @author Jason van Zyl
 */
public interface CommonMavenObjectFactory
{
     ArtifactRepository createLocalRepository( File localRepositoryPath,
                                               boolean offline,
                                               boolean updateSnapshots,
                                               String globalChecksumPolicy );
}
