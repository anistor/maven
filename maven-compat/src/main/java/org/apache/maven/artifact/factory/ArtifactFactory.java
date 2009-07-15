package org.apache.maven.artifact.factory;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;

@Deprecated
public interface ArtifactFactory
{
    @Deprecated
    String ROLE = ArtifactFactory.class.getName();
   
    Artifact createArtifact( String groupId, String artifactId, String version, String packaging );

    Artifact createArtifact( String groupId, String artifactId, String version, String scope, String type );

    Artifact createProjectArtifact( String groupId, String artifactId, String version );

    Artifact createArtifactWithClassifier( String groupId, String artifactId, String version, String type, String classifier );
    
    Artifact createPluginArtifact( Plugin plugin );
    
    Artifact createDependencyArtifact( Dependency dependency );
    
    
    /*

    Will restore as necessary

    Artifact createDependencyArtifact( String groupId, String artifactId, VersionRange versionRange, String type, String classifier, String scope );

    Artifact createDependencyArtifact( String groupId, String artifactId, VersionRange versionRange, String type, String classifier, String scope, boolean optional );

    Artifact createDependencyArtifact( String groupId, String artifactId, VersionRange versionRange, String type, String classifier, String scope, String inheritedScope );

    Artifact createDependencyArtifact( String groupId, String artifactId, VersionRange versionRange, String type, String classifier, String scope, String inheritedScope, boolean optional );

    Artifact createBuildArtifact( String groupId, String artifactId, String version, String packaging );

    Artifact createParentArtifact( String groupId, String artifactId, String version );

    Artifact createPluginArtifact( String groupId, String artifactId, VersionRange versionRange );

    Artifact createProjectArtifact( String groupId, String artifactId, String version, String scope );

    Artifact createExtensionArtifact( String groupId, String artifactId, VersionRange versionRange );
    
    */
}
