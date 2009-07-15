package org.apache.maven.artifact.repository.layout;

import org.codehaus.plexus.component.annotations.Component;

@Deprecated
@Component(role=ArtifactRepositoryLayout.class,hint="legacy")
public class LegacyRepositoryLayout
    extends org.apache.maven.repository.legacy.repository.layout.LegacyRepositoryLayout
    implements ArtifactRepositoryLayout
{
}
