package org.apache.maven.plugin.loader;

import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

public interface PluginLoader
{

    Object loadPluginComponent( String role, String roleHint, Plugin plugin, MavenProject project )
        throws ComponentLookupException, PluginLoaderException;

    PluginDescriptor loadPlugin( Plugin plugin, MavenProject project )
        throws PluginLoaderException;

    PluginDescriptor loadPlugin( MojoBinding mojoBinding, MavenProject project )
        throws PluginLoaderException;

    PluginDescriptor loadReportPlugin( ReportPlugin reportPlugin, MavenProject project )
        throws PluginLoaderException;

    PluginDescriptor loadReportPlugin( MojoBinding mojoBinding, MavenProject project )
        throws PluginLoaderException;

    PluginDescriptor findPluginForPrefix( String prefix, MavenProject project )
        throws PluginLoaderException;
    
}
