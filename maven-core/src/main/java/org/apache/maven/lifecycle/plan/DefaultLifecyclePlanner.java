package org.apache.maven.lifecycle.plan;

import org.apache.maven.BuildFailureException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.lifecycle.LifecycleException;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.plugin.InvalidPluginException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.manager.PluginLoader;
import org.apache.maven.plugin.version.PluginVersionNotFoundException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * @plexus.component
 *   role="org.apache.maven.lifecycle.plan.LifecyclePlanner" role-hint="default"
 *   
 * @author jdcasey
 *
 */
public class DefaultLifecyclePlanner
    implements LifecyclePlanner, LogEnabled
{

    private List lifecycles;

    private Map phaseToLifecycleMap;

    private Logger logger;

    private PluginManager pluginManager;
    
    private PluginLoader pluginLoader;

    public List constructExecutionPlan()
    {
        return null;
    }

    public Map getPhaseToLifecycleMap()
        throws LifecycleExecutionException
    {
        if ( phaseToLifecycleMap == null )
        {
            phaseToLifecycleMap = new HashMap();

            for ( Iterator i = lifecycles.iterator(); i.hasNext(); )
            {
                Lifecycle lifecycle = (Lifecycle) i.next();

                for ( Iterator p = lifecycle.getPhases().iterator(); p.hasNext(); )
                {
                    String phase = (String) p.next();

                    if ( phaseToLifecycleMap.containsKey( phase ) )
                    {
                        Lifecycle prevLifecycle = (Lifecycle) phaseToLifecycleMap.get( phase );
                        throw new LifecycleException( "Phase '" + phase + "' is defined in more than one lifecycle: '"
                            + lifecycle.getId() + "' and '" + prevLifecycle.getId() + "'" );
                    }
                    else
                    {
                        phaseToLifecycleMap.put( phase, lifecycle );
                    }
                }
            }
        }
        return phaseToLifecycleMap;
    }

    public Lifecycle getLifecycleForPhase( String phase )
        throws LifecycleExecutionException
    {
        return (Lifecycle) getPhaseToLifecycleMap().get( phase );
    }

    public boolean isLifecyclePhase( String task )
        throws LifecycleExecutionException
    {
        return getPhaseToLifecycleMap().containsKey( task );
    }

    private Map constructLifecycleMappings( MavenSession session, String selectedPhase, MavenProject project, Lifecycle lifecycle )
        throws LifecycleExecutionException, BuildFailureException, PluginNotFoundException
    {
        // first, bind those associated with the packaging
        Map lifecycleMappings = bindLifecycleForPackaging( session, selectedPhase, project, lifecycle );

        // next, loop over plugins and for any that have a phase, bind it
        for ( Iterator i = project.getBuildPlugins().iterator(); i.hasNext(); )
        {
            Plugin plugin = (Plugin) i.next();

            bindPluginToLifecycle( plugin, session, lifecycleMappings, project );
        }

        return lifecycleMappings;
    }

    private Map bindLifecycleForPackaging( MavenSession session, String selectedPhase, MavenProject project, Lifecycle lifecycle )
        throws LifecycleExecutionException, BuildFailureException, PluginNotFoundException
    {
        Map mappings = findMappingsForLifecycle( session, project, lifecycle );

        List optionalMojos = findOptionalMojosForLifecycle( session, project, lifecycle );

        Map lifecycleMappings = new HashMap();

        for ( Iterator i = lifecycle.getPhases().iterator(); i.hasNext(); )
        {
            String phase = (String) i.next();

            String phaseTasks = (String) mappings.get( phase );

            if ( phaseTasks != null )
            {
                for ( StringTokenizer tok = new StringTokenizer( phaseTasks, "," ); tok.hasMoreTokens(); )
                {
                    String goal = tok.nextToken().trim();

                    // Not from the CLI, don't use prefix
                    MojoDescriptor mojoDescriptor = getMojoDescriptor( goal, session, project, selectedPhase, false,
                                                                       optionalMojos.contains( goal ) );

                    if ( mojoDescriptor == null )
                    {
                        continue;
                    }

                    if ( mojoDescriptor.isDirectInvocationOnly() )
                    {
                        throw new LifecycleExecutionException( "Mojo: \'" + goal
                            + "\' requires direct invocation. It cannot be used as part of lifecycle: \'"
                            + project.getPackaging() + "\'." );
                    }

                    addToLifecycleMappings( lifecycleMappings, phase, new MojoExecution( mojoDescriptor ), session.getSettings() );
                }
            }

            if ( phase.equals( selectedPhase ) )
            {
                break;
            }
        }

        return lifecycleMappings;
    }

    private Map findMappingsForLifecycle( MavenSession session, MavenProject project, Lifecycle lifecycle )
        throws LifecycleExecutionException, PluginNotFoundException
    {
        String packaging = project.getPackaging();
        Map mappings = null;

        LifecycleMapping m = (LifecycleMapping) findExtension( project, LifecycleMapping.ROLE, packaging, session );
        if ( m != null )
        {
            mappings = m.getPhases( lifecycle.getId() );
        }

        Map defaultMappings = lifecycle.getDefaultPhases();

        if ( mappings == null )
        {
            try
            {
                m = (LifecycleMapping) session.lookup( LifecycleMapping.ROLE, packaging );
                mappings = m.getPhases( lifecycle.getId() );
            }
            catch ( ComponentLookupException e )
            {
                if ( defaultMappings == null )
                {
                    throw new LifecycleExecutionException( "Cannot find lifecycle mapping for packaging: \'" + packaging + "\'.",
                                                           e );
                }
            }
        }

        if ( mappings == null )
        {
            if ( defaultMappings == null )
            {
                throw new LifecycleExecutionException( "Cannot find lifecycle mapping for packaging: \'" + packaging
                    + "\', and there is no default" );
            }
            else
            {
                mappings = defaultMappings;
            }
        }

        return mappings;
    }

    private List findOptionalMojosForLifecycle( MavenSession session, MavenProject project, Lifecycle lifecycle )
        throws LifecycleExecutionException, PluginNotFoundException
    {
        String packaging = project.getPackaging();
        List optionalMojos = null;

        LifecycleMapping m = (LifecycleMapping) findExtension( project, LifecycleMapping.ROLE, packaging, session );

        if ( m != null )
        {
            optionalMojos = m.getOptionalMojos( lifecycle.getId() );
        }

        if ( optionalMojos == null )
        {
            try
            {
                m = (LifecycleMapping) session.lookup( LifecycleMapping.ROLE, packaging );
                optionalMojos = m.getOptionalMojos( lifecycle.getId() );
            }
            catch ( ComponentLookupException e )
            {
                getLogger().debug(
                                   "Error looking up lifecycle mapping to retrieve optional mojos. Lifecycle ID: "
                                       + lifecycle.getId() + ". Error: " + e.getMessage(), e );
            }
        }

        if ( optionalMojos == null )
        {
            optionalMojos = Collections.EMPTY_LIST;
        }

        return optionalMojos;
    }

    /**
     * Take each mojo contained with a plugin, look to see whether it contributes to a
     * phase in the lifecycle and if it does place it at the end of the list of goals
     * to execute for that given phase.
     *
     * @param project
     * @param session
     */
    private void bindPluginToLifecycle( Plugin plugin, MavenSession session, Map phaseMap, MavenProject project )
        throws LifecycleException
    {
        Settings settings = session.getSettings();

        PluginDescriptor pluginDescriptor = pluginLoader.loadPlugin( plugin, project, session );

        if ( pluginDescriptor.getMojos() != null && !pluginDescriptor.getMojos().isEmpty() )
        {
            // use the plugin if inherit was true in a base class, or it is in the current POM, otherwise use the default inheritence setting
            if ( plugin.isInheritanceApplied() || pluginDescriptor.isInheritedByDefault() )
            {
                if ( plugin.getGoals() != null )
                {
                    getLogger().error( "Plugin contains a <goals/> section: this is IGNORED - please use <executions/> instead." );
                }

                List executions = plugin.getExecutions();

                if ( executions != null )
                {
                    for ( Iterator it = executions.iterator(); it.hasNext(); )
                    {
                        PluginExecution execution = (PluginExecution) it.next();

                        bindExecutionToLifecycle( pluginDescriptor, phaseMap, execution, settings );
                    }
                }
            }
        }
    }

    private void bindExecutionToLifecycle( PluginDescriptor pluginDescriptor, Map phaseMap, PluginExecution execution,
                                           Settings settings )
        throws LifecycleException
    {
        for ( Iterator i = execution.getGoals().iterator(); i.hasNext(); )
        {
            String goal = (String) i.next();

            MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( goal );
            if ( mojoDescriptor == null )
            {
                throw new LifecycleException( "Goal '" + goal
                    + "' was specified in an execution, but not found in plugin " + pluginDescriptor.getId() );
            }

            // We have to check to see that the inheritance rules have been applied before binding this mojo.
            if ( execution.isInheritanceApplied() || mojoDescriptor.isInheritedByDefault() )
            {
                MojoExecution mojoExecution = new MojoExecution( mojoDescriptor, execution.getId() );

                String phase = execution.getPhase();

                if ( phase == null )
                {
                    // if the phase was not in the configuration, use the phase in the descriptor
                    phase = mojoDescriptor.getPhase();
                }

                if ( phase != null )
                {
                    if ( mojoDescriptor.isDirectInvocationOnly() )
                    {
                        throw new LifecycleException(
                                                               "Mojo: \'"
                                                                   + goal
                                                                   + "\' requires direct invocation. It cannot be used as part of the lifecycle (it was included via the POM)." );
                    }

                    addToLifecycleMappings( phaseMap, phase, mojoExecution, settings );
                }
            }
        }
    }

    private void addToLifecycleMappings( Map lifecycleMappings, String phase, MojoExecution mojoExecution, Settings settings )
    {
        List goals = (List) lifecycleMappings.get( phase );

        if ( goals == null )
        {
            goals = new ArrayList();
            lifecycleMappings.put( phase, goals );
        }

        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();
        if ( settings.isOffline() && mojoDescriptor.isOnlineRequired() )
        {
            String goal = mojoDescriptor.getGoal();
            getLogger().warn( goal + " requires online mode, but maven is currently offline. Disabling " + goal + "." );
        }
        else
        {
            goals.add( mojoExecution );
        }
    }

    protected Logger getLogger()
    {
        if ( logger == null )
        {
            logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "DefaultLifecyclePlanner::internal" );
        }

        return logger;
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

    private MojoDescriptor getMojoDescriptor( String task, MavenSession session, MavenProject project,
                                              String invokedVia, boolean canUsePrefix, boolean isOptionalMojo )
        throws BuildFailureException, LifecycleExecutionException, PluginNotFoundException
    {
        String goal;
        Plugin plugin;

        PluginDescriptor pluginDescriptor = null;

        try
        {
            StringTokenizer tok = new StringTokenizer( task, ":" );
            int numTokens = tok.countTokens();

            if ( numTokens == 2 )
            {
                if ( !canUsePrefix )
                {
                    String msg = "Mapped-prefix lookup of mojos are only supported from direct invocation. " +
                        "Please use specification of the form groupId:artifactId[:version]:goal instead. " +
                        "(Offending mojo: \'" + task + "\', invoked via: \'" + invokedVia + "\')";
                    throw new LifecycleExecutionException( msg );
                }

                String prefix = tok.nextToken();
                goal = tok.nextToken();

                // Steps for retrieving the plugin model instance:
                // 1. request directly from the plugin collector by prefix
                pluginDescriptor = pluginManager.getPluginDescriptorForPrefix( prefix );

                // 2. look in the repository via search groups
                if ( pluginDescriptor == null )
                {
                    plugin = pluginManager.getPluginDefinitionForPrefix( prefix, session, project );
                }
                else
                {
                    plugin = new Plugin();

                    plugin.setGroupId( pluginDescriptor.getGroupId() );
                    plugin.setArtifactId( pluginDescriptor.getArtifactId() );
                    plugin.setVersion( pluginDescriptor.getVersion() );
                }

                // 3. search plugins in the current POM
                if ( plugin == null )
                {
                    for ( Iterator i = project.getBuildPlugins().iterator(); i.hasNext(); )
                    {
                        Plugin buildPlugin = (Plugin) i.next();

                        PluginDescriptor desc = verifyPlugin( buildPlugin, project, session );

                        if ( prefix.equals( desc.getGoalPrefix() ) )
                        {
                            plugin = buildPlugin;
                        }
                    }
                }

                // 4. default to o.a.m.plugins and maven-<prefix>-plugin
                if ( plugin == null )
                {
                    plugin = new Plugin();
                    plugin.setGroupId( PluginDescriptor.getDefaultPluginGroupId() );
                    plugin.setArtifactId( PluginDescriptor.getDefaultPluginArtifactId( prefix ) );
                }

                for ( Iterator i = project.getBuildPlugins().iterator(); i.hasNext(); )
                {
                    Plugin buildPlugin = (Plugin) i.next();

                    if ( buildPlugin.getKey().equals( plugin.getKey() ) )
                    {
                        plugin = buildPlugin;
                        break;
                    }
                }
            }
            else if ( numTokens == 3 || numTokens == 4 )
            {
                plugin = new Plugin();

                plugin.setGroupId( tok.nextToken() );
                plugin.setArtifactId( tok.nextToken() );

                if ( numTokens == 4 )
                {
                    plugin.setVersion( tok.nextToken() );
                }

                goal = tok.nextToken();
            }
            else
            {
                String message = "Invalid task '" + task + "': you must specify a valid lifecycle phase, or" +
                    " a goal in the format plugin:goal or pluginGroupId:pluginArtifactId:pluginVersion:goal";
                throw new BuildFailureException( message );
            }

            project.injectPluginManagementInfo( plugin );

            if ( pluginDescriptor == null )
            {
                pluginDescriptor = pluginManager.verifyPlugin( plugin, project, session );
            }

            // this has been simplified from the old code that injected the plugin management stuff, since
            // pluginManagement injection is now handled by the project method.
            project.addPlugin( plugin );

            MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( goal );
            if ( mojoDescriptor == null )
            {
                if ( isOptionalMojo )
                {
                    getLogger().info( "Skipping missing optional mojo: " + task );
                }
                else
                {
                    throw new BuildFailureException( "Required goal not found: " + task );
                }
            }

            return mojoDescriptor;
        }
        catch ( PluginNotFoundException e )
        {
            if ( isOptionalMojo )
            {
                getLogger().info( "Skipping missing optional mojo: " + task );
                getLogger().debug( "Mojo: " + task + " could not be found. Reason: " + e.getMessage(), e );
            }
            else
            {
                throw e;
            }
        }

        return null;
    }

    private List processGoalChain( String task, Map phaseMap, Lifecycle lifecycle )
    {
        List goals = new ArrayList();

        // only execute up to the given phase
        int index = lifecycle.getPhases().indexOf( task );

        for ( int i = 0; i <= index; i++ )
        {
            String p = (String) lifecycle.getPhases().get( i );

            List phaseGoals = (List) phaseMap.get( p );

            if ( phaseGoals != null )
            {
                goals.addAll( phaseGoals );
            }
        }
        return goals;
    }

}
