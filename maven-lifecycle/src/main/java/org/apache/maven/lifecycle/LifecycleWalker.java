package org.apache.maven.lifecycle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LifecycleWalker
{
    
    private LifecycleWalker()
    {
    }

    public static List getMojoBindings( String phaseName, LifecycleBinding lifecycle )
        throws NoSuchPhaseException
    {
        List phaseNames = lifecycle.getPhaseBindingNamesInOrder();

        int idx = phaseNames.indexOf( phaseName );

        if ( idx < 0 )
        {
            throw new NoSuchPhaseException( phaseName, "Phase not found in lifecycle: " + lifecycle.getId() );
        }

        List phases = lifecycle.getPhaseBindingsInOrder();

        List bindings = new ArrayList();
        for ( int i = 0; i <= idx; i++ )
        {
            Phase phase = (Phase) phases.get( i );
            List phaseBindings = phase.getBindings();

            if ( phaseBindings != null && !phaseBindings.isEmpty() )
            {
                bindings.addAll( phaseBindings );
            }
        }

        return bindings;
    }

    public static LifecycleBinding findLifecycleBindingsForPhase( String phaseName, LifecycleBindings lifecycles )
        throws NoSuchLifecycleException
    {
        List lifecyclesAvailable = lifecycles.getMappingList();

        for ( Iterator it = lifecyclesAvailable.iterator(); it.hasNext(); )
        {
            LifecycleBinding lifecycle = (LifecycleBinding) it.next();

            if ( lifecycle.getPhaseBindingNamesInOrder().indexOf( phaseName ) > -1 )
            {
                return lifecycle;
            }
        }

        throw new NoSuchLifecycleException( lifecycles.getPackaging(), "Phase not found in any lifecycle: " + phaseName );
    }

    public static void addMojoBinding( String phaseName, MojoBinding mojo, LifecycleBindings bindings )
        throws LifecycleSpecificationException
    {
        LifecycleBinding binding = findLifecycleBindingsForPhase( phaseName, bindings );
        
        List phaseNames = binding.getPhaseBindingNamesInOrder();

        int idx = phaseNames.indexOf( phaseName );

        if ( idx < 0 )
        {
            throw new NoSuchPhaseException( phaseName, "Phase not found in lifecycle: " + binding.getId() );
        }

        List phases = binding.getPhaseBindingsInOrder();
        
        Phase phase = (Phase) phases.get( idx );
        phase.addBinding( mojo );
    }

}
