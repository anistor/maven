package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleBindings;

public class DefaultLifecyclePlan
    implements LifecyclePlan
{

    private final LifecycleBindings defaultBindings;
    private final LifecycleBindings packagingBindings;
    private final LifecycleBindings projectBindings;

    public DefaultLifecyclePlan( LifecycleBindings defaultBindings, LifecycleBindings packagingBindings,
                                 LifecycleBindings projectBindings )
    {
        this.defaultBindings = defaultBindings;
        this.packagingBindings = packagingBindings;
        this.projectBindings = projectBindings;
    }

}
