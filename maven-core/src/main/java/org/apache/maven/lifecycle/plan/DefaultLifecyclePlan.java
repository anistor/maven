package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.LifecycleUtils;
import org.apache.maven.lifecycle.model.LifecycleBindings;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DefaultLifecyclePlan
    implements LifecyclePlan
{

    private final List tasks;
    private final LifecycleBindings lifecycleBindings;
    
    private List planModifiers = new ArrayList();

    public DefaultLifecyclePlan( List tasks, LifecycleBindings lifecycleBindings )
    {
        this.tasks = tasks;
        this.lifecycleBindings = lifecycleBindings;
    }

    public List getPlanMojoBindings()
        throws LifecycleSpecificationException, LifecyclePlannerException
    {
        LifecycleBindings cloned = LifecycleUtils.cloneBindings( lifecycleBindings );
        
        for ( Iterator it = planModifiers.iterator(); it.hasNext(); )
        {
            LifecyclePlanModifier modifier = (LifecyclePlanModifier) it.next();
            
            cloned = modifier.modifyBindings( cloned );
        }
        
        return LifecycleUtils.assembleMojoBindingList( tasks, cloned );
    }
    
    public LifecycleBindings getPlanLifecycleBindings()
    {
        return lifecycleBindings;
    }
    
    public List getTasks()
    {
        return tasks;
    }

    public void addModifier( LifecyclePlanModifier planModifier )
    {
        planModifiers.add( planModifier );
    }

    public List getModifiers()
    {
        return planModifiers;
    }

}
