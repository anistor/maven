package org.apache.maven.plugin.manager;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;

public interface PluginLoader
{

    PluginDescriptor loadPlugin( Plugin plugin, MavenProject project, MavenSession session );

}
