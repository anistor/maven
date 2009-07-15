package org.apache.maven.artifact.factory;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Deprecated
@Component(role=ArtifactFactory.class)
public class DefaultArtifactFactory
    implements ArtifactFactory
{
    @Requirement
    private RepositorySystem repositorySystem;

    public Artifact createArtifact( String groupId, String artifactId, String version, String scope, String type )
    {
        return repositorySystem.createArtifact( groupId, artifactId, version, scope, type );
    }

    public Artifact createArtifactWithClassifier( String groupId, String artifactId, String version, String type, String classifier )
    {
        return repositorySystem.createArtifactWithClassifier( groupId, artifactId, version, type, classifier );
    }

    public Artifact createArtifact( String groupId, String artifactId, String version, String packaging )
    {
        return repositorySystem.createArtifact( groupId, artifactId, version, packaging );
    }

    public Artifact createDependencyArtifact( Dependency dependency )
    {
        return repositorySystem.createDependencyArtifact( dependency );
    }

    public Artifact createPluginArtifact( Plugin plugin )
    {
        return repositorySystem.createPluginArtifact( plugin );        
    }

    public Artifact createProjectArtifact( String groupId, String artifactId, String version )
    {
        return repositorySystem.createProjectArtifact( groupId, artifactId, version );        
    }
}
