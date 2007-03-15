package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.LifecycleUtils;
import org.apache.maven.lifecycle.binding.LifecycleBindingManager;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DirectInvocationPlan
    implements BuildPlan
{

    private List planModifiers = new ArrayList();

    private Map directInvocationPlans = new HashMap();

    private final MojoBinding directInvocationBinding;

    private final List tasks;

    public DirectInvocationPlan( MojoBinding binding, List tasks )
    {
        this.directInvocationBinding = binding;
        this.tasks = tasks;
    }

    public void addDirectInvocationPlan( MojoBinding directInvocationBinding, BuildPlan plan )
    {
        directInvocationPlans.put( LifecycleUtils.createMojoBindingKey( directInvocationBinding, true ), plan );
    }

    public boolean hasDirectInvocationPlans()
    {
        return !directInvocationPlans.isEmpty();
    }

    public List getPlanMojoBindings( MavenProject project, LifecycleBindingManager bindingManager )
        throws LifecycleSpecificationException, LifecyclePlannerException, LifecycleLoaderException
    {
        // null parameter here avoids creating an extra instance just for purposes of cloning.
        LifecycleBindings bindings = BuildPlanUtils.modifyPlanBindings( null, planModifiers );

        List result = bindingManager.assembleMojoBindingList( tasks, bindings, directInvocationPlans, project );

        result.add( directInvocationBinding );

        return result;
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

}
