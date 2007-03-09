package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.model.LifecycleBindings;

import java.util.List;

public interface LifecyclePlan extends ModifiablePlanElement
{

    List getPlanMojoBindings()
        throws LifecycleSpecificationException, LifecyclePlannerException;
    
    LifecycleBindings getPlanLifecycleBindings();
    
    List getTasks();

}
