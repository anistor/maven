package org.apache.maven.artifact.repository;

public class ArtifactRepositoryPolicyAdapter
    extends org.apache.maven.repository.legacy.repository.ArtifactRepositoryPolicy
{
    private ArtifactRepositoryPolicy policy;

    public ArtifactRepositoryPolicyAdapter( ArtifactRepositoryPolicy policy )
    {
        this.policy = policy;
    }        
}
