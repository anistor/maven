package org.apache.maven.lifecycle.binding;

import org.apache.maven.lifecycle.model.LifecycleBinding;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.model.Phase;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.ReportSet;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ModelUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

final class BindingUtils
{

    static Map buildPluginMap( MavenProject project )
    {
        Map pluginMap = new HashMap();
        
        if ( project != null )
        {
            Build build = project.getBuild();
            if ( build != null )
            {
                for ( Iterator it = build.getPlugins().iterator(); it.hasNext(); )
                {
                    Plugin plugin = (Plugin) it.next();

                    pluginMap.put( createPluginKey( plugin ), plugin );
                }
            }
        }

        return pluginMap;
    }

    static Map buildPluginMap( PluginContainer pluginContainer )
    {
        Map pluginMap = new HashMap();
        
        if ( pluginContainer != null )
        {
            for ( Iterator it = pluginContainer.getPlugins().iterator(); it.hasNext(); )
            {
                Plugin plugin = (Plugin) it.next();

                pluginMap.put( createPluginKey( plugin ), plugin );
            }
        }

        return pluginMap;
    }

    static String createPluginKey( Plugin plugin )
    {
        return createPluginKey( plugin.getGroupId(), plugin.getArtifactId() );
    }

    static String createPluginKey( String groupId, String artifactId )
    {
        return ( groupId == null ? PluginDescriptor.getDefaultPluginGroupId() : groupId ) + ":" + artifactId;
    }

    static Object mergeConfigurations( ReportPlugin reportPlugin, ReportSet reportSet )
    {
        return mergeRawConfigurations( reportSet.getConfiguration(), reportPlugin.getConfiguration() );
    }

    static Object mergeConfigurations( Plugin plugin, PluginExecution execution )
    {
        if ( plugin == null && execution == null )
        {
            return null;
        }
        else if ( execution == null )
        {
            return plugin.getConfiguration();
        }
        else if ( plugin == null )
        {
            return execution.getConfiguration();
        }
        else
        {
            return mergeRawConfigurations( execution.getConfiguration(), plugin.getConfiguration() );
        }
    }

    static Object mergeRawConfigurations( Object dominant, Object recessive )
    {
        Xpp3Dom dominantConfig = (Xpp3Dom) dominant;
        Xpp3Dom recessiveConfig = (Xpp3Dom) recessive;

        if ( recessiveConfig == null )
        {
            return dominantConfig;
        }
        else if ( dominantConfig == null )
        {
            return recessiveConfig;
        }
        else
        {
            return Xpp3Dom.mergeXpp3Dom( new Xpp3Dom( dominantConfig ), recessiveConfig );
        }
    }
    
    static void injectProjectConfiguration( MojoBinding binding, MavenProject project )
    {
        Map pluginMap = buildPluginMap( project );
        Plugin plugin = (Plugin) pluginMap.get( createPluginKey( binding.getGroupId(), binding.getArtifactId() ) );

        if ( plugin == null )
        {
            plugin = new Plugin();
            plugin.setGroupId( binding.getGroupId() );
            plugin.setArtifactId( binding.getArtifactId() );
        }
        
        injectPluginManagementInfo( plugin, project );

        PluginExecution exec = (PluginExecution) plugin.getExecutionsAsMap().get( binding.getExecutionId() );

        binding.setConfiguration( mergeConfigurations( plugin, exec ) );
    }

    static void injectProjectConfiguration( LifecycleBindings bindings, MavenProject project )
    {
        Map pluginsByVersionlessKey = buildPluginMap( project );

        for ( Iterator lifecycleIt = bindings.getBindingList().iterator(); lifecycleIt.hasNext(); )
        {
            LifecycleBinding binding = (LifecycleBinding) lifecycleIt.next();

            for ( Iterator phaseIt = binding.getPhasesInOrder().iterator(); phaseIt.hasNext(); )
            {
                Phase phase = (Phase) phaseIt.next();

                for ( Iterator mojoIt = phase.getBindings().iterator(); mojoIt.hasNext(); )
                {
                    MojoBinding mojo = (MojoBinding) mojoIt.next();

                    String pluginKey = createPluginKey( mojo.getGroupId(), mojo.getArtifactId() );
                    Plugin plugin = (Plugin) pluginsByVersionlessKey.get( pluginKey );

                    if ( plugin == null )
                    {
                        plugin = new Plugin();
                        plugin.setGroupId( mojo.getGroupId() );
                        plugin.setArtifactId( mojo.getArtifactId() );
                    }
                    
                    injectPluginManagementInfo( plugin, project );

                    PluginExecution exec = (PluginExecution) plugin.getExecutionsAsMap().get( mojo.getExecutionId() );

                    mojo.setConfiguration( mergeConfigurations( plugin, exec ) );
                }
            }
        }
    }

    static void injectPluginManagementInfo( Plugin plugin, MavenProject project )
    {
        if ( project == null )
        {
            return;
        }
        
        Build build = project.getBuild();
        if ( build == null )
        {
            return;
        }
        
        PluginManagement plugMgmt = build.getPluginManagement();
        if ( plugMgmt == null )
        {
            return;
        }
        
        Map plugMgmtMap = buildPluginMap( plugMgmt );
        
        String key = createPluginKey( plugin );
        Plugin mgmtPlugin = (Plugin) plugMgmtMap.get( key );
        
        if ( mgmtPlugin != null )
        {
            ModelUtils.mergePluginDefinitions( plugin, mgmtPlugin, false );
        }
    }

}
