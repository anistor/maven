package org.apache.maven;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.legacy.repository.ArtifactRepository;

public interface ProjectDependenciesResolver
{
    public Set<Artifact> resolve( MavenProject project, Collection<String> scopes, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws ArtifactResolutionException, ArtifactNotFoundException;
}
