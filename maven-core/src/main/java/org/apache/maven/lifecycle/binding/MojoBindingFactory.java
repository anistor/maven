package org.apache.maven.lifecycle.binding;

import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.project.MavenProject;

public interface MojoBindingFactory
{

    String ROLE = MojoBindingFactory.class.getName();

    MojoBinding parseMojoBinding( String bindingSpec, MavenProject project, boolean allowPrefixReference )
        throws LifecycleSpecificationException, LifecycleLoaderException;

    MojoBinding createMojoBinding( String groupId, String artifactId, String version, String goal, MavenProject project );

    MojoBinding parseMojoBinding( String bindingSpec )
        throws LifecycleSpecificationException;

}
