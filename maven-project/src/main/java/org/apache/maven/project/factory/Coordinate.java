package org.apache.maven.project.factory;

/** @author Jason van Zyl */
public class Coordinate
{
    private String groupId;

    private String artifactId;

    private String version;

    public Coordinate( String groupId,
                       String artifactId,
                       String version )
    {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getVersion()
    {
        return version;
    }
}
