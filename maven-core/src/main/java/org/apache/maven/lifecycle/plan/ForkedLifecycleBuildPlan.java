package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.statemgmt.StateManagementUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ForkedLifecycleBuildPlan
    extends LifecycleBuildPlan
{
    
    private final MojoBinding forkPoint;

    public ForkedLifecycleBuildPlan( MojoBinding forkPoint, String phase, LifecycleBindings bindings )
    {
        super( Collections.singletonList( phase ), bindings );
        this.forkPoint = forkPoint;
    }

    public List getPlanMojoBindings()
        throws LifecycleSpecificationException, LifecyclePlannerException
    {
        List bindings = new ArrayList( super.getPlanMojoBindings() );
        
        bindings.add( 0, StateManagementUtils.createStartForkedExecutionMojoBinding() );
        bindings.add( forkPoint );
        bindings.add( StateManagementUtils.createEndForkedExecutionMojoBinding() );
        
        return bindings;
    }

}
