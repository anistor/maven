package org.apache.maven.lifecycle.binding;

import org.apache.maven.lifecycle.LegacyLifecycleMappingParser;
import org.apache.maven.lifecycle.LifecycleBindingLoader;
import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.LifecycleUtils;
import org.apache.maven.lifecycle.MojoBindingParser;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.lifecycle.Execution;
import org.apache.maven.plugin.lifecycle.Lifecycle;
import org.apache.maven.plugin.loader.PluginLoader;
import org.apache.maven.plugin.loader.PluginLoaderException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.collections.ActiveMap;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
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
                            mojoBinding.setConfiguration( execution.getConfiguration() );
                            mojoBinding.setExecutionId( execution.getId() );

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
                                        throw new LifecycleLoaderException( "Failed to load plugin: " + plugin + ". Reason: "
                                            + e.getMessage(), e );
                                    }
                                }

                                MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( goal );
                                phase = mojoDescriptor.getPhase();

                                if ( phase == null )
                                {
                                    throw new LifecycleSpecificationException( "No phase specified for goal: " + goal
                                        + " in plugin: " + plugin.getKey() + " from POM: " + projectId );
                                }
                            }

                            LifecycleUtils.addMojoBinding( phase, mojoBinding, bindings );
                        }
                    }
                }
            }
        }

        LifecycleUtils.setOrigin( bindings, projectId );

        return bindings;
    }

    public LifecycleBindings getPluginLifecycleOverlay( PluginDescriptor pluginDescriptor, String lifecycleId )
        throws LifecycleLoaderException, LifecycleSpecificationException
    {
        Lifecycle lifecycleOverlay = null;

        try
        {
            lifecycleOverlay = pluginDescriptor.getLifecycleMapping( lifecycleId );
        }
        catch ( IOException e )
        {
            throw new LifecycleLoaderException( "Unable to read lifecycle mapping file: " + e.getMessage(), e );
        }
        catch ( XmlPullParserException e )
        {
            throw new LifecycleLoaderException( "Unable to parse lifecycle mapping file: " + e.getMessage(), e );
        }

        if ( lifecycleOverlay == null )
        {
            throw new LifecycleLoaderException( "Lifecycle '" + lifecycleId + "' not found in plugin" );
        }

        LifecycleBindings bindings = new LifecycleBindings();

        for ( Iterator i = lifecycleOverlay.getPhases().iterator(); i.hasNext(); )
        {
            org.apache.maven.plugin.lifecycle.Phase phase = (org.apache.maven.plugin.lifecycle.Phase) i.next();
            List phaseBindings = new ArrayList();

            for ( Iterator j = phase.getExecutions().iterator(); j.hasNext(); )
            {
                Execution exec = (Execution) j.next();

                for ( Iterator k = exec.getGoals().iterator(); k.hasNext(); )
                {
                    String goal = (String) k.next();

                    // Here we are looking to see if we have a mojo from an external plugin.
                    // If we do then we need to lookup the plugin descriptor for the externally
                    // referenced plugin so that we can overly the execution into the lifecycle.
                    // An example of this is the corbertura plugin that needs to call the surefire
                    // plugin in forking mode.
                    //
                    //<phase>
                    //  <id>test</id>
                    //  <executions>
                    //    <execution>
                    //      <goals>
                    //        <goal>org.apache.maven.plugins:maven-surefire-plugin:test</goal>
                    //      </goals>
                    //      <configuration>
                    //        <classesDirectory>${project.build.directory}/generated-classes/cobertura</classesDirectory>
                    //        <ignoreFailures>true</ignoreFailures>
                    //        <forkMode>once</forkMode>
                    //      </configuration>
                    //    </execution>
                    //  </executions>
                    //</phase>

                    // ----------------------------------------------------------------------
                    //
                    // ----------------------------------------------------------------------

                    MojoBinding binding;
                    if ( goal.indexOf( ":" ) > 0 )
                    {
                        binding = MojoBindingParser.parseMojoBinding( goal, false );
                    }
                    else
                    {
                        binding = new MojoBinding();
                        binding.setGroupId( pluginDescriptor.getGroupId() );
                        binding.setArtifactId( pluginDescriptor.getArtifactId() );
                        binding.setVersion( pluginDescriptor.getVersion() );
                        binding.setGoal( goal );
                    }

                    Xpp3Dom configuration = (Xpp3Dom) exec.getConfiguration();
                    if ( phase.getConfiguration() != null )
                    {
                        configuration = Xpp3Dom.mergeXpp3Dom( new Xpp3Dom( (Xpp3Dom) phase.getConfiguration() ), configuration );
                    }

                    binding.setConfiguration( configuration );
                    binding.setOrigin( lifecycleId );

                    LifecycleUtils.addMojoBinding( phase.getId(), binding, bindings );
                    phaseBindings.add( binding );
                }
            }

            if ( phase.getConfiguration() != null )
            {
                // Merge in general configuration for a phase.
                // TODO: this is all kind of backwards from the POMM. Let's align it all under 2.1.
                //   We should create a new lifecycle executor for modelVersion >5.0.0

                // [jdcasey; 08-March-2007] Not sure what the above to-do references...how _should_
                // this work??
                for ( Iterator j = phaseBindings.iterator(); j.hasNext(); )
                {
                    MojoBinding binding = (MojoBinding) j.next();

                    Xpp3Dom configuration = Xpp3Dom.mergeXpp3Dom( new Xpp3Dom( (Xpp3Dom) phase.getConfiguration() ),
                                                                  (Xpp3Dom) binding.getConfiguration() );

                    binding.setConfiguration( configuration );
                }
            }

        }

        return bindings;
    }

}
