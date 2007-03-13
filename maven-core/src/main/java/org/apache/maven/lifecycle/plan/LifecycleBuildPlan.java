package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.LifecycleUtils;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LifecycleBuildPlan
    implements BuildPlan
{

    private final List tasks;
    private final LifecycleBindings lifecycleBindings;
    
    private List planModifiers = new ArrayList();
    private Map directInvocationPlans = new HashMap();

    public LifecycleBuildPlan( List tasks, LifecycleBindings lifecycleBindings )
    {
        this.tasks = tasks;
        this.lifecycleBindings = lifecycleBindings;
    }

    public List getPlanMojoBindings()
        throws LifecycleSpecificationException, LifecyclePlannerException
    {
        LifecycleBindings cloned = BuildPlanUtils.modifyPlanBindings( lifecycleBindings, planModifiers );
        
        return BuildPlanUtils.assembleMojoBindingList( tasks, cloned, directInvocationPlans );
    }
    
    public List getTasks()
    {
        return tasks;
    }

    public void addModifier( BuildPlanModifier planModifier )
    {
        planModifiers.add( planModifier );
    }

    public boolean hasModifiers()
    {
        return !planModifiers.isEmpty();
    }

    public void addDirectInvocationPlan( MojoBinding directInvocationBinding, BuildPlan plan )
    {
        directInvocationPlans.put( LifecycleUtils.createMojoBindingKey( directInvocationBinding, true ), plan );
    }

    public boolean hasDirectInvocationPlans()
    {
        return !directInvocationPlans.isEmpty();
    }

}
