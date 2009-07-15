package org.apache.maven.artifact.resolver;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.repository.legacy.metadata.ArtifactMetadataSource;
import org.apache.maven.repository.legacy.repository.ArtifactRepository;
import org.apache.maven.repository.legacy.resolver.ResolutionListener;
import org.apache.maven.repository.legacy.resolver.conflict.ConflictResolver;
import org.apache.maven.repository.legacy.resolver.filter.ArtifactFilter;

@Deprecated
public interface ArtifactCollector
{
    ArtifactResolutionResult collect( Set<Artifact> artifacts,
                                      Artifact originatingArtifact,
                                      Map managedVersions,
                                      ArtifactRepository localRepository,
                                      List<ArtifactRepository> remoteRepositories,
                                      ArtifactMetadataSource source,
                                      ArtifactFilter filter,
                                      List<ResolutionListener> listeners,
                                      List<ConflictResolver> conflictResolvers );

    // used by maven-dependency-tree and maven-dependency-plugin
    @Deprecated
    ArtifactResolutionResult collect( Set<Artifact> artifacts, Artifact originatingArtifact, Map managedVersions,
                                      ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories,
                                      ArtifactMetadataSource source, ArtifactFilter filter,
                                      List<ResolutionListener> listeners );    
}
