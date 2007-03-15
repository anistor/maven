package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.LifecycleUtils;
import org.apache.maven.lifecycle.MojoBindingUtils;
import org.apache.maven.lifecycle.binding.LifecycleBindingManager;
import org.apache.maven.lifecycle.binding.MojoBindingFactory;
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

public class DefaultBuildPlanner
    implements BuildPlanner, LogEnabled
{

    private Logger logger;

    private PluginLoader pluginLoader;

    private LifecycleBindingManager lifecycleBindingManager;

    private MojoBindingFactory mojoBindingFactory;

    public BuildPlan constructLifecyclePlan( List tasks, MavenProject project )
        throws LifecycleLoaderException, LifecycleSpecificationException, LifecyclePlannerException
    {
        LifecycleBindings defaultBindings = lifecycleBindingManager.getDefaultBindings( project );
        LifecycleBindings packagingBindings = lifecycleBindingManager.getBindingsForPackaging( project );
        LifecycleBindings projectBindings = lifecycleBindingManager.getProjectCustomBindings( project );

        LifecycleBindings merged = LifecycleUtils.mergeBindings( packagingBindings, projectBindings, defaultBindings, true, false );

        // foreach task, find the binding list from the merged lifecycle-bindings.
        // if the binding list is a super-set of a previous task, forget the previous task/binding
        //     list, and use the new one.
        // if the binding list is null, treat it like a one-off mojo invocation, and parse/validate
        //     that it can be called as such.
        // as binding lists accumulate, push them onto an aggregated "plan" listing...
        BuildPlan plan = new LifecycleBuildPlan( tasks, merged );

        // Inject forked lifecycles as plan modifiers for each mojo that has @execute in it.
        addForkedLifecycleModifiers( plan, merged, project, tasks );
        addReportingLifecycleModifiers( plan, merged, project, tasks );

        // TODO: Inject relative-ordered project/plugin executions as plan modifiers.

        return plan;
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

    private void addForkedLifecycleModifiers( ModifiablePlanElement planElement, LifecycleBindings lifecycleBindings,
                                              MavenProject project, List tasks )
        throws LifecyclePlannerException, LifecycleSpecificationException, LifecycleLoaderException
    {
        List planBindings = lifecycleBindingManager.assembleMojoBindingList( tasks, lifecycleBindings, project );

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

    private void addReportingLifecycleModifiers( ModifiablePlanElement planElement, LifecycleBindings lifecycleBindings,
                                                 MavenProject project, List tasks )
        throws LifecyclePlannerException, LifecycleSpecificationException, LifecycleLoaderException
    {
        List planBindings = lifecycleBindingManager.assembleMojoBindingList( tasks, lifecycleBindings, project );

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

            if ( mojoDescriptor.isRequiresReports() )
            {
                List reportBindings = lifecycleBindingManager.getReportBindings( project );

                BuildPlanModifier modder = new ReportingPlanModifier( mojoBinding, reportBindings );

                planElement.addModifier( modder );

                // NOTE: the first sighting of a mojo requiring reports should satisfy this condition.
                // therefore, we can break out as soon as we find one.
                break;
            }
        }
    }

    private void findForkModifiers( MojoBinding mojoBinding, PluginDescriptor pluginDescriptor,
                                    ModifiablePlanElement planElement, LifecycleBindings mergedBindings, MavenProject project,
                                    LinkedList forkingBindings, List tasks )
        throws LifecyclePlannerException, LifecycleSpecificationException, LifecycleLoaderException
    {
        forkingBindings.addLast( mojoBinding );

        try
        {
            String referencingGoal = mojoBinding.getGoal();

            MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( referencingGoal );

            if ( mojoDescriptor.getExecuteGoal() != null )
            {
                recurseSingleMojoFork( mojoBinding, pluginDescriptor, planElement, mergedBindings, project, forkingBindings,
                                       tasks );
            }
            else if ( mojoDescriptor.getExecutePhase() != null )
            {
                recursePhaseMojoFork( mojoBinding, pluginDescriptor, planElement, mergedBindings, project, forkingBindings, tasks );
            }
        }
        finally
        {
            forkingBindings.removeLast();
        }
    }

    private void modifyBuildPlanForForkedDirectInvocation( MojoBinding invokedBinding, MojoBinding invokedVia,
                                                           PluginDescriptor pluginDescriptor, ModifiablePlanElement planElement,
                                                           LifecycleBindings mergedBindings, MavenProject project,
                                                           LinkedList forkingBindings, List tasks )
        throws LifecyclePlannerException, LifecycleSpecificationException, LifecycleLoaderException
    {
        if ( LifecycleUtils.findPhaseForMojoBinding( invokedVia, mergedBindings, true ) != null )
        {
            ForkPlanModifier modifier = new ForkPlanModifier( invokedVia, Collections.singletonList( invokedBinding ) );

            forkingBindings.addLast( invokedBinding );
            try
            {
                findForkModifiers( invokedBinding, pluginDescriptor, modifier, mergedBindings, project, forkingBindings, tasks );
            }
            finally
            {
                forkingBindings.removeLast();
            }

            planElement.addModifier( modifier );
        }
        else if ( planElement instanceof BuildPlan )
        {
            BuildPlan directInvocationPlan = new DirectInvocationPlan( invokedBinding, tasks );

            forkingBindings.addLast( invokedBinding );
            try
            {
                findForkModifiers( invokedBinding, pluginDescriptor, directInvocationPlan, mergedBindings, project,
                                   forkingBindings, tasks );
            }
            finally
            {
                forkingBindings.removeLast();
            }

            // if we don't have further modifications to this single direct invocation, don't weigh
            // things down.
            if ( directInvocationPlan.hasModifiers() || directInvocationPlan.hasDirectInvocationPlans() )
            {
                ( (BuildPlan) planElement ).addDirectInvocationPlan( invokedVia, directInvocationPlan );
            }
        }
        else
        {
            throw new LifecyclePlannerException( "Mojo: " + MojoBindingUtils.toString( invokedVia )
                + " is not bound to the lifecycle; you cannot attach this mojo to a build-plan modifier." );
        }
    }

    private void modifyBuildPlanForForkedLifecycle( MojoBinding mojoBinding, PluginDescriptor pluginDescriptor,
                                                    ModifiablePlanElement planElement, LifecycleBindings bindings,
                                                    MavenProject project, LinkedList forkingBindings, List tasks )
        throws LifecycleSpecificationException, LifecyclePlannerException, LifecycleLoaderException
    {
        MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( mojoBinding.getGoal() );
        String phase = mojoDescriptor.getExecutePhase();

        List forkedPhaseBindingList = lifecycleBindingManager.assembleMojoBindingList( Collections.singletonList( phase ),
                                                                                       bindings, project );

        ModifiablePlanElement mpe;

        // setup the ModifiablePlanElement, into which we'll recurse to find further modifications.
        if ( LifecycleUtils.findPhaseForMojoBinding( mojoBinding, bindings, true ) != null )
        {
            mpe = new ForkPlanModifier( mojoBinding, forkedPhaseBindingList );
        }
        else if ( planElement instanceof BuildPlan )
        {
            mpe = new ForkedLifecycleBuildPlan( mojoBinding, phase, bindings );
        }
        else
        {
            throw new LifecyclePlannerException( "Mojo: " + MojoBindingUtils.toString( mojoBinding )
                + " is not bound to the lifecycle; you cannot attach this mojo to a build-plan modifier." );
        }

        // recurse, to find further modifications, using the ModifiablePlanElement from above, along
        // with the modified task list (which doesn't contain the direct-invocation task that landed
        // us here...
        for ( Iterator it = forkedPhaseBindingList.iterator(); it.hasNext(); )
        {
            MojoBinding forkedBinding = (MojoBinding) it.next();

            PluginDescriptor forkedPluginDescriptor;
            try
            {
                forkedPluginDescriptor = pluginLoader.loadPlugin( forkedBinding, project );
            }
            catch ( PluginLoaderException e )
            {
                throw new LifecyclePlannerException( e.getMessage(), e );
            }

            findForkModifiers( forkedBinding, forkedPluginDescriptor, mpe, bindings, project, forkingBindings, tasks );
        }

        // now that we've discovered any deeper modifications, add the current MPE to the parent MPE
        // in the appropriate location.
        if ( LifecycleUtils.findPhaseForMojoBinding( mojoBinding, bindings, true ) != null )
        {
            planElement.addModifier( (BuildPlanModifier) mpe );
        }
        else if ( planElement instanceof BuildPlan )
        {
            ( (BuildPlan) planElement ).addDirectInvocationPlan( mojoBinding, (BuildPlan) mpe );
        }
    }

    private void recursePhaseMojoFork( MojoBinding mojoBinding, PluginDescriptor pluginDescriptor,
                                       ModifiablePlanElement planElement, LifecycleBindings mergedBindings, MavenProject project,
                                       LinkedList forkingBindings, List tasks )
        throws LifecyclePlannerException, LifecycleSpecificationException, LifecycleLoaderException
    {
        String referencingGoal = mojoBinding.getGoal();

        MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( referencingGoal );

        String phase = mojoDescriptor.getExecutePhase();

        if ( phase == null )
        {
            return;
        }

        if ( LifecycleUtils.findLifecycleBindingForPhase( phase, mergedBindings ) == null )
        {
            throw new LifecyclePlannerException( "Cannot find lifecycle for phase: " + phase );
        }

        LifecycleBindings cloned;
        if ( mojoDescriptor.getExecuteLifecycle() != null )
        {
            String executeLifecycle = mojoDescriptor.getExecuteLifecycle();

            LifecycleBindings overlayBindings;
            try
            {
                overlayBindings = lifecycleBindingManager.getPluginLifecycleOverlay( pluginDescriptor, executeLifecycle, project );
            }
            catch ( LifecycleLoaderException e )
            {
                throw new LifecyclePlannerException( "Failed to load overlay lifecycle: " + executeLifecycle + ". Reason: "
                    + e.getMessage(), e );
            }

            cloned = LifecycleUtils.cloneBindings( mergedBindings );
            cloned = LifecycleUtils.mergeBindings( overlayBindings, cloned, null, true, true );
        }
        else
        {
            cloned = LifecycleUtils.cloneBindings( mergedBindings );
        }

        LifecycleUtils.removeMojoBindings( forkingBindings, cloned, false );

        modifyBuildPlanForForkedLifecycle( mojoBinding, pluginDescriptor, planElement, cloned, project, forkingBindings, tasks );
    }

    private void recurseSingleMojoFork( MojoBinding mojoBinding, PluginDescriptor pluginDescriptor,
                                        ModifiablePlanElement planElement, LifecycleBindings mergedBindings,
                                        MavenProject project, LinkedList forkingBindings, List tasks )
        throws LifecyclePlannerException, LifecycleSpecificationException, LifecycleLoaderException
    {
        String referencingGoal = mojoBinding.getGoal();

        MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( referencingGoal );

        String executeGoal = mojoDescriptor.getExecuteGoal();

        if ( executeGoal == null )
        {
            return;
        }

        MojoDescriptor otherDescriptor = pluginDescriptor.getMojo( executeGoal );
        if ( otherDescriptor == null )
        {
            throw new LifecyclePlannerException( "Mojo: " + executeGoal + " (referenced by: " + referencingGoal
                + ") does not exist in plugin: " + pluginDescriptor.getId() + "." );
        }

        MojoBinding binding = mojoBindingFactory.createMojoBinding( pluginDescriptor.getGroupId(),
                                                                    pluginDescriptor.getArtifactId(),
                                                                    pluginDescriptor.getVersion(), executeGoal, project );

        binding.setOrigin( "Forked from " + referencingGoal );

        if ( !LifecycleUtils.isMojoBindingPresent( binding, forkingBindings, false ) )
        {
            modifyBuildPlanForForkedDirectInvocation( binding, mojoBinding, pluginDescriptor, planElement, mergedBindings,
                                                      project, forkingBindings, tasks );
        }
    }

}
