package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.LifecycleUtils;
import org.apache.maven.lifecycle.binding.LifecycleBindingManager;
import org.apache.maven.lifecycle.model.LifecycleBinding;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.loader.PluginLoader;
import org.apache.maven.plugin.loader.PluginLoaderException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
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

        LifecycleBindings merged = LifecycleUtils.mergeBindings( packagingBindings, projectBindings, defaultBindings, false, false );

        // foreach task, find the binding list from the merged lifecycle-bindings.
        // if the binding list is a super-set of a previous task, forget the previous task/binding
        //     list, and use the new one.
        // if the binding list is null, treat it like a one-off mojo invocation, and parse/validate
        //     that it can be called as such.
        // as binding lists accumulate, push them onto an aggregated "plan" listing...
        LifecyclePlan plan = new DefaultLifecyclePlan( tasks, merged );

        // Inject forked lifecycles as plan modifiers for each mojo that has @execute in it.
        addForkedLifecycleModifiers( plan, merged, project, tasks );

        // TODO: Inject relative-ordered project/plugin executions as plan modifiers.

        return plan;
    }

    private void addForkedLifecycleModifiers( ModifiablePlanElement planElement, LifecycleBindings lifecycleBindings,
                                              MavenProject project, List tasks )
        throws LifecyclePlannerException, LifecycleSpecificationException
    {
        List planBindings = LifecycleUtils.assembleMojoBindingList( tasks, lifecycleBindings );
        
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
                throw new LifecyclePlannerException( "Mojo: " + mojoBinding.getGoal() + " does not exist in plugin: "
                    + pluginDescriptor.getId() + "." );
            }

            findForkModifiers( mojoBinding, pluginDescriptor, planElement, lifecycleBindings, project, new LinkedList(), tasks );
        }
    }

    private void findForkModifiers( MojoBinding mojoBinding, PluginDescriptor pluginDescriptor,
                                     ModifiablePlanElement planElement, LifecycleBindings mergedBindings, MavenProject project, 
                                     LinkedList forkingBindings, List tasks )
        throws LifecyclePlannerException, LifecycleSpecificationException
    {
        forkingBindings.addLast( mojoBinding );

        try
        {
            String referencingGoal = mojoBinding.getGoal();

            MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( referencingGoal );

            if ( mojoDescriptor.getExecuteGoal() != null )
            {
                recurseSingleMojoFork( mojoBinding, pluginDescriptor, planElement, mergedBindings, forkingBindings, tasks );
            }
            else if ( mojoDescriptor.getExecutePhase() != null )
            {
                recursePhaseMojoFork( mojoBinding, pluginDescriptor, planElement, mergedBindings, project, forkingBindings, tasks );
            }
            else if ( mojoDescriptor.getExecuteLifecycle() != null )
            {
                recurseLifecycleOverlayFork( mojoBinding, pluginDescriptor, planElement, mergedBindings, project, forkingBindings, tasks );
            }
        }
        finally
        {
            forkingBindings.removeLast();
        }
    }

    private void recurseLifecycleOverlayFork( MojoBinding mojoBinding, PluginDescriptor pluginDescriptor,
                                              ModifiablePlanElement planElement, LifecycleBindings mergedBindings, MavenProject project,
                                              LinkedList forkingBindings, List tasks )
        throws LifecycleSpecificationException, LifecyclePlannerException
    {
        String referencingGoal = mojoBinding.getGoal();

        MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( referencingGoal );

        String executeLifecycle = mojoDescriptor.getExecuteLifecycle();

        LifecycleBindings overlayBindings;
        try
        {
            overlayBindings = lifecycleBindingManager.getPluginLifecycleOverlay( pluginDescriptor, executeLifecycle );
        }
        catch ( LifecycleLoaderException e )
        {
            throw new LifecyclePlannerException( "Failed to load overlay lifecycle: " + executeLifecycle + ". Reason: "
                + e.getMessage(), e );
        }

        // constructed to allow us to recurse for forks/modifications, etc.
        LifecyclePlanModifier modifier = new LifecycleOverlayPlanModifier( overlayBindings );
        
        addForkedLifecycleModifiers( modifier, overlayBindings, project, tasks );
        
        planElement.addModifier( modifier );
    }

    private void recursePhaseMojoFork( MojoBinding mojoBinding, PluginDescriptor pluginDescriptor,
                                       ModifiablePlanElement planElement, LifecycleBindings mergedBindings, MavenProject project,
                                       LinkedList forkingBindings, List tasks )
        throws LifecyclePlannerException, LifecycleSpecificationException
    {
        String referencingGoal = mojoBinding.getGoal();

        MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( referencingGoal );

        String phase = mojoDescriptor.getExecutePhase();

        LifecycleBinding binding = LifecycleUtils.findLifecycleBindingForPhase( phase, mergedBindings );
        if ( binding == null )
        {
            throw new LifecyclePlannerException( "Cannot find lifecycle for phase: " + phase );
        }

        LifecycleBindings cloned = LifecycleUtils.cloneBindings( mergedBindings );

        LifecycleUtils.removeMojoBindings( forkingBindings, cloned, false );

        List forkedPhaseBindingList = LifecycleUtils.assembleMojoBindingList( Collections.singletonList( phase ), cloned );
        
        LifecyclePlanModifier modifier = new DefaultLifecyclePlanModifier( mojoBinding, forkedPhaseBindingList );
        
        for ( Iterator it = forkedPhaseBindingList.iterator(); it.hasNext(); )
        {
            MojoBinding forkedBinding = (MojoBinding) it.next();
            
            findForkModifiers( forkedBinding, pluginDescriptor, modifier, cloned, project, forkingBindings, tasks );
        }
        
        planElement.addModifier( modifier );
    }

    private void recurseSingleMojoFork( MojoBinding mojoBinding, PluginDescriptor pluginDescriptor,
                                        ModifiablePlanElement planElement, LifecycleBindings mergedBindings, LinkedList forkingBindings, List tasks )
        throws LifecyclePlannerException, LifecycleSpecificationException
    {
        String referencingGoal = mojoBinding.getGoal();

        MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( referencingGoal );

        String executeGoal = mojoDescriptor.getExecuteGoal();

        MojoDescriptor otherDescriptor = pluginDescriptor.getMojo( executeGoal );
        if ( otherDescriptor == null )
        {
            throw new LifecyclePlannerException( "Mojo: " + executeGoal + " (referenced by: " + referencingGoal
                + ") does not exist in plugin: " + pluginDescriptor.getId() + "." );
        }
        
        MojoBinding binding = new MojoBinding();
        binding.setGroupId( pluginDescriptor.getGroupId() );
        binding.setArtifactId( pluginDescriptor.getArtifactId() );
        binding.setVersion( pluginDescriptor.getVersion() );
        binding.setGoal( executeGoal );
        binding.setOrigin( "Forked from " + referencingGoal );
        
        if ( !LifecycleUtils.isMojoBindingPresent( binding, forkingBindings, false ) )
        {
            planElement.addModifier( new DefaultLifecyclePlanModifier( mojoBinding, Collections.singletonList( binding ) ) );

            forkingBindings.addLast( binding );
            try
            {
                recurseSingleMojoFork( binding, pluginDescriptor, planElement, mergedBindings, forkingBindings, tasks );
            }
            finally
            {
                forkingBindings.removeLast();
            }
        }
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

}
