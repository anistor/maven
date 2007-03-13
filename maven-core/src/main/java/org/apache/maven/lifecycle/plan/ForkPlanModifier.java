package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.LifecycleUtils;
import org.apache.maven.lifecycle.MojoBindingUtils;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.model.Phase;
import org.apache.maven.lifecycle.statemgmt.StateManagementUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ForkPlanModifier
    implements BuildPlanModifier
{

    private final MojoBinding modificationPoint;
    private List planModifiers = new ArrayList();

    private List mojoBindings;
    private LifecycleBindings lifecycleBindings;
    private String lifecyclePhase;

    public ForkPlanModifier( MojoBinding modificationPoint, List mojoBindings )
    {
        this.modificationPoint = modificationPoint;
        this.mojoBindings = mojoBindings;
    }

    public ForkPlanModifier( MojoBinding modificationPoint, LifecycleBindings modifiedBindings, String phase )
        throws LifecycleSpecificationException
    {
        this.modificationPoint = modificationPoint;
        this.lifecycleBindings = modifiedBindings;
        this.lifecyclePhase = phase;
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

        int stopIndex = -1;
        int insertionIndex = -1;
        List phaseBindings = phase.getBindings();

        for ( int i = 0; i < phaseBindings.size(); i++ )
        {
            MojoBinding candidate = (MojoBinding) phaseBindings.get( i );

            String key = LifecycleUtils.createMojoBindingKey( candidate, true );
            if ( key.equals( modificationKey ) )
            {
                insertionIndex = i;
                stopIndex = i + 1;
                break;
            }
        }
        
        phaseBindings.add( stopIndex, StateManagementUtils.createEndForkedExecutionMojoBinding() );
        
        if ( mojoBindings == null )
        {
            try
            {
                mojoBindings = BuildPlanUtils.assembleMojoBindingList( Collections.singletonList( lifecyclePhase ), lifecycleBindings );
            }
            catch ( LifecycleSpecificationException e )
            {
                throw new LifecyclePlannerException( "Error building modifications list for forked execution of " + MojoBindingUtils.toString( getModificationPoint() ) );
            }
        }
        
        phaseBindings.addAll( insertionIndex, mojoBindings );
        phaseBindings.add( insertionIndex, StateManagementUtils.createStartForkedExecutionMojoBinding() );
        
        phase.setBindings( phaseBindings );
        
        for ( Iterator it = planModifiers.iterator(); it.hasNext(); )
        {
            BuildPlanModifier modifier = (BuildPlanModifier) it.next();
            
            modifier.modifyBindings( bindings );
        }

        return bindings;
    }

    public void addModifier( BuildPlanModifier planModifier )
    {
        planModifiers.add( planModifier );
    }

    public boolean hasModifiers()
    {
        return !planModifiers.isEmpty();
    }

}
