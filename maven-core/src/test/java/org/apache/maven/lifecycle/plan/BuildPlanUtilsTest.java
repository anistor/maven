package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.PrefixedMojoBinding;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

public class BuildPlanUtilsTest
    extends TestCase
{

    public void testAssembleMojoBindingList_ThrowErrorForInvalidPhaseNameAsSingletonTaskList()
        throws LifecyclePlannerException
    {
        try
        {
            BuildPlanUtils.assembleMojoBindingList( Collections.singletonList( "dud" ), new LifecycleBindings() );

            fail( "Should fail with LifecycleSpecificationException due to invalid phase/direct mojo reference." );
        }
        catch ( LifecycleSpecificationException e )
        {
            // expected.
        }
    }

    public void testAssembleMojoBindingList_ReturnBindingWithDirectInvocationOriginWhenSpecifiedInTasks()
        throws LifecycleSpecificationException, LifecyclePlannerException
    {
        List result = BuildPlanUtils.assembleMojoBindingList( Collections.singletonList( "dud:goal" ), new LifecycleBindings() );

        assertEquals( 1, result.size() );

        MojoBinding binding = (MojoBinding) result.get( 0 );

        assertTrue( binding instanceof PrefixedMojoBinding );
        assertEquals( "direct invocation", binding.getOrigin() );
        assertEquals( "dud", ( (PrefixedMojoBinding) binding ).getPrefix() );
        assertEquals( "goal", binding.getGoal() );
    }

    public void testAssembleMojoBindingList_ReturnBindingsUpToStopPhaseForSinglePhaseTaskList()
        throws LifecycleSpecificationException, LifecyclePlannerException
    {
        LifecycleBindings bindings = new LifecycleBindings();
        bindings.getCleanBinding().getPreClean().addBinding( newMojoBinding( "goal", "artifact", "pre-clean" ) );
        bindings.getCleanBinding().getClean().addBinding( newMojoBinding( "goal", "artifact", "clean" ) );
        bindings.getCleanBinding().getPostClean().addBinding( newMojoBinding( "goal", "artifact", "post-clean" ) );

        List result = BuildPlanUtils.assembleMojoBindingList( Collections.singletonList( "clean" ), bindings );

        assertEquals( 2, result.size() );

        MojoBinding binding = (MojoBinding) result.get( 0 );

        assertEquals( "goal", binding.getGroupId() );
        assertEquals( "artifact", binding.getArtifactId() );
        assertEquals( "pre-clean", binding.getGoal() );

        binding = (MojoBinding) result.get( 1 );

        assertEquals( "goal", binding.getGroupId() );
        assertEquals( "artifact", binding.getArtifactId() );
        assertEquals( "clean", binding.getGoal() );
    }

    public void testAssembleMojoBindingList_CombinePreviousBindingsWhenSubsetOfNextBindingsForTwoPhaseTaskList()
        throws LifecycleSpecificationException, LifecyclePlannerException
    {
        LifecycleBindings bindings = new LifecycleBindings();
        bindings.getCleanBinding().getPreClean().addBinding( newMojoBinding( "goal", "artifact", "pre-clean" ) );
        bindings.getCleanBinding().getClean().addBinding( newMojoBinding( "goal", "artifact", "clean" ) );
        bindings.getCleanBinding().getClean().addBinding( newMojoBinding( "goal", "artifact", "post-clean" ) );

        List tasks = new ArrayList( 2 );
        tasks.add( "clean" );
        tasks.add( "post-clean" );

        List result = BuildPlanUtils.assembleMojoBindingList( tasks, bindings );

        assertEquals( 3, result.size() );

        MojoBinding binding = (MojoBinding) result.get( 0 );

        assertEquals( "goal", binding.getGroupId() );
        assertEquals( "artifact", binding.getArtifactId() );
        assertEquals( "pre-clean", binding.getGoal() );

        binding = (MojoBinding) result.get( 1 );

        assertEquals( "goal", binding.getGroupId() );
        assertEquals( "artifact", binding.getArtifactId() );
        assertEquals( "clean", binding.getGoal() );

        binding = (MojoBinding) result.get( 2 );

        assertEquals( "goal", binding.getGroupId() );
        assertEquals( "artifact", binding.getArtifactId() );
        assertEquals( "post-clean", binding.getGoal() );
    }

    public void testAssembleMojoBindingList_IgnoreSuccessiveBindingsWhenSameAsPreviousOnesForTwoPhaseTaskList()
        throws LifecycleSpecificationException, LifecyclePlannerException
    {
        LifecycleBindings bindings = new LifecycleBindings();
        bindings.getCleanBinding().getPreClean().addBinding( newMojoBinding( "goal", "artifact", "pre-clean" ) );
        bindings.getCleanBinding().getClean().addBinding( newMojoBinding( "goal", "artifact", "clean" ) );

        List tasks = new ArrayList( 2 );
        tasks.add( "clean" );
        tasks.add( "post-clean" );

        List result = BuildPlanUtils.assembleMojoBindingList( tasks, bindings );

        assertEquals( 2, result.size() );

        MojoBinding binding = (MojoBinding) result.get( 0 );

        assertEquals( "goal", binding.getGroupId() );
        assertEquals( "artifact", binding.getArtifactId() );
        assertEquals( "pre-clean", binding.getGoal() );

        binding = (MojoBinding) result.get( 1 );

        assertEquals( "goal", binding.getGroupId() );
        assertEquals( "artifact", binding.getArtifactId() );
        assertEquals( "clean", binding.getGoal() );
    }

    public void testAssembleMojoBindingList_ReturnBindingsUpToStopPhasesForTwoPhaseTaskList()
        throws LifecycleSpecificationException, LifecyclePlannerException
    {
        LifecycleBindings bindings = new LifecycleBindings();

        bindings.getCleanBinding().getPreClean().addBinding( newMojoBinding( "goal", "artifact", "pre-clean" ) );
        bindings.getCleanBinding().getClean().addBinding( newMojoBinding( "goal", "artifact", "clean" ) );
        bindings.getCleanBinding().getPostClean().addBinding( newMojoBinding( "goal", "artifact", "post-clean" ) );

        bindings.getBuildBinding().getInitialize().addBinding( newMojoBinding( "goal", "artifact", "initialize" ) );
        bindings.getBuildBinding().getCompile().addBinding( newMojoBinding( "goal", "artifact", "compile" ) );
        bindings.getBuildBinding().getCreatePackage().addBinding( newMojoBinding( "goal", "artifact", "package" ) );

        List tasks = new ArrayList( 2 );
        tasks.add( "clean" );
        tasks.add( "compile" );

        List result = BuildPlanUtils.assembleMojoBindingList( tasks, bindings );

        assertEquals( 4, result.size() );

        MojoBinding binding = (MojoBinding) result.get( 0 );

        assertEquals( "goal", binding.getGroupId() );
        assertEquals( "artifact", binding.getArtifactId() );
        assertEquals( "pre-clean", binding.getGoal() );

        binding = (MojoBinding) result.get( 1 );

        assertEquals( "goal", binding.getGroupId() );
        assertEquals( "artifact", binding.getArtifactId() );
        assertEquals( "clean", binding.getGoal() );

        binding = (MojoBinding) result.get( 2 );

        assertEquals( "goal", binding.getGroupId() );
        assertEquals( "artifact", binding.getArtifactId() );
        assertEquals( "initialize", binding.getGoal() );

        binding = (MojoBinding) result.get( 3 );

        assertEquals( "goal", binding.getGroupId() );
        assertEquals( "artifact", binding.getArtifactId() );
        assertEquals( "compile", binding.getGoal() );

    }

    private MojoBinding newMojoBinding( String groupId, String artifactId, String goal )
    {
        MojoBinding binding = new MojoBinding();
        binding.setGroupId( groupId );
        binding.setArtifactId( artifactId );
        binding.setGoal( goal );

        return binding;
    }
}
