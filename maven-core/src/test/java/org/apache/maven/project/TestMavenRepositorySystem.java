package org.apache.maven.project;

import org.apache.maven.repository.LegacyRepositorySystem;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.repository.legacy.resolver.ArtifactResolver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Component(role = RepositorySystem.class, hint = "classpath")
public class TestMavenRepositorySystem
    extends LegacyRepositorySystem
{
    @Requirement(hint="classpath")
    private ArtifactResolver artifactResolver;
}
