package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.LifecycleUtils;
import org.apache.maven.lifecycle.binding.LifecycleBindingManager;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.project.MavenProject;

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

    private Map directInvocationModifiers = new HashMap();

    public LifecycleBuildPlan( List tasks, LifecycleBindings lifecycleBindings )
    {
        this.tasks = tasks;
        this.lifecycleBindings = lifecycleBindings;
    }

    public List getPlanMojoBindings( MavenProject project, LifecycleBindingManager bindingManager )
        throws LifecycleSpecificationException, LifecyclePlannerException, LifecycleLoaderException
    {
        LifecycleBindings cloned = BuildPlanUtils.modifyPlanBindings( lifecycleBindings, planModifiers );

        return bindingManager.assembleMojoBindingList( tasks, cloned, directInvocationModifiers, project );
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

    public void addDirectInvocationModifier( DirectInvocationModifier modifier )
    {
        directInvocationModifiers.put( LifecycleUtils.createMojoBindingKey( modifier.getBindingToModify(), true ), modifier );
    }

}
