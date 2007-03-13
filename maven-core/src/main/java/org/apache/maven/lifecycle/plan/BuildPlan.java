package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.model.MojoBinding;

import java.util.List;

public interface BuildPlan
    extends ModifiablePlanElement
{

    List getPlanMojoBindings()
        throws LifecycleSpecificationException, LifecyclePlannerException;

    void addDirectInvocationPlan( MojoBinding directInvocationBinding, BuildPlan plan );

    boolean hasDirectInvocationPlans();

    List getTasks();

}
