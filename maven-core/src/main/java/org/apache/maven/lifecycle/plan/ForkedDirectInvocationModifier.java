package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.binding.LifecycleBindingManager;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.statemgmt.StateManagementUtils;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.List;

public class ForkedDirectInvocationModifier
    implements DirectInvocationModifier
{

    private final List forkedBindings;
    private final MojoBinding forkingBinding;

    public ForkedDirectInvocationModifier( MojoBinding forkingBinding, List forkedBindings )
    {
        this.forkingBinding = forkingBinding;
        this.forkedBindings = forkedBindings;
    }

    public List getModifiedBindings( MavenProject project, LifecycleBindingManager bindingManager )
    {
        List result = new ArrayList();

        result.add( StateManagementUtils.createStartForkedExecutionMojoBinding() );
        result.addAll( forkedBindings );
        result.add( StateManagementUtils.createEndForkedExecutionMojoBinding() );
        result.add( forkingBinding );
        result.add( StateManagementUtils.createClearForkedExecutionMojoBinding() );

        return result;
    }

    public MojoBinding getBindingToModify()
    {
        return forkingBinding;
    }

}
