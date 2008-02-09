package org.apache.maven.project.factory;

import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.project.MavenProject;

import java.util.LinkedList;
import java.util.List;

/** @author Jason van Zyl */
public class DefaultProjectFactory
    implements ProjectFactory
{
    /** @plexus.requirement */
    private ArtifactResolver artifactResolver;

    // Things we need to account for:
    // - different formats with different versions
    // - mixins
    // - exactly where the error occured in parsing any format
    // - differentiate between parsing, i/o, or any other errors
    // - the model reader implementer should be able to provide exacting detail on errors
    // - we must preserve the original model for each project along the way
    // - how best to share projects that are parsed so they can be cached for long-lived processes
    // - adding a listener so that elements can be picked up like extensions or profiles so that we don't have to process them again

    // Take the source, determine the model format and version of the format
    // - could be XML element-based, version 4.0.0, or newer
    // - could be XML attribute-based, version 1.0.0

    /** @plexus.requirement */
    private ModelReader modelReader;
    
    public MavenProject build( ModelReaderSource source )
        throws ModelReaderSourceException, ModelReadingException
    {
        Model model = modelReader.read( source );

        // We need a copy of the unadulterated model
        // The project needs to adulterated model
        // Think about how to build up the projects/models to be efficient
        // Start the tests

        List lineage = new LinkedList();

        Model currentModel = model;

        lineage.add( currentModel );

        while( currentModel.getParent() != null )
        {
            Parent parent = model.getParent();

            Model parentModel = modelReader.read(
                new CoordinateModelReaderSource( new ParentCoordinateAdapter( parent ), artifactResolver ) );

            currentModel = parentModel;

            lineage.add( currentModel );
        }

        MavenProject project = new MavenProject();

        return project;
    }
}
