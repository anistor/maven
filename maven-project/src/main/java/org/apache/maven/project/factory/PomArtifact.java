package org.apache.maven.project.factory;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.versioning.VersionRange;

/** @author Jason van Zyl */
public class PomArtifact
    extends DefaultArtifact
{
    public PomArtifact( String groupId, String artifactId, String version )
    {
        super( groupId, artifactId, VersionRange.createFromVersion( version ), "pom", null, false, Artifact.SCOPE_RUNTIME, null );
    }
}
