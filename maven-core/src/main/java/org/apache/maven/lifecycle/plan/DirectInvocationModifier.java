package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.binding.LifecycleBindingManager;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.project.MavenProject;

import java.util.List;

public interface DirectInvocationModifier
{
    
    MojoBinding getBindingToModify();
    
    List getModifiedBindings( MavenProject project, LifecycleBindingManager bindingManager );

}
