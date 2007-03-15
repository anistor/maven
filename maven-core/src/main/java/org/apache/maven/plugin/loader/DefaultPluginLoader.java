package org.apache.maven.plugin.loader;

import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.context.BuildContextManager;
import org.apache.maven.execution.SessionContext;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.plugin.InvalidPluginException;
import org.apache.maven.plugin.MavenPluginCollector;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginMappingManager;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.version.PluginVersionNotFoundException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;

import java.util.Iterator;

public class DefaultPluginLoader
    implements PluginLoader, LogEnabled
{

    private Logger logger;

    // FIXME: Move the functionality used from this into the PluginLoader when PluginManager refactor is complete.
    private PluginManager pluginManager;

    private PluginMappingManager pluginMappingManager;

    private BuildContextManager buildContextManager;

    private MavenPluginCollector pluginCollector;

    public Object loadPluginComponent( String role, String roleHint, Plugin plugin, MavenProject project )
        throws ComponentLookupException, PluginLoaderException
    {
        loadPlugin( plugin, project );

        try
        {
            return pluginManager.getPluginComponent( plugin, role, roleHint );
        }
        catch ( PluginManagerException e )
        {
            Throwable cause = e.getCause();

            if ( cause != null && ( cause instanceof ComponentLookupException ) )
            {
                StringBuffer message = new StringBuffer();
                message.append( "ComponentLookupException in PluginManager while looking up a component in the realm of: " );
                message.append( plugin.getKey() );
                message.append( ".\nReason: " );
                message.append( cause.getMessage() );
                message.append( "\n\nStack-Trace inside of PluginManager was:\n\n" );

                StackTraceElement[] elements = e.getStackTrace();
                for ( int i = 0; i < elements.length; i++ )
                {
                    if ( elements[i].getClassName().indexOf( "PluginManager" ) < 0 )
                    {
                        break;
                    }

                    message.append( elements[i] );
                }

                logger.debug( message.toString() + "\n" );

                throw (ComponentLookupException) cause;
            }
            else
            {
                throw new PluginLoaderException( plugin, "Failed to lookup plugin component. Reason: " + e.getMessage(), e );
            }
        }
    }

    public PluginDescriptor loadPlugin( MojoBinding mojoBinding, MavenProject project )
        throws PluginLoaderException
    {
        PluginDescriptor pluginDescriptor = null;
        
        Plugin plugin = new Plugin();
        plugin.setGroupId( mojoBinding.getGroupId() );
        plugin.setArtifactId( mojoBinding.getArtifactId() );
        plugin.setVersion( mojoBinding.getVersion() );

        pluginDescriptor = loadPlugin( plugin, project );

        // fill in any blanks once we know more about this plugin.
        if ( pluginDescriptor != null )
        {
            mojoBinding.setGroupId( pluginDescriptor.getGroupId() );
            mojoBinding.setArtifactId( pluginDescriptor.getArtifactId() );
            mojoBinding.setVersion( pluginDescriptor.getVersion() );
        }

        return pluginDescriptor;
    }
    
    public PluginDescriptor findPluginForPrefix( String prefix, MavenProject project )
        throws PluginLoaderException
    {
        PluginDescriptor pluginDescriptor = loadByPrefix( prefix, project );

        if ( pluginDescriptor == null )
        {
            pluginDescriptor = loadFromProject( prefix, project );
        }
            
        if ( pluginDescriptor == null )
        {
            Plugin plugin = new Plugin();
            plugin.setArtifactId( PluginDescriptor.getDefaultPluginArtifactId( prefix ) );
            
            pluginDescriptor = loadPlugin( plugin, project );
        }
        
        if ( pluginDescriptor == null )
        {
            throw new PluginLoaderException( "Cannot find plugin with prefix: " + prefix );
        }
        
        return pluginDescriptor;
    }

    private PluginDescriptor loadFromProject( String prefix, MavenProject project )
        throws PluginLoaderException
    {
        PluginDescriptor result = null;
        
        for ( Iterator it = project.getBuildPlugins().iterator(); it.hasNext(); )
        {
            Plugin plugin = (Plugin) it.next();
            
            PluginDescriptor pluginDescriptor = loadPlugin( plugin, project );
            if ( prefix.equals( pluginDescriptor.getGoalPrefix() ) )
            {
                result = pluginDescriptor;
                break;
            }
        }
        
        return result;
    }

    private PluginDescriptor loadByPrefix( String prefix, MavenProject project )
        throws PluginLoaderException
    {
        PluginDescriptor pluginDescriptor = pluginCollector.getPluginDescriptorForPrefix( prefix );
        
        SessionContext ctx = SessionContext.read( buildContextManager );
        Settings settings = ctx.getSettings();
        
        if ( pluginDescriptor == null )
        {
            Plugin plugin = pluginMappingManager.getByPrefix( prefix, settings.getPluginGroups(),
                                                       project.getPluginArtifactRepositories(), ctx.getLocalRepository() );
            
            if ( plugin != null )
            {
                pluginDescriptor = loadPlugin( plugin, project );
            }
        }
        
        return pluginDescriptor;
    }

    public PluginDescriptor loadPlugin( Plugin plugin, MavenProject project )
        throws PluginLoaderException
    {
        if ( plugin.getGroupId() == null )
        {
            plugin.setGroupId( PluginDescriptor.getDefaultPluginGroupId() );
        }
        
        SessionContext ctx = SessionContext.read( buildContextManager );

        try
        {
            PluginDescriptor result = pluginManager.verifyPlugin( plugin, project, ctx.getSession() );
            
            // this has been simplified from the old code that injected the plugin management stuff, since
            // pluginManagement injection is now handled by the project method.
            project.addPlugin( plugin );
            
            return result;
        }
        catch ( ArtifactResolutionException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( PluginNotFoundException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( PluginVersionResolutionException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( InvalidPluginException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( PluginManagerException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( PluginVersionNotFoundException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

    public PluginDescriptor loadReportPlugin( MojoBinding mojoBinding, MavenProject project )
        throws PluginLoaderException
    {
        ReportPlugin plugin = new ReportPlugin();
        plugin.setGroupId( mojoBinding.getGroupId() );
        plugin.setArtifactId( mojoBinding.getArtifactId() );
        plugin.setVersion( mojoBinding.getVersion() );

        PluginDescriptor pluginDescriptor = loadReportPlugin( plugin, project );

        mojoBinding.setVersion( pluginDescriptor.getVersion() );

        return pluginDescriptor;
    }

    public PluginDescriptor loadReportPlugin( ReportPlugin plugin, MavenProject project )
        throws PluginLoaderException
    {
        // TODO: Shouldn't we be injecting pluginManagement info here??
        
        SessionContext ctx = SessionContext.read( buildContextManager );

        try
        {
            return pluginManager.verifyReportPlugin( plugin, project, ctx.getSession() );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( PluginNotFoundException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( PluginVersionResolutionException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( InvalidPluginException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( PluginManagerException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( PluginVersionNotFoundException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
    }

}
