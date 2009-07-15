package org.apache.maven.artifact.repository;

public class LegacyArtifactRepositoryPolicyAdapter
    extends ArtifactRepositoryPolicy
{
    private org.apache.maven.repository.legacy.repository.ArtifactRepositoryPolicy policy;

    public LegacyArtifactRepositoryPolicyAdapter( org.apache.maven.repository.legacy.repository.ArtifactRepositoryPolicy policy )
    {
        this.policy = policy;
    }
}
