package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.LifecycleUtils;
import org.apache.maven.lifecycle.MojoBindingUtils;
import org.apache.maven.lifecycle.model.LifecycleBinding;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.statemgmt.StateManagementUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class BuildPlanUtils
{

    private BuildPlanUtils()
    {
    }

    public static List assembleMojoBindingList( List tasks, LifecycleBindings bindings )
        throws LifecycleSpecificationException, LifecyclePlannerException
    {
        return assembleMojoBindingList( tasks, bindings, Collections.EMPTY_MAP );
    }

    public static List assembleMojoBindingList( List tasks, LifecycleBindings lifecycleBindings, Map directInvocationPlans )
        throws LifecycleSpecificationException, LifecyclePlannerException
    {
        List planBindings = new ArrayList();

        List lastMojoBindings = null;
        for ( Iterator it = tasks.iterator(); it.hasNext(); )
        {
            String task = (String) it.next();

            LifecycleBinding binding = LifecycleUtils.findLifecycleBindingForPhase( task, lifecycleBindings );
            if ( binding != null )
            {
                List mojoBindings = LifecycleUtils.getMojoBindingListForLifecycle( task, binding );

                // save these so we can reference the originals...
                List originalMojoBindings = mojoBindings;

                // if these mojo bindings are a superset of the last bindings, only add the difference.
                if ( isSameOrSuperListOfMojoBindings( mojoBindings, lastMojoBindings ) )
                {
                    List revised = new ArrayList( mojoBindings );
                    revised.removeAll( lastMojoBindings );

                    if ( revised.isEmpty() )
                    {
                        continue;
                    }

                    mojoBindings = revised;
                }

                planBindings.addAll( mojoBindings );
                lastMojoBindings = originalMojoBindings;
            }
            else
            {
                MojoBinding mojoBinding = MojoBindingUtils.parseMojoBinding( task, true );
                mojoBinding.setOrigin( "direct invocation" );

                String key = LifecycleUtils.createMojoBindingKey( mojoBinding, true );
                BuildPlan diPlan = (BuildPlan) directInvocationPlans.get( key );

                if ( diPlan != null )
                {
                    planBindings.addAll( diPlan.getPlanMojoBindings() );
                }
                else
                {
                    planBindings.add( mojoBinding );
                }
            }
        }

        return planBindings;
    }

    public static LifecycleBindings modifyPlanBindings( LifecycleBindings bindings, List planModifiers )
        throws LifecyclePlannerException
    {
        LifecycleBindings result;

        // if the bindings are completely empty, passing in null avoids an extra instance creation 
        // for the purposes of cloning...
        if ( bindings != null )
        {
            result = LifecycleUtils.cloneBindings( bindings );
        }
        else
        {
            result = new LifecycleBindings();
        }

        for ( Iterator it = planModifiers.iterator(); it.hasNext(); )
        {
            BuildPlanModifier modifier = (BuildPlanModifier) it.next();

            result = modifier.modifyBindings( result );
        }

        return result;
    }

    public static String listBuildPlan( BuildPlan plan )
        throws LifecycleSpecificationException, LifecyclePlannerException
    {
        List mojoBindings = plan.getPlanMojoBindings();

        return listBuildPlan( mojoBindings );
    }

    public static String listBuildPlan( List mojoBindings )
        throws LifecycleSpecificationException, LifecyclePlannerException
    {
        StringBuffer listing = new StringBuffer();
        int indentLevel = 0;

        for ( Iterator it = mojoBindings.iterator(); it.hasNext(); )
        {
            MojoBinding binding = (MojoBinding) it.next();

            if ( StateManagementUtils.isForkedExecutionStartMarker( binding ) )
            {
                lineAndIndent( listing, indentLevel );
                listing.append( "[fork start] " ).append( formatMojoListing( binding, indentLevel ) );
                
                indentLevel++;
            }
            else if ( StateManagementUtils.isForkedExecutionEndMarker( binding ) )
            {
                indentLevel--;
                
                lineAndIndent( listing, indentLevel );
                listing.append( "[fork end] " ).append( formatMojoListing( binding, indentLevel ) );
            }
            else
            {
                lineAndIndent( listing, indentLevel );
                listing.append( formatMojoListing( binding, indentLevel ) );
            }
        }

        return listing.toString();
    }

    private static void lineAndIndent( StringBuffer listing, int indentLevel )
    {
        listing.append( '\n' );
        
        for ( int i = 0; i < indentLevel; i++ )
        {
            listing.append( "  " );
        }
    }

    public static String formatMojoListing( MojoBinding binding, int indentLevel )
    {
        return MojoBindingUtils.toString( binding ) + " (origin: " + binding.getOrigin() + ")";
    }

    private static boolean isSameOrSuperListOfMojoBindings( List superCandidate, List check )
    {
        if ( superCandidate == null || check == null )
        {
            return false;
        }

        if ( superCandidate.size() < check.size() )
        {
            return false;
        }

        List superKeys = new ArrayList( superCandidate.size() );
        for ( Iterator it = superCandidate.iterator(); it.hasNext(); )
        {
            MojoBinding binding = (MojoBinding) it.next();

            superKeys.add( LifecycleUtils.createMojoBindingKey( binding, true ) );
        }

        List checkKeys = new ArrayList( check.size() );
        for ( Iterator it = check.iterator(); it.hasNext(); )
        {
            MojoBinding binding = (MojoBinding) it.next();

            checkKeys.add( LifecycleUtils.createMojoBindingKey( binding, true ) );
        }

        return superKeys.subList( 0, checkKeys.size() ).equals( checkKeys );
    }

}
