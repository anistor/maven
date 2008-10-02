package org.apache.maven.plugin.loader;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.plugin.InvalidPluginException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.version.PluginVersionNotFoundException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;

/**
 * Responsible for loading plugins, reports, and any components contained therein. Will resolve
 * plugin versions and plugin prefixes as necessary for plugin resolution.
 *
 * @author jdcasey
 *
 */
public class DefaultPluginLoader
    implements PluginLoader, LogEnabled
{

    private Logger logger;

    // FIXME: Move the functionality used from this into the PluginLoader when PluginManager refactor is complete.
    private PluginManager pluginManager;

    /**
     * Load the {@link PluginDescriptor} instance for the specified plugin, using the project for
     * the {@link ArtifactRepository} and other supplemental plugin information as necessary.
     */
    public PluginDescriptor loadPlugin( Plugin plugin, MavenProject project, MavenSession session )
        throws PluginLoaderException
    {
        if ( plugin.getGroupId() == null )
        {
            plugin.setGroupId( PluginDescriptor.getDefaultPluginGroupId() );
        }

        try
        {
            PluginDescriptor result = pluginManager.verifyPlugin( plugin, project, session );

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

    /**
     * Load the {@link PluginDescriptor} instance for the specified report plugin, using the project for
     * the {@link ArtifactRepository} and other supplemental report/plugin information as necessary.
     */
    public PluginDescriptor loadReportPlugin( ReportPlugin plugin, MavenProject project, MavenSession session )
        throws PluginLoaderException
    {
        // TODO: Shouldn't we be injecting pluginManagement info here??

        try
        {
            return pluginManager.verifyReportPlugin( plugin, project, session );
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
