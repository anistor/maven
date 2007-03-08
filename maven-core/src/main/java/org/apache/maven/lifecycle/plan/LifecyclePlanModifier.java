package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleBindings;

public interface LifecyclePlanModifier extends ModifiablePlanElement
{

    LifecycleBindings modifyBindings( LifecycleBindings bindings )
        throws LifecyclePlannerException;

}
