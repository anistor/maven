package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleBindings;
import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.binding.LifecycleBindingManager;
import org.apache.maven.plugin.PluginMappingManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;

import java.util.List;

public class DefaultLifecyclePlanner
    implements LifecyclePlanner, LogEnabled
{

    private Logger logger;

    private PluginMappingManager pluginMappingManager;

    private LifecycleBindingManager lifecycleBindingManager;

    public LifecyclePlan constructLifecyclePlan( List tasks, MavenProject project )
        throws LifecycleLoaderException, LifecycleSpecificationException
    {
        LifecycleBindings defaultBindings = lifecycleBindingManager.getDefaultBindings();
        LifecycleBindings packagingBindings = lifecycleBindingManager.getBindingsForPackaging( project );
        LifecycleBindings projectBindings = lifecycleBindingManager.getProjectCustomBindings( project );

        LifecyclePlan plan = new DefaultLifecyclePlan( defaultBindings, packagingBindings, projectBindings );
        
        return plan;
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

}
