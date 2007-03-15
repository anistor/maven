package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.binding.LifecycleBindingManager;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SubLifecycleBuildPlan
    extends LifecycleBuildPlan
{
    
    public SubLifecycleBuildPlan( String phase, LifecycleBindings bindings )
    {
        super( Collections.singletonList( phase ), bindings );
    }

    public List getPlanMojoBindings(MavenProject project, LifecycleBindingManager bindingManager)
        throws LifecycleSpecificationException, LifecyclePlannerException, LifecycleLoaderException
    {
        List bindings = new ArrayList( super.getPlanMojoBindings(project, bindingManager) );
        
        return bindings;
    }

}
