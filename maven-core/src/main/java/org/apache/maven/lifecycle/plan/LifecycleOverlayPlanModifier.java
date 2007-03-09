package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleUtils;
import org.apache.maven.lifecycle.model.LifecycleBindings;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LifecycleOverlayPlanModifier
    implements LifecyclePlanModifier
{
    
    private final LifecycleBindings overlay;
    private List planModifiers = new ArrayList();

    public LifecycleOverlayPlanModifier( LifecycleBindings overlay )
    {
        this.overlay = overlay;
    }

    public LifecycleBindings modifyBindings( LifecycleBindings bindings )
        throws LifecyclePlannerException
    {
        LifecycleBindings cloned = LifecycleUtils.cloneBindings( overlay );
        
        if ( planModifiers != null && !planModifiers.isEmpty() )
        {
            for ( Iterator it = planModifiers.iterator(); it.hasNext(); )
            {
                LifecyclePlanModifier modifier = (LifecyclePlanModifier) it.next();
                
                cloned = modifier.modifyBindings( cloned );
            }
        }
        
        // the ordering of these LifecycleBindings instances may seem reversed, but it is done this
        // way on purpose, in order to make the configurations from the main bindings be dominant
        // over those specified in the lifecycle overlay.
        return LifecycleUtils.mergeBindings( cloned, bindings, null, true );
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
