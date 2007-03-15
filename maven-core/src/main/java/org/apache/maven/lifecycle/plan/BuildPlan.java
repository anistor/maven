package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.binding.LifecycleBindingManager;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.project.MavenProject;

import java.util.List;

public interface BuildPlan
    extends ModifiablePlanElement
{

    List getPlanMojoBindings(MavenProject project, LifecycleBindingManager bindingManager)
        throws LifecycleSpecificationException, LifecyclePlannerException, LifecycleLoaderException;

    void addDirectInvocationPlan( MojoBinding directInvocationBinding, BuildPlan plan );

    boolean hasDirectInvocationPlans();

    List getTasks();

}
