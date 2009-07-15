package org.apache.maven.artifact.resolver;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.wagon.events.TransferListener;

@Deprecated
public interface ArtifactResolver
{
    // USED BY MAVEN ASSEMBLY PLUGIN 2.2-beta-2 
    @Deprecated
    String ROLE = ArtifactResolver.class.getName();

    // USED BY SUREFIRE
    @Deprecated
    ArtifactResolutionResult resolveTransitively( Set<Artifact> artifacts, Artifact originatingArtifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories,
                                                  ArtifactMetadataSource source, ArtifactFilter filter )
        throws ArtifactResolutionException, ArtifactNotFoundException;

    // USED BY MAVEN ASSEMBLY PLUGIN
    @Deprecated
    public ArtifactResolutionResult resolveTransitively( Set<Artifact> artifacts, Artifact originatingArtifact, Map managedVersions, ArtifactRepository localRepository,
                                                         List<ArtifactRepository> remoteRepositories, ArtifactMetadataSource source, ArtifactFilter filter )
        throws ArtifactResolutionException, ArtifactNotFoundException;

    // USED BY REMOTE RESOURCES PLUGIN
    @Deprecated
    void resolve( Artifact artifact, List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactResolutionException, ArtifactNotFoundException;

    // USED BY REMOTE RESOURCES PLUGIN
    @Deprecated
    void resolve( Artifact artifact, List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository, TransferListener downloadMonitor )
        throws ArtifactResolutionException, ArtifactNotFoundException;

    // USED BY ARCHETYPE DOWNLOADER
    @Deprecated
    void resolveAlways( Artifact artifact, List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactResolutionException, ArtifactNotFoundException;
}
