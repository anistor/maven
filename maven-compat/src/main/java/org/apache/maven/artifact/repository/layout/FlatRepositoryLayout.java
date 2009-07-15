package org.apache.maven.artifact.repository.layout;

import org.codehaus.plexus.component.annotations.Component;

@Deprecated
@Component(role=ArtifactRepositoryLayout.class,hint="flat")
public class FlatRepositoryLayout
    extends org.apache.maven.repository.legacy.repository.layout.FlatRepositoryLayout
    implements ArtifactRepositoryLayout
{
}
