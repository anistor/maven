package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.LifecycleUtils;
import org.apache.maven.lifecycle.MojoBindingUtils;
import org.apache.maven.lifecycle.NoSuchPhaseException;
import org.apache.maven.lifecycle.model.LifecycleBinding;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.statemgmt.StateManagementUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class BuildPlan
{
    private LifecycleBindings bindings;

    private final List tasks;

    private final Map forkedDirectInvocations;

    private final Map forkedPhases;

    private List renderedLifecycleMojos = new ArrayList();

    private final Map directInvocationBindings;

    public BuildPlan( final LifecycleBindings packaging, final LifecycleBindings projectBindings,
                      final LifecycleBindings defaults, final List tasks )
        throws LifecycleSpecificationException, LifecycleLoaderException
    {
        this( LifecycleUtils.mergeBindings( packaging, projectBindings, defaults, true, false ), tasks );
    }

    public BuildPlan( final LifecycleBindings bindings, final List tasks )
        throws LifecycleSpecificationException, LifecycleLoaderException
    {
        this.bindings = bindings;
        this.tasks = tasks;
        forkedDirectInvocations = new HashMap();
        forkedPhases = new HashMap();
        directInvocationBindings = new HashMap();
    }

    private BuildPlan( final LifecycleBindings bindings, final Map forkedDirectInvocations, final Map forkedPhases,
                       final Map directInvocationBindings, final List tasks )
    {
        this.bindings = LifecycleUtils.cloneBindings( bindings );
        this.forkedDirectInvocations = new HashMap( forkedDirectInvocations );
        this.forkedPhases = new HashMap( forkedPhases );
        this.tasks = tasks;
        this.directInvocationBindings = new HashMap( directInvocationBindings );
    }

    public void addLifecycleOverlay( final LifecycleBindings overlay )
    {
        bindings = LifecycleUtils.mergeBindings( overlay, bindings, null, true, true );
    }

    public void addTask( final String task, final MojoBinding binding )
        throws LifecycleSpecificationException, LifecycleLoaderException
    {
        tasks.add( task );

        if ( !LifecycleUtils.isValidPhaseName( task ) )
        {
            addDirectInvocationBinding( task, binding );
        }
    }

    public LifecycleBindings getLifecycleBindings()
    {
        return bindings;
    }

    public List getTasks()
    {
        return tasks;
    }

    public void addDirectInvocationBinding( final String key, final MojoBinding binding )
    {
        directInvocationBindings.put( MojoBindingUtils.createMojoBindingKey( binding, false ), binding );
        directInvocationBindings.put( key, binding );
    }

    public Map getDirectInvocationBindings()
    {
        return directInvocationBindings;
    }

    public Map getForkedDirectInvocations()
    {
        return forkedDirectInvocations;
    }

    public Map getForkedPhases()
    {
        return forkedPhases;
    }

    public void addForkedExecution( final MojoBinding mojoBinding, final BuildPlan plan )
    {
        String key = MojoBindingUtils.createMojoBindingKey( mojoBinding, false );

        forkedPhases.put( key, plan );
    }

    public void addForkedExecution( final MojoBinding mojoBinding, final List forkedInvocations )
    {
        String key = MojoBindingUtils.createMojoBindingKey( mojoBinding, false );

        List invoke = (List) forkedDirectInvocations.get( key );

        if ( invoke == null )
        {
            invoke = new ArrayList();
            forkedDirectInvocations.put( key, invoke );
        }

        invoke.addAll( forkedInvocations );
    }

    public BuildPlan copy( final List newTasks )
    {
        return new BuildPlan( bindings, forkedDirectInvocations, forkedPhases, directInvocationBindings, newTasks );
    }

    public void resetExecutionProgress()
    {
        renderedLifecycleMojos = new ArrayList();
        for ( Iterator it = forkedPhases.values().iterator(); it.hasNext(); )
        {
            BuildPlan plan = (BuildPlan) it.next();
            plan.resetExecutionProgress();
        }
    }

    public List renderExecutionPlan( final Stack executionStack )
        throws NoSuchPhaseException
    {
        List plan = new ArrayList();

        for ( Iterator it = tasks.iterator(); it.hasNext(); )
        {
            String task = (String) it.next();

            if ( LifecycleUtils.isValidPhaseName( task ) )
            {
                // find the lifecyle that this task belongs in...
                LifecycleBinding binding = LifecycleUtils.findLifecycleBindingForPhase( task, bindings );

                // get the list of mojo bindings before or at the specified phase in the lifecycle
                List bindingsToAdd = LifecycleUtils.getMojoBindingListForLifecycle( task, binding );

                // backup copy for rendered tracking...
                List newRendered = new ArrayList( bindingsToAdd );

                // if we've already executed part of the lifecycle, just continue with the new bindings.
                if ( ( renderedLifecycleMojos != null ) && ( renderedLifecycleMojos.size() < bindingsToAdd.size() )
                     && bindingsToAdd.containsAll( renderedLifecycleMojos ) )
                {
                    bindingsToAdd = bindingsToAdd.subList( renderedLifecycleMojos.size(), bindingsToAdd.size() );
                }

                // save the current binding list for tracking progress through the lifecycle.
                renderedLifecycleMojos = newRendered;

                for ( Iterator itMojos = bindingsToAdd.iterator(); itMojos.hasNext(); )
                {
                    MojoBinding mojoBinding = (MojoBinding) itMojos.next();
                    String key = MojoBindingUtils.createMojoBindingKey( mojoBinding, false );
                    if ( !executionStack.contains( key ) )
                    {
                        addResolverIfLateBound( mojoBinding, plan );
                        plan.addAll( findForkedBindings( mojoBinding, executionStack ) );
                    }
                }
            }
            else
            {
                MojoBinding mojoBinding = (MojoBinding) directInvocationBindings.get( task );
                String key = MojoBindingUtils.createMojoBindingKey( mojoBinding, false );
                if ( !executionStack.contains( key ) )
                {
                    addResolverIfLateBound( mojoBinding, plan );
                    plan.addAll( findForkedBindings( mojoBinding, executionStack ) );
                }
            }
        }

        return plan;
    }

    private void addResolverIfLateBound( final MojoBinding mojoBinding, final List plan )
    {
        if ( mojoBinding.isLateBound() )
        {
            MojoBinding resolveBinding = StateManagementUtils.createResolveLateBoundMojoBinding( mojoBinding );
            plan.add( resolveBinding );
        }
    }

    private List findForkedBindings( final MojoBinding mojoBinding, final Stack executionStack )
        throws NoSuchPhaseException
    {
        List bindings = new ArrayList();

        String key = MojoBindingUtils.createMojoBindingKey( mojoBinding, false );

        executionStack.push( key );
        try
        {
            List forkedBindings = new ArrayList();

            BuildPlan forkedPlan = (BuildPlan) forkedPhases.get( key );

            // if we have a forked build plan, recurse into it and retrieve the execution plan.
            if ( forkedPlan != null )
            {
                forkedBindings = forkedPlan.renderExecutionPlan( executionStack );
            }

            List directInvocations = (List) forkedDirectInvocations.get( key );

            // leave room for new kinds of forks, and do some extra validation...
            // if this is a list, it's a list of direct mojo invocations...we have to see if they have their own
            // forks.
            if ( directInvocations != null )
            {
                for ( Iterator it = directInvocations.iterator(); it.hasNext(); )
                {
                    MojoBinding invocation = (MojoBinding) it.next();

                    String invocationKey = MojoBindingUtils.createMojoBindingKey( invocation, false );

                    if ( executionStack.contains( invocationKey ) )
                    {
                        continue;
                    }

                    addResolverIfLateBound( invocation, forkedBindings );
                    forkedBindings.addAll( findForkedBindings( invocation, executionStack ) );
                }
            }

            if ( !forkedBindings.isEmpty() )
            {
                bindings.add( StateManagementUtils.createStartForkedExecutionMojoBinding() );
                bindings.addAll( forkedBindings );
                bindings.add( StateManagementUtils.createEndForkedExecutionMojoBinding() );
            }

            bindings.add( mojoBinding );

            if ( !forkedBindings.isEmpty() )
            {
                bindings.add( StateManagementUtils.createClearForkedExecutionMojoBinding() );
            }
        }
        finally
        {
            executionStack.pop();
        }

        return bindings;
    }

}
