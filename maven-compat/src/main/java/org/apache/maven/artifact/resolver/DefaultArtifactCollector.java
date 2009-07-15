package org.apache.maven.artifact.resolver;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.repository.legacy.metadata.ArtifactMetadataSource;
import org.apache.maven.repository.legacy.repository.ArtifactRepository;
import org.apache.maven.repository.legacy.resolver.ResolutionListener;
import org.apache.maven.repository.legacy.resolver.conflict.ConflictResolver;
import org.apache.maven.repository.legacy.resolver.filter.ArtifactFilter;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Deprecated
@Component(role=ArtifactCollector.class)
public class DefaultArtifactCollector
    implements ArtifactCollector
{    
    @Requirement
    private RepositorySystem repositorySystem;

    public ArtifactResolutionResult collect( Set<Artifact> artifacts, Artifact originatingArtifact, Map managedVersions, ArtifactRepository localRepository,
                                             List<ArtifactRepository> remoteRepositories, ArtifactMetadataSource source, ArtifactFilter filter, List<ResolutionListener> listeners,
                                             List<ConflictResolver> conflictResolvers )
    {
        return repositorySystem.collect( artifacts, originatingArtifact, managedVersions, localRepository, remoteRepositories, source, filter, listeners, conflictResolvers );
    }

    public ArtifactResolutionResult collect( Set<Artifact> artifacts, Artifact originatingArtifact, Map managedVersions, ArtifactRepository localRepository,
                                             List<ArtifactRepository> remoteRepositories, ArtifactMetadataSource source, ArtifactFilter filter, List<ResolutionListener> listeners )
    {
        return repositorySystem.collect( artifacts, originatingArtifact, managedVersions, localRepository, remoteRepositories, source, filter, listeners );
    }
}
