package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleBindings;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.LifecycleUtils;
import org.apache.maven.lifecycle.MojoBinding;
import org.apache.maven.lifecycle.Phase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class DefaultLifecyclePlanModifier
    implements LifecyclePlanModifier
{

    private final MojoBinding modificationPoint;
    private List planModifiers = new ArrayList();

    private final List mojoBindings;

    public DefaultLifecyclePlanModifier( MojoBinding modificationPoint, List mojoBindings )
    {
        this.modificationPoint = modificationPoint;
        this.mojoBindings = mojoBindings;
    }

    public DefaultLifecyclePlanModifier( MojoBinding modificationPoint, LifecycleBindings modifiedBindings, String phase )
        throws LifecycleSpecificationException
    {
        this.modificationPoint = modificationPoint;
        this.mojoBindings = LifecycleUtils.assembleMojoBindingList( Collections.singletonList( phase ), modifiedBindings );
    }

    public MojoBinding getModificationPoint()
    {
        return modificationPoint;
    }

    public LifecycleBindings modifyBindings( LifecycleBindings bindings )
        throws LifecyclePlannerException
    {
        Phase phase = LifecycleUtils.findPhaseForMojoBinding( getModificationPoint(), bindings, true );

        String modificationKey = LifecycleUtils.createMojoBindingKey( getModificationPoint(), true );

        if ( phase == null )
        {
            throw new LifecyclePlannerException( "Failed to modify plan. No phase found containing mojoBinding: "
                + modificationKey );
        }

        int insertionIndex = -1;
        List phaseBindings = phase.getBindings();

        for ( int i = 0; i < phaseBindings.size(); i++ )
        {
            MojoBinding candidate = (MojoBinding) phaseBindings.get( i );

            String key = LifecycleUtils.createMojoBindingKey( candidate, true );
            if ( key.equals( modificationKey ) )
            {
                insertionIndex = i + 1;
                break;
            }
        }
        
        phaseBindings.addAll( insertionIndex, mojoBindings );
        phase.setBindings( phaseBindings );
        
        for ( Iterator it = planModifiers.iterator(); it.hasNext(); )
        {
            LifecyclePlanModifier modifier = (LifecyclePlanModifier) it.next();
            
            modifier.modifyBindings( bindings );
        }

        return bindings;
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
