package org.apache.maven.lifecycle.binding;

import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.plan.LifecyclePlannerException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;

import java.util.List;
import java.util.Map;

// FIXME: This needs a better name!
public interface LifecycleBindingManager
{

    String ROLE = LifecycleBindingManager.class.getName();

    LifecycleBindings getDefaultBindings( MavenProject project )
        throws LifecycleSpecificationException;

    LifecycleBindings getBindingsForPackaging( MavenProject project )
        throws LifecycleLoaderException, LifecycleSpecificationException;

    LifecycleBindings getProjectCustomBindings( MavenProject project )
        throws LifecycleLoaderException, LifecycleSpecificationException;

    LifecycleBindings getPluginLifecycleOverlay( PluginDescriptor pluginDescriptor, String lifecycleId, MavenProject project )
        throws LifecycleLoaderException, LifecycleSpecificationException;

    List getReportBindings( MavenProject project )
        throws LifecycleLoaderException, LifecycleSpecificationException;

    List assembleMojoBindingList( List tasks, LifecycleBindings bindings, Map directInvocationPlans, MavenProject project )
        throws LifecycleSpecificationException, LifecyclePlannerException, LifecycleLoaderException;

    List assembleMojoBindingList( List tasks, LifecycleBindings lifecycleBindings, MavenProject project )
        throws LifecycleSpecificationException, LifecyclePlannerException, LifecycleLoaderException;

}
