package org.apache.maven.project.factory;

import org.apache.maven.model.Parent;

/** @author Jason van Zyl */
public class ParentCoordinateAdapter
    extends Coordinate
{
    private Parent parent;

    public ParentCoordinateAdapter( Parent parent )
    {
        super( parent.getGroupId(), parent.getArtifactId(), parent.getVersion() );
    }
}
