package org.apache.maven.lifecycle.binding;

import org.apache.maven.lifecycle.LifecycleBindingLoader;
import org.apache.maven.lifecycle.LifecycleBindings;
import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.LifecycleWalker;
import org.apache.maven.lifecycle.MojoBinding;
import org.apache.maven.lifecycle.parser.LegacyLifecycleMappingParser;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.loader.PluginLoader;
import org.apache.maven.plugin.loader.PluginLoaderException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.collections.ActiveMap;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;

import java.util.Iterator;
import java.util.List;

//FIXME: This needs a better name!
public class DefaultLifecycleBindingManager
    implements LifecycleBindingManager, LogEnabled
{

    private ActiveMap bindingsByPackaging;

    private PluginLoader pluginLoader;

    private Logger logger;

    // configured. Moved out of DefaultLifecycleExecutor...
    private List legacyLifecycles;
    
    public LifecycleBindings getBindingsForPackaging( MavenProject project )
        throws LifecycleLoaderException, LifecycleSpecificationException
    {
        String packaging = project.getPackaging();

        LifecycleBindings bindings = null;

        LifecycleBindingLoader loader = (LifecycleBindingLoader) bindingsByPackaging.get( packaging );
        if ( loader != null )
        {
            bindings = loader.getBindings();
        }
        else
        {
            bindings = searchPluginsWithExtensions( project );
        }

        if ( bindings == null )
        {
            bindings = getDefaultBindings();
        }

        return bindings;
    }

    private LifecycleBindings searchPluginsWithExtensions( MavenProject project )
        throws LifecycleLoaderException, LifecycleSpecificationException
    {
        List plugins = project.getBuildPlugins();
        String packaging = project.getPackaging();

        LifecycleBindings bindings = null;

        for ( Iterator it = plugins.iterator(); it.hasNext(); )
        {
            Plugin plugin = (Plugin) it.next();
            if ( plugin.isExtensions() )
            {
                LifecycleBindingLoader loader = null;

                try
                {
                    loader = (LifecycleBindingLoader) pluginLoader.loadPluginComponent( LifecycleBindingLoader.ROLE, packaging,
                                                                                        plugin, project );
                }
                catch ( ComponentLookupException e )
                {
                    logger.debug( LifecycleBindingLoader.ROLE + " for packaging: " + packaging
                        + " could not be retrieved from plugin: " + plugin.getKey() + ".\nReason: " + e.getMessage(), e );
                }
                catch ( PluginLoaderException e )
                {
                    throw new LifecycleLoaderException( "Failed to load plugin: " + plugin.getKey() + ". Reason: "
                        + e.getMessage(), e );
                }
                
                if ( loader != null )
                {
                    bindings = loader.getBindings();
                    
                    if ( bindings != null )
                    {
                        break;
                    }
                }
            }
        }

        return bindings;
    }

    public LifecycleBindings getDefaultBindings()
        throws LifecycleSpecificationException
    {
        return LegacyLifecycleMappingParser.parseDefaultMappings( legacyLifecycles );
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

    public LifecycleBindings getProjectCustomBindings( MavenProject project )
        throws LifecycleLoaderException, LifecycleSpecificationException
    {
        String projectId = project.getId();
        
        LifecycleBindings bindings = new LifecycleBindings();
        bindings.setOrigin( projectId );
        bindings.setPackaging( project.getPackaging() );
        
        List plugins = project.getBuildPlugins();
        if ( plugins != null )
        {
            for ( Iterator it = plugins.iterator(); it.hasNext(); )
            {
                Plugin plugin = (Plugin) it.next();
                PluginDescriptor pluginDescriptor = null;
                
                List executions = plugin.getExecutions();
                if ( executions != null )
                {
                    for ( Iterator execIt = executions.iterator(); execIt.hasNext(); )
                    {
                        PluginExecution execution = (PluginExecution) execIt.next();
                        
                        List goals = execution.getGoals();
                        for ( Iterator goalIterator = goals.iterator(); goalIterator.hasNext(); )
                        {
                            String goal = (String) goalIterator.next();
                            
                            MojoBinding mojoBinding = new MojoBinding();
                            
                            mojoBinding.setGroupId( plugin.getGroupId() );
                            mojoBinding.setArtifactId( plugin.getArtifactId() );
                            mojoBinding.setVersion( plugin.getVersion() );
                            mojoBinding.setGoal( goal );
                            mojoBinding.setDefaultConfiguration( execution.getConfiguration() );
                            
                            String phase = execution.getPhase();
                            if ( phase == null )
                            {
                                if ( pluginDescriptor == null )
                                {
                                    try
                                    {
                                        pluginDescriptor = pluginLoader.loadPlugin( plugin, project );
                                    }
                                    catch ( PluginLoaderException e )
                                    {
                                        throw new LifecycleLoaderException( "Failed to load plugin: " + plugin + ". Reason: " + e.getMessage(), e );
                                    }
                                }
                                
                                MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( goal );
                                phase = mojoDescriptor.getPhase();
                                
                                if ( phase == null )
                                {
                                    throw new LifecycleSpecificationException( "No phase specified for goal: " + goal + " in plugin: " + plugin.getKey() + " from POM: " + projectId );
                                }
                            }
                            
                            LifecycleWalker.addMojoBinding( phase, mojoBinding, bindings );
                        }
                    }
                }
            }
        }
        
        return bindings;
    }

}
