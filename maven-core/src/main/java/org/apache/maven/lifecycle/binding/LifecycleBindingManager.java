package org.apache.maven.lifecycle.binding;

import org.apache.maven.lifecycle.LifecycleBindings;
import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;

// FIXME: This needs a better name!
public interface LifecycleBindingManager
{

    String ROLE = LifecycleBindingManager.class.getName();

    LifecycleBindings getDefaultBindings()
        throws LifecycleSpecificationException;

    LifecycleBindings getBindingsForPackaging( MavenProject project )
        throws LifecycleLoaderException, LifecycleSpecificationException;

    LifecycleBindings getProjectCustomBindings( MavenProject project )
        throws LifecycleLoaderException, LifecycleSpecificationException;

    LifecycleBindings getPluginLifecycleOverlay( PluginDescriptor pluginDescriptor, String lifecycleId )
        throws LifecycleLoaderException, LifecycleSpecificationException;
}
