package org.apache.maven.lifecycle.binding;

import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.loader.PluginLoader;
import org.apache.maven.plugin.loader.PluginLoaderException;
import org.apache.maven.project.MavenProject;

import java.util.StringTokenizer;

public class DefaultMojoBindingFactory
    implements MojoBindingFactory
{

    PluginLoader pluginLoader;

    public MojoBinding parseMojoBinding( String bindingSpec, MavenProject project, boolean allowPrefixReference )
        throws LifecycleSpecificationException, LifecycleLoaderException
    {
        StringTokenizer tok = new StringTokenizer( bindingSpec, ":" );
        int numTokens = tok.countTokens();

        MojoBinding binding = null;

        if ( numTokens == 2 )
        {
            if ( !allowPrefixReference )
            {
                String msg = "Mapped-prefix lookup of mojos are only supported from direct invocation. "
                    + "Please use specification of the form groupId:artifactId[:version]:goal instead.";

                throw new LifecycleSpecificationException( msg );
            }

            String prefix = tok.nextToken();

            PluginDescriptor pluginDescriptor;
            try
            {
                pluginDescriptor = pluginLoader.findPluginForPrefix( prefix, project );
            }
            catch ( PluginLoaderException e )
            {
                throw new LifecycleLoaderException(
                                                    "Failed to find plugin for prefix: " + prefix + ". Reason: " + e.getMessage(),
                                                    e );
            }

            binding = createMojoBinding( pluginDescriptor.getGroupId(), pluginDescriptor.getArtifactId(),
                                         pluginDescriptor.getVersion(), tok.nextToken(), project );
        }
        else if ( numTokens == 3 || numTokens == 4 )
        {
            binding = new MojoBinding();

            String groupId = tok.nextToken();
            String artifactId = tok.nextToken();

            String version = null;
            if ( numTokens == 4 )
            {
                version = tok.nextToken();
            }

            String goal = tok.nextToken();

            binding = createMojoBinding( groupId, artifactId, version, goal, project );
        }
        else
        {
            String message = "Invalid task '" + bindingSpec + "': you must specify a valid lifecycle phase, or"
                + " a goal in the format plugin:goal or pluginGroupId:pluginArtifactId:pluginVersion:goal";

            throw new LifecycleSpecificationException( message );
        }

        return binding;
    }

    public MojoBinding createMojoBinding( String groupId, String artifactId, String version, String goal, MavenProject project )
    {
        MojoBinding binding = new MojoBinding();

        binding.setGroupId( groupId );
        binding.setArtifactId( artifactId );
        binding.setVersion( version );
        binding.setGoal( goal );

        BindingUtils.injectProjectConfiguration( binding, project );

        return binding;
    }

    public MojoBinding parseMojoBinding( String bindingSpec )
        throws LifecycleSpecificationException
    {
        try
        {
            return parseMojoBinding( bindingSpec, null, false );
        }
        catch ( LifecycleLoaderException e )
        {
            IllegalStateException error = new IllegalStateException( e.getMessage()
                + "\n\nTHIS SHOULD BE IMPOSSIBLE DUE TO THE USAGE OF THE PLUGIN-LOADER." );
            
            error.initCause( e );
            
            throw error;
        }
    }

}
