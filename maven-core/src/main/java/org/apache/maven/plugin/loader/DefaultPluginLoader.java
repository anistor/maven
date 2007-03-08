package org.apache.maven.plugin.loader;

import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.context.BuildContextManager;
import org.apache.maven.execution.SessionContext;
import org.apache.maven.lifecycle.MojoBinding;
import org.apache.maven.lifecycle.PrefixedMojoBinding;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.InvalidPluginException;
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

public class DefaultPluginLoader
    implements PluginLoader, LogEnabled
{

    private Logger logger;

    // FIXME: Move the functionality used from this into the PluginLoader when PluginManager refactor is complete.
    private PluginManager pluginManager;
    
    private PluginMappingManager pluginMappingManager;

    private BuildContextManager buildContextManager;

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
        Plugin plugin = null;
        if ( mojoBinding instanceof PrefixedMojoBinding )
        {
            PrefixedMojoBinding prefixed = (PrefixedMojoBinding) mojoBinding;
            
            SessionContext ctx = SessionContext.read( buildContextManager );
            Settings settings = ctx.getSettings();
            
            plugin = pluginMappingManager.getByPrefix( prefixed.getPrefix(), settings.getPluginGroups(),
                                                              project.getPluginArtifactRepositories(), ctx.getLocalRepository() );
            
            if ( plugin == null )
            {
                throw new PluginLoaderException( "Cannot find plugin with prefix: " + prefixed.getPrefix() );
            }
            
            mojoBinding.setGroupId( plugin.getGroupId() );
            mojoBinding.setArtifactId( plugin.getArtifactId() );
        }
        else
        {
            plugin = new Plugin();
            plugin.setGroupId( mojoBinding.getGroupId() );
            plugin.setArtifactId( mojoBinding.getArtifactId() );
            plugin.setVersion( mojoBinding.getVersion() );
        }
        
        PluginDescriptor pluginDescriptor = loadPlugin( plugin, project );
        
        mojoBinding.setVersion( pluginDescriptor.getVersion() );
        
        return pluginDescriptor;
    }

    public PluginDescriptor loadPlugin( Plugin plugin, MavenProject project )
        throws PluginLoaderException
    {
        SessionContext ctx = SessionContext.read( buildContextManager );

        try
        {
            return pluginManager.verifyPlugin( plugin, project, ctx.getSession() );
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

}
