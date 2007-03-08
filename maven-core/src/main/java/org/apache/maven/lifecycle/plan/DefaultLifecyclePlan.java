package org.apache.maven.lifecycle.plan;

import java.util.List;

public class DefaultLifecyclePlan
    implements LifecyclePlan
{

    private final List planBindings;

    public DefaultLifecyclePlan( List planBindings )
    {
        this.planBindings = planBindings;
    }

}
