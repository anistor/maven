package org.apache.maven.lifecycle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LifecycleUtils
{

    private LifecycleUtils()
    {
    }

    public static void setOrigin( LifecycleBindings bindings, String origin )
    {
        for ( Iterator bindingIt = bindings.getBindingList().iterator(); bindingIt.hasNext(); )
        {
            LifecycleBinding binding = (LifecycleBinding) bindingIt.next();

            for ( Iterator phaseIt = binding.getPhasesInOrder().iterator(); phaseIt.hasNext(); )
            {
                Phase phase = (Phase) phaseIt.next();

                for ( Iterator mojoIt = phase.getBindings().iterator(); mojoIt.hasNext(); )
                {
                    MojoBinding mojoBinding = (MojoBinding) mojoIt.next();

                    mojoBinding.setOrigin( origin );
                }
            }
        }
    }

    public static List getMojoBindings( String phaseName, LifecycleBinding lifecycle )
        throws NoSuchPhaseException
    {
        List phaseNames = lifecycle.getPhaseNamesInOrder();

        int idx = phaseNames.indexOf( phaseName );

        if ( idx < 0 )
        {
            throw new NoSuchPhaseException( phaseName, "Phase not found in lifecycle: " + lifecycle.getId() );
        }

        List phases = lifecycle.getPhasesInOrder();

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

    /**
     * @return null if the phase is not contained in any of the lifecycles.
     */
    public static LifecycleBinding findLifecycleBindingsForPhase( String phaseName, LifecycleBindings lifecycles )
    {
        List lifecyclesAvailable = lifecycles.getBindingList();

        for ( Iterator it = lifecyclesAvailable.iterator(); it.hasNext(); )
        {
            LifecycleBinding lifecycle = (LifecycleBinding) it.next();

            if ( lifecycle.getPhaseNamesInOrder().indexOf( phaseName ) > -1 )
            {
                return lifecycle;
            }
        }
        
        return null;
    }

    public static void addMojoBinding( String phaseName, MojoBinding mojo, LifecycleBindings bindings )
        throws LifecycleSpecificationException
    {
        LifecycleBinding binding = findLifecycleBindingsForPhase( phaseName, bindings );

        List phaseNames = binding.getPhaseNamesInOrder();

        int idx = phaseNames.indexOf( phaseName );

        if ( idx < 0 )
        {
            throw new NoSuchPhaseException( phaseName, "Phase not found in lifecycle: " + binding.getId() );
        }

        List phases = binding.getPhasesInOrder();

        Phase phase = (Phase) phases.get( idx );
        phase.addBinding( mojo );
    }

    public static LifecycleBindings mergeBindings( LifecycleBindings packagingBindings, LifecycleBindings projectBindings,
                                                   LifecycleBindings defaultBindings )
    {
        LifecycleBindings result = new LifecycleBindings();
        result.setPackaging( projectBindings.getPackaging() );

        CleanBinding cb = packagingBindings.getCleanBinding();
        if ( cb == null )
        {
            cb = defaultBindings.getCleanBinding();
        }

        if ( cb == null )
        {
            cb = new CleanBinding();
        }

        result.setCleanBinding( cb );

        BuildBinding bb = packagingBindings.getBuildBinding();
        if ( bb == null )
        {
            bb = defaultBindings.getBuildBinding();
        }

        if ( bb == null )
        {
            bb = new BuildBinding();
        }

        result.setBuildBinding( bb );

        SiteBinding sb = packagingBindings.getSiteBinding();
        if ( sb == null )
        {
            sb = defaultBindings.getSiteBinding();
        }

        if ( sb == null )
        {
            sb = new SiteBinding();
        }

        result.setSiteBinding( sb );

        for ( Iterator bindingIt = projectBindings.getBindingList().iterator(); bindingIt.hasNext(); )
        {
            LifecycleBinding lifecycleBinding = (LifecycleBinding) bindingIt.next();

            if ( lifecycleBinding != null )
            {
                List phaseNames = lifecycleBinding.getPhaseNamesInOrder();
                List phases = lifecycleBinding.getPhasesInOrder();

                for ( int i = 0; i < phases.size(); i++ )
                {
                    Phase phase = (Phase) phases.get( i );
                    String name = (String) phaseNames.get( i );

                    if ( phase != null && phase.getBindings() != null && !phase.getBindings().isEmpty() )
                    {
                        for ( Iterator phaseIt = phase.getBindings().iterator(); phaseIt.hasNext(); )
                        {
                            MojoBinding mojoBinding = (MojoBinding) phaseIt.next();

                            try
                            {
                                addMojoBinding( name, mojoBinding, result );
                            }
                            catch ( LifecycleSpecificationException e )
                            {
                                // NOTE: this shouldn't happen as long as normal components are used
                                // to create/read these LifecycleBindings instances.
                                IllegalArgumentException error = new IllegalArgumentException(
                                                                                               "Project bindings are invalid. Reason: "
                                                                                                   + e.getMessage() );
                                
                                error.initCause( e );

                                throw error;
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    public static void removeBindings( List toRemove, List removeFrom )
    {
        // remove where g:a:v:goal matches.
    }

}
