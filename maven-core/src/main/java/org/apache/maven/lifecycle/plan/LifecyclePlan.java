package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleBindings;
import org.apache.maven.lifecycle.LifecycleSpecificationException;

import java.util.List;

public interface LifecyclePlan extends ModifiablePlanElement
{

    List getPlanMojoBindings()
        throws LifecycleSpecificationException, LifecyclePlannerException;
    
    LifecycleBindings getPlanLifecycleBindings();
    
    List getTasks();

}
