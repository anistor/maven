package org.apache.maven.plugin.loader;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

public interface PluginLoader
{

    Object loadPluginComponent( String role, String roleHint, Plugin plugin, MavenProject project )
        throws ComponentLookupException, PluginLoaderException;

    PluginDescriptor loadPlugin( Plugin plugin, MavenProject project )
        throws PluginLoaderException;

}
