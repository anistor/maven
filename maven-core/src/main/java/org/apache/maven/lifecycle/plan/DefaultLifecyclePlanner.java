package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleBinding;
import org.apache.maven.lifecycle.LifecycleBindings;
import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.LifecycleUtils;
import org.apache.maven.lifecycle.MojoBinding;
import org.apache.maven.lifecycle.binding.LifecycleBindingManager;
import org.apache.maven.lifecycle.parser.MojoReferenceParser;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.loader.PluginLoader;
import org.apache.maven.plugin.loader.PluginLoaderException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DefaultLifecyclePlanner
    implements LifecyclePlanner, LogEnabled
{

    private Logger logger;

    private PluginLoader pluginLoader;

    private LifecycleBindingManager lifecycleBindingManager;

    public LifecyclePlan constructLifecyclePlan( List tasks, MavenProject project )
        throws LifecycleLoaderException, LifecycleSpecificationException, LifecyclePlannerException
    {
        LifecycleBindings defaultBindings = lifecycleBindingManager.getDefaultBindings();
        LifecycleBindings packagingBindings = lifecycleBindingManager.getBindingsForPackaging( project );
        LifecycleBindings projectBindings = lifecycleBindingManager.getProjectCustomBindings( project );

        LifecycleBindings merged = LifecycleUtils.mergeBindings( packagingBindings, projectBindings, defaultBindings );

        // foreach task, find the binding list from the merged lifecycle-bindings.
        // if the binding list is a super-set of a previous task, forget the previous task/binding
        //     list, and use the new one.
        // if the binding list is null, treat it like a one-off mojo invocation, and parse/validate
        //     that it can be called as such.
        // as binding lists accumulate, push them onto an aggregated "plan" listing...
        List planBindings = assemblePlanBindings( tasks, merged, project );

        LifecyclePlan plan = new DefaultLifecyclePlan( planBindings );

        // Inject forked lifecycles as plan modifiers for each mojo that has @execute in it.
        addForkedLifecycleModifiers( planBindings, plan, merged, project );
        
        // TODO: Inject relative-ordered project/plugin executions as plan modifiers.

        return plan;
    }

    private void addForkedLifecycleModifiers( List planBindings, LifecyclePlan plan, LifecycleBindings mergedBindings, MavenProject project ) throws LifecyclePlannerException, LifecycleSpecificationException
    {
        for ( Iterator it = planBindings.iterator(); it.hasNext(); )
        {
            MojoBinding mojoBinding = (MojoBinding) it.next();
            
            PluginDescriptor pluginDescriptor;
            try
            {
                pluginDescriptor = pluginLoader.loadPlugin( mojoBinding, project );
            }
            catch ( PluginLoaderException e )
            {
                throw new LifecyclePlannerException( e.getMessage(), e );
            }
            
            MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( mojoBinding.getGoal() );
            if ( mojoDescriptor == null )
            {
                throw new LifecyclePlannerException( "Mojo: " + mojoBinding.getGoal() + " does not exist in plugin: " + pluginDescriptor.getId() + "." );
            }
            
            List forkedBindings = new ArrayList();
            
            findForkedBindings( forkedBindings, mojoBinding, pluginDescriptor, mergedBindings, project, new ArrayList() );
        }
    }

    private void findForkedBindings( List forkedBindings, MojoBinding mojoBinding, PluginDescriptor pluginDescriptor, LifecycleBindings mergedBindings, MavenProject project, List forkingBindings ) throws LifecyclePlannerException, LifecycleSpecificationException
    {
        String referencingGoal = mojoBinding.getGoal();
        
        MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( referencingGoal );
        
        if ( mojoDescriptor.getExecuteGoal() != null )
        {
            forkingBindings.add( mojoBinding );
            String executeGoal = mojoDescriptor.getExecuteGoal();
            
            MojoDescriptor otherDescriptor = pluginDescriptor.getMojo( executeGoal );
            if ( otherDescriptor == null )
            {
                throw new LifecyclePlannerException( "Mojo: " + executeGoal + " (referenced by: " + referencingGoal + ") does not exist in plugin: " + pluginDescriptor.getId() + "." );
            }
            
            MojoBinding binding = new MojoBinding();
            binding.setGroupId( pluginDescriptor.getGroupId() );
            binding.setArtifactId( pluginDescriptor.getArtifactId() );
            binding.setVersion( pluginDescriptor.getVersion() );
            binding.setGoal( executeGoal );
            binding.setOrigin( "Forked from " + referencingGoal );
            
            forkedBindings.add( binding );
            
            findForkedBindings( forkedBindings, binding, pluginDescriptor, mergedBindings, project, forkingBindings );
        }
        else if ( mojoDescriptor.getExecutePhase() != null )
        {
            forkingBindings.add( mojoBinding );
            String phase = mojoDescriptor.getExecutePhase();
            
            LifecycleBinding binding = LifecycleUtils.findLifecycleBindingsForPhase( phase, mergedBindings );
            if ( binding == null )
            {
                throw new LifecyclePlannerException( "Cannot find lifecycle for phase: " + phase );
            }
            
            List mojoBindings = LifecycleUtils.getMojoBindings( phase, binding );
            
            LifecycleUtils.removeBindings( forkingBindings, mojoBindings );

            forkedBindings.addAll( mojoBindings );
        }
        else if ( mojoDescriptor.getExecuteLifecycle() != null )
        {
            // TODO: Handle lifecycle overlays!
        }
    }

    private List assemblePlanBindings( List tasks, LifecycleBindings mergedBindings, MavenProject project )
        throws LifecycleSpecificationException
    {
        List planBindings = new ArrayList();

        List lastMojoBindings = null;
        for ( Iterator it = tasks.iterator(); it.hasNext(); )
        {
            String task = (String) it.next();

            LifecycleBinding binding = LifecycleUtils.findLifecycleBindingsForPhase( task, mergedBindings );
            if ( binding != null )
            {
                List mojoBindings = LifecycleUtils.getMojoBindings( task, binding );

                // save these so we can reference the originals...
                List originalMojoBindings = mojoBindings;

                // if these mojo bindings are a superset of the last bindings, only add the difference.
                if ( lastMojoBindings != null && mojoBindings.containsAll( mojoBindings ) )
                {
                    List revised = new ArrayList( mojoBindings );
                    revised.removeAll( lastMojoBindings );

                    mojoBindings = revised;
                }

                planBindings.addAll( mojoBindings );
                lastMojoBindings = originalMojoBindings;
            }
            else
            {
                MojoBinding mojoBinding = MojoReferenceParser.parseMojoBinding( task, true );
                mojoBinding.setOrigin( "direct invocation" );
                
                planBindings.add( mojoBinding );
            }
        }
        
        return planBindings;
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

}
