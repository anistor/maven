package org.apache.maven.lifecycle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

public class LifecycleUtilsTest
    extends TestCase
{

    public void testSetOrigin_ShouldSetMojoBindingOrigin()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        binding.setOrigin( "original" );

        LifecycleBindings bindings = new LifecycleBindings();
        bindings.getCleanBinding().getClean().addBinding( binding );

        LifecycleUtils.setOrigin( bindings, "changed" );

        assertEquals( "changed", binding.getOrigin() );
    }

    public void testCreateMojoBindingKey_NoExecId()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        String key = LifecycleUtils.createMojoBindingKey( binding, false );

        assertEquals( "group:artifact:goal", key );
    }

    public void testCreateMojoBindingKey_WithExecId()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        binding.setExecutionId( "execution" );

        String key = LifecycleUtils.createMojoBindingKey( binding, true );

        assertEquals( "group:artifact:goal:execution", key );
    }

    public void testCreateMojoBindingKey_WithPrefixedMojo()
    {
        PrefixedMojoBinding binding = new PrefixedMojoBinding();
        binding.setPrefix( "prefix" );
        binding.setGoal( "goal" );

        String key = LifecycleUtils.createMojoBindingKey( binding, false );

        assertEquals( "prefix:goal", key );
    }

    public void testFindLifecycleBindingForPhase_ShouldFindMojoBindingInPhase()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        LifecycleBindings bindings = new LifecycleBindings();
        bindings.getCleanBinding().getClean().addBinding( binding );

        LifecycleBinding result = LifecycleUtils.findLifecycleBindingForPhase( "clean", bindings );

        assertTrue( result instanceof CleanBinding );

        CleanBinding cb = (CleanBinding) result;
        Phase clean = cb.getClean();

        assertNotNull( clean );
        assertEquals( 1, clean.getBindings().size() );

        MojoBinding resultBinding = (MojoBinding) clean.getBindings().get( 0 );

        assertEquals( "group", resultBinding.getGroupId() );
        assertEquals( "artifact", resultBinding.getArtifactId() );
        assertEquals( "goal", resultBinding.getGoal() );
    }

    public void testFindLifecycleBindingForPhase_ShouldReturnNullForInvalidPhase()
    {
        LifecycleBindings bindings = new LifecycleBindings();

        LifecycleBinding result = LifecycleUtils.findLifecycleBindingForPhase( "dud", bindings );

        assertNull( result );
    }

    public void testFindMatchingMojoBinding_ShouldFindMatchWithoutExecId()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        LifecycleBindings bindings = new LifecycleBindings();

        BuildBinding bb = new BuildBinding();

        Phase phase = new Phase();
        phase.addBinding( binding );

        bb.setCompile( phase );

        bindings.setBuildBinding( bb );

        MojoBinding binding2 = LifecycleUtils.findMatchingMojoBinding( binding, bindings, false );

        assertNotNull( binding2 );
        assertEquals( "goal", binding2.getGoal() );
        assertEquals( "group", binding2.getGroupId() );
        assertEquals( "artifact", binding2.getArtifactId() );
    }

    public void testFindMatchingMojoBinding_ShouldFindMatchWithExecId()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        binding.setExecutionId( "exec" );

        MojoBinding binding2 = newMojoBinding( "group", "artifact", "goal" );
        binding2.setExecutionId( "exec" );

        LifecycleBindings bindings = new LifecycleBindings();

        BuildBinding bb = new BuildBinding();

        Phase phase = new Phase();
        phase.addBinding( binding );

        bb.setCompile( phase );

        bindings.setBuildBinding( bb );

        MojoBinding binding3 = LifecycleUtils.findMatchingMojoBinding( binding2, bindings, true );

        assertNotNull( binding3 );
        assertEquals( "goal", binding3.getGoal() );
        assertEquals( "group", binding3.getGroupId() );
        assertEquals( "artifact", binding3.getArtifactId() );
        assertEquals( "exec", binding3.getExecutionId() );
    }

    public void testFindMatchingMojoBinding_ShouldReturnNullNoMatchWithoutExecId()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        MojoBinding binding2 = newMojoBinding( "group2", "artifact", "goal" );

        LifecycleBindings bindings = new LifecycleBindings();

        BuildBinding bb = new BuildBinding();

        Phase phase = new Phase();
        phase.addBinding( binding );

        bb.setCompile( phase );

        bindings.setBuildBinding( bb );

        MojoBinding binding3 = LifecycleUtils.findMatchingMojoBinding( binding2, bindings, true );

        assertNull( binding3 );
    }

    public void testFindMatchingMojoBinding_ShouldReturnNullWhenExecIdsDontMatch()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        // default executionId == 'default'

        MojoBinding binding2 = newMojoBinding( "group", "artifact", "goal" );
        binding2.setExecutionId( "execution" );

        LifecycleBindings bindings = new LifecycleBindings();

        BuildBinding bb = new BuildBinding();

        Phase phase = new Phase();
        phase.addBinding( binding );

        bb.setCompile( phase );

        bindings.setBuildBinding( bb );

        MojoBinding binding3 = LifecycleUtils.findMatchingMojoBinding( binding2, bindings, true );

        assertNull( binding3 );
    }

    public void testRemoveMojoBinding_ThrowErrorWhenPhaseNotInLifecycleBinding()
    {
        CleanBinding cleanBinding = new CleanBinding();

        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        String phase = "phase";

        try
        {
            LifecycleUtils.removeMojoBinding( phase, binding, cleanBinding, false );

            fail( "Should fail when phase doesn't exist in lifecycle binding." );
        }
        catch ( NoSuchPhaseException e )
        {
            // expected
        }
    }

    public void testRemoveMojoBinding_DoNothingWhenMojoBindingNotInLifecycleBinding()
        throws NoSuchPhaseException
    {
        CleanBinding cleanBinding = new CleanBinding();

        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        MojoBinding binding2 = newMojoBinding( "group", "artifact", "goal2" );

        Phase phase = new Phase();
        phase.addBinding( binding2 );

        cleanBinding.setClean( phase );

        String phaseName = "clean";

        LifecycleUtils.removeMojoBinding( phaseName, binding, cleanBinding, false );

        Phase result = cleanBinding.getClean();
        assertEquals( 1, result.getBindings().size() );
    }

    public void testRemoveMojoBinding_DoNothingWhenMojoPhaseIsNullInLifecycleBinding()
        throws NoSuchPhaseException
    {
        CleanBinding cleanBinding = new CleanBinding();

        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        MojoBinding binding2 = newMojoBinding( "group", "artifact", "goal2" );

        Phase phase = new Phase();
        phase.addBinding( binding2 );

        cleanBinding.setPreClean( phase );

        String phaseName = "clean";

        LifecycleUtils.removeMojoBinding( phaseName, binding, cleanBinding, false );

        Phase result = cleanBinding.getPreClean();
        assertEquals( 1, result.getBindings().size() );
    }

    public void testRemoveMojoBinding_RemoveMojoBindingWhenFoundInLifecycleBinding()
        throws NoSuchPhaseException
    {
        CleanBinding cleanBinding = new CleanBinding();

        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        Phase phase = new Phase();
        phase.addBinding( binding );

        cleanBinding.setClean( phase );

        String phaseName = "clean";

        LifecycleUtils.removeMojoBinding( phaseName, binding, cleanBinding, false );

        Phase result = cleanBinding.getClean();
        assertEquals( 0, result.getBindings().size() );
    }

    public void testCloneMojoBinding_NullVersionIsPropagated()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        MojoBinding binding2 = LifecycleUtils.cloneMojoBinding( binding );

        assertNotNull( binding2 );
        assertEquals( "goal", binding2.getGoal() );
        assertEquals( "group", binding2.getGroupId() );
        assertEquals( "artifact", binding2.getArtifactId() );
        assertNull( binding.getVersion() );
        assertNull( binding2.getVersion() );
    }

    public void testCloneMojoBinding_ExecutionIdIsPropagated()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        binding.setExecutionId( "exec" );

        MojoBinding binding2 = LifecycleUtils.cloneMojoBinding( binding );

        assertNotNull( binding2 );
        assertEquals( "goal", binding2.getGoal() );
        assertEquals( "group", binding2.getGroupId() );
        assertEquals( "artifact", binding2.getArtifactId() );
        assertEquals( "exec", binding2.getExecutionId() );
    }

    public void testCloneMojoBinding_VersionIsPropagated()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        binding.setVersion( "version" );

        MojoBinding binding2 = LifecycleUtils.cloneMojoBinding( binding );

        assertNotNull( binding2 );
        assertEquals( "goal", binding2.getGoal() );
        assertEquals( "group", binding2.getGroupId() );
        assertEquals( "artifact", binding2.getArtifactId() );
        assertEquals( "version", binding2.getVersion() );
        assertEquals( "default", binding2.getExecutionId() );
    }

    public void testAddMojoBinding_LifecycleBinding_AddOneMojoBindingToEmptyLifecycle()
        throws NoSuchPhaseException
    {
        CleanBinding cleanBinding = new CleanBinding();

        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        LifecycleUtils.addMojoBinding( "clean", binding, cleanBinding );

        Phase clean = cleanBinding.getClean();
        assertEquals( 1, clean.getBindings().size() );
    }

    public void testAddMojoBinding_LifecycleBinding_ThrowErrorWhenPhaseDoesntExist()
    {
        CleanBinding cleanBinding = new CleanBinding();

        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        try
        {
            LifecycleUtils.addMojoBinding( "compile", binding, cleanBinding );

            fail( "Should fail because compile phase isn't in the clean lifecycle." );
        }
        catch ( NoSuchPhaseException e )
        {
            // expected
        }
    }

    public void testAddMojoBinding_LifecycleBindings_AddOneMojoBindingToEmptyLifecycle()
        throws LifecycleSpecificationException
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        LifecycleBindings bindings = new LifecycleBindings();

        LifecycleUtils.addMojoBinding( "clean", binding, bindings );

        CleanBinding cleanBinding = bindings.getCleanBinding();
        assertNotNull( cleanBinding );

        Phase clean = cleanBinding.getClean();
        assertNotNull( clean );
        assertEquals( 1, clean.getBindings().size() );
    }

    public void testAddMojoBinding_LifecycleBindings_ThrowErrorWhenPhaseDoesntExist()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        try
        {
            LifecycleUtils.addMojoBinding( "dud", binding, new LifecycleBindings() );

            fail( "Should fail because dud phase isn't in the any lifecycle." );
        }
        catch ( LifecycleSpecificationException e )
        {
            // expected
        }
    }

    public void testGetMojoBindingListForLifecycle_LifecycleBinding_FailWithInvalidStopPhase()
    {
        try
        {
            LifecycleUtils.getMojoBindingListForLifecycle( "dud", new CleanBinding() );

            fail( "Should fail when asked for an invalid phase." );
        }
        catch ( NoSuchPhaseException e )
        {
            // expected
        }
    }

    public void testGetMojoBindingListForLifecycle_LifecycleBinding_RetrieveMojoBindingInStopPhase()
        throws NoSuchPhaseException
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        CleanBinding cleanBinding = new CleanBinding();
        cleanBinding.getClean().addBinding( binding );

        List result = LifecycleUtils.getMojoBindingListForLifecycle( "clean", cleanBinding );

        assertNotNull( result );
        assertEquals( 1, result.size() );

        MojoBinding resultBinding = (MojoBinding) result.get( 0 );

        assertEquals( "group", resultBinding.getGroupId() );
        assertEquals( "artifact", resultBinding.getArtifactId() );
        assertEquals( "goal", resultBinding.getGoal() );
    }

    public void testGetMojoBindingListForLifecycle_LifecycleBinding_RetrieveMojoBindingInPreviousPhase()
        throws NoSuchPhaseException
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        CleanBinding cleanBinding = new CleanBinding();
        cleanBinding.getPreClean().addBinding( binding );

        List result = LifecycleUtils.getMojoBindingListForLifecycle( "clean", cleanBinding );

        assertNotNull( result );
        assertEquals( 1, result.size() );

        MojoBinding resultBinding = (MojoBinding) result.get( 0 );

        assertEquals( "group", resultBinding.getGroupId() );
        assertEquals( "artifact", resultBinding.getArtifactId() );
        assertEquals( "goal", resultBinding.getGoal() );
    }

    public void testGetMojoBindingListForLifecycle_LifecycleBinding_RetrieveTwoMojoBindings()
        throws NoSuchPhaseException
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        MojoBinding binding2 = newMojoBinding( "group", "artifact", "goal2" );

        CleanBinding cleanBinding = new CleanBinding();
        cleanBinding.getClean().addBinding( binding );
        cleanBinding.getPreClean().addBinding( binding2 );

        List result = LifecycleUtils.getMojoBindingListForLifecycle( "clean", cleanBinding );

        assertNotNull( result );
        assertEquals( 2, result.size() );

        MojoBinding resultBinding2 = (MojoBinding) result.get( 0 );

        assertEquals( "group", resultBinding2.getGroupId() );
        assertEquals( "artifact", resultBinding2.getArtifactId() );
        assertEquals( "goal2", resultBinding2.getGoal() );

        MojoBinding resultBinding = (MojoBinding) result.get( 1 );

        assertEquals( "group", resultBinding.getGroupId() );
        assertEquals( "artifact", resultBinding.getArtifactId() );
        assertEquals( "goal", resultBinding.getGoal() );
    }

    public void testGetMojoBindingListForLifecycle_LifecycleBinding_DontRetrieveMojoBindingsInPhaseAfterStopPhase()
        throws NoSuchPhaseException
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        MojoBinding binding2 = newMojoBinding( "group", "artifact", "goal2" );
        MojoBinding binding3 = newMojoBinding( "group", "artifact", "goal3" );

        CleanBinding cleanBinding = new CleanBinding();
        cleanBinding.getClean().addBinding( binding );
        cleanBinding.getPreClean().addBinding( binding2 );
        cleanBinding.getPostClean().addBinding( binding3 );

        List result = LifecycleUtils.getMojoBindingListForLifecycle( "clean", cleanBinding );

        assertNotNull( result );
        assertEquals( 2, result.size() );

        MojoBinding resultBinding2 = (MojoBinding) result.get( 0 );

        assertEquals( "group", resultBinding2.getGroupId() );
        assertEquals( "artifact", resultBinding2.getArtifactId() );
        assertEquals( "goal2", resultBinding2.getGoal() );

        MojoBinding resultBinding = (MojoBinding) result.get( 1 );

        assertEquals( "group", resultBinding.getGroupId() );
        assertEquals( "artifact", resultBinding.getArtifactId() );
        assertEquals( "goal", resultBinding.getGoal() );
    }

    public void testGetMojoBindingListForLifecycle_LifecycleBindings_RetrieveMojoBindingInStopPhase()
        throws NoSuchPhaseException
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        LifecycleBindings bindings = new LifecycleBindings();
        bindings.getCleanBinding().getClean().addBinding( binding );

        List result = LifecycleUtils.getMojoBindingListForLifecycle( "clean", bindings );

        assertNotNull( result );
        assertEquals( 1, result.size() );

        MojoBinding resultBinding = (MojoBinding) result.get( 0 );

        assertEquals( "group", resultBinding.getGroupId() );
        assertEquals( "artifact", resultBinding.getArtifactId() );
        assertEquals( "goal", resultBinding.getGoal() );
    }

    public void testGetMojoBindingListForLifecycle_LifecycleBindings_FailWithInvalidStopPhase()
    {
        try
        {
            LifecycleUtils.getMojoBindingListForLifecycle( "dud", new LifecycleBindings() );

            fail( "Should fail when asked for an invalid phase." );
        }
        catch ( NoSuchPhaseException e )
        {
            // expected
        }
    }

    public void testGetMojoBindingListForLifecycle_LifecycleBindings_RetrieveMojoBindingInPreviousPhase()
        throws NoSuchPhaseException
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        LifecycleBindings bindings = new LifecycleBindings();
        bindings.getCleanBinding().getPreClean().addBinding( binding );

        List result = LifecycleUtils.getMojoBindingListForLifecycle( "clean", bindings );

        assertNotNull( result );
        assertEquals( 1, result.size() );

        MojoBinding resultBinding = (MojoBinding) result.get( 0 );

        assertEquals( "group", resultBinding.getGroupId() );
        assertEquals( "artifact", resultBinding.getArtifactId() );
        assertEquals( "goal", resultBinding.getGoal() );
    }

    public void testGetMojoBindingListForLifecycle_LifecycleBindings_RetrieveTwoMojoBindings()
        throws NoSuchPhaseException
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        MojoBinding binding2 = newMojoBinding( "group", "artifact", "goal2" );

        LifecycleBindings bindings = new LifecycleBindings();
        CleanBinding cleanBinding = bindings.getCleanBinding();
        cleanBinding.getClean().addBinding( binding );
        cleanBinding.getPreClean().addBinding( binding2 );

        List result = LifecycleUtils.getMojoBindingListForLifecycle( "clean", bindings );

        assertNotNull( result );
        assertEquals( 2, result.size() );

        MojoBinding resultBinding2 = (MojoBinding) result.get( 0 );

        assertEquals( "group", resultBinding2.getGroupId() );
        assertEquals( "artifact", resultBinding2.getArtifactId() );
        assertEquals( "goal2", resultBinding2.getGoal() );

        MojoBinding resultBinding = (MojoBinding) result.get( 1 );

        assertEquals( "group", resultBinding.getGroupId() );
        assertEquals( "artifact", resultBinding.getArtifactId() );
        assertEquals( "goal", resultBinding.getGoal() );
    }

    public void testGetMojoBindingListForLifecycle_LifecycleBindings_DontRetrieveMojoBindingsInPhaseAfterStopPhase()
        throws NoSuchPhaseException
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        MojoBinding binding2 = newMojoBinding( "group", "artifact", "goal2" );
        MojoBinding binding3 = newMojoBinding( "group", "artifact", "goal3" );

        LifecycleBindings bindings = new LifecycleBindings();
        CleanBinding cleanBinding = bindings.getCleanBinding();
        cleanBinding.getClean().addBinding( binding );
        cleanBinding.getPreClean().addBinding( binding2 );
        cleanBinding.getPostClean().addBinding( binding3 );

        List result = LifecycleUtils.getMojoBindingListForLifecycle( "clean", bindings );

        assertNotNull( result );
        assertEquals( 2, result.size() );

        MojoBinding resultBinding2 = (MojoBinding) result.get( 0 );

        assertEquals( "group", resultBinding2.getGroupId() );
        assertEquals( "artifact", resultBinding2.getArtifactId() );
        assertEquals( "goal2", resultBinding2.getGoal() );

        MojoBinding resultBinding = (MojoBinding) result.get( 1 );

        assertEquals( "group", resultBinding.getGroupId() );
        assertEquals( "artifact", resultBinding.getArtifactId() );
        assertEquals( "goal", resultBinding.getGoal() );
    }

    public void testIsMojoBindingPresent_ReturnFalseWhenMojoBindingIsMissing_WithoutExecIdCompare()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        assertFalse( LifecycleUtils.isMojoBindingPresent( binding, new ArrayList(), false ) );
    }

    public void testIsMojoBindingPresent_ReturnFalseWhenMojoBindingIsMissing_WithExecIdCompare()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        assertFalse( LifecycleUtils.isMojoBindingPresent( binding, new ArrayList(), true ) );
    }

    public void testIsMojoBindingPresent_ReturnTrueWhenMojoBindingExecIdDoesntMatch_WithoutExecIdCompare()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        binding.setExecutionId( "exec" );

        MojoBinding binding2 = newMojoBinding( "group", "artifact", "goal" );

        List mojos = new ArrayList();
        mojos.add( binding );

        assertTrue( LifecycleUtils.isMojoBindingPresent( binding2, mojos, false ) );
    }

    public void testIsMojoBindingPresent_ReturnFalseWhenMojoBindingExecIdDoesntMatch_WithExecIdCompare()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        binding.setExecutionId( "exec" );

        MojoBinding binding2 = newMojoBinding( "group", "artifact", "goal" );

        List mojos = new ArrayList();
        mojos.add( binding );

        assertFalse( LifecycleUtils.isMojoBindingPresent( binding2, mojos, true ) );
    }

    public void testFindPhaseForMojoBinding_ReturnNullIfBindingNotFound_WithoutExecIdCompare()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        LifecycleBindings bindings = new LifecycleBindings();
        Phase phase = LifecycleUtils.findPhaseForMojoBinding( binding, bindings, false );

        assertNull( phase );
    }

    public void testFindPhaseForMojoBinding_ReturnPhaseContainingBinding_WithoutExecIdCompare()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        LifecycleBindings bindings = new LifecycleBindings();
        Phase cleanPhase = bindings.getCleanBinding().getClean();
        cleanPhase.addBinding( binding );

        Phase phase = LifecycleUtils.findPhaseForMojoBinding( binding, bindings, false );

        assertNotNull( phase );
        assertSame( cleanPhase, phase );
    }

    public void testFindPhaseForMojoBinding_ReturnPhaseContainingSimilarBindingWithOtherExecId_WithoutExecIdCompare()
    {
        MojoBinding binding2 = newMojoBinding( "group", "artifact", "goal" );
        binding2.setExecutionId( "exec" );

        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        LifecycleBindings bindings = new LifecycleBindings();
        Phase cleanPhase = bindings.getCleanBinding().getClean();
        cleanPhase.addBinding( binding );

        Phase phase = LifecycleUtils.findPhaseForMojoBinding( binding2, bindings, false );

        assertNotNull( phase );
        assertSame( cleanPhase, phase );
    }

    public void testFindPhaseForMojoBinding_ReturnNullWhenBindingExecIdsDontMatch_WithExecIdCompare()
    {
        MojoBinding binding2 = newMojoBinding( "group", "artifact", "goal" );
        binding2.setExecutionId( "exec" );

        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        LifecycleBindings bindings = new LifecycleBindings();
        Phase cleanPhase = bindings.getCleanBinding().getClean();
        cleanPhase.addBinding( binding );

        Phase phase = LifecycleUtils.findPhaseForMojoBinding( binding2, bindings, true );

        assertNull( phase );
    }

    public void testAssembleMojoBindingList_ThrowErrorForInvalidPhaseNameAsSingletonTaskList()
    {
        try
        {
            LifecycleUtils.assembleMojoBindingList( Collections.singletonList( "dud" ), new LifecycleBindings() );

            fail( "Should fail with LifecycleSpecificationException due to invalid phase/direct mojo reference." );
        }
        catch ( LifecycleSpecificationException e )
        {
            // expected.
        }
    }

    public void testAssembleMojoBindingList_ReturnBindingWithDirectInvocationOriginWhenSpecifiedInTasks()
        throws LifecycleSpecificationException
    {
        List result = LifecycleUtils.assembleMojoBindingList( Collections.singletonList( "dud:goal" ), new LifecycleBindings() );

        assertEquals( 1, result.size() );

        MojoBinding binding = (MojoBinding) result.get( 0 );

        assertTrue( binding instanceof PrefixedMojoBinding );
        assertEquals( "direct invocation", binding.getOrigin() );
        assertEquals( "dud", ( (PrefixedMojoBinding) binding ).getPrefix() );
        assertEquals( "goal", binding.getGoal() );
    }

    public void testAssembleMojoBindingList_ReturnBindingsUpToStopPhaseForSinglePhaseTaskList()
        throws LifecycleSpecificationException
    {
        LifecycleBindings bindings = new LifecycleBindings();
        bindings.getCleanBinding().getPreClean().addBinding( newMojoBinding( "goal", "artifact", "pre-clean" ) );
        bindings.getCleanBinding().getClean().addBinding( newMojoBinding( "goal", "artifact", "clean" ) );
        bindings.getCleanBinding().getPostClean().addBinding( newMojoBinding( "goal", "artifact", "post-clean" ) );

        List result = LifecycleUtils.assembleMojoBindingList( Collections.singletonList( "clean" ), bindings );

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
        throws LifecycleSpecificationException
    {
        LifecycleBindings bindings = new LifecycleBindings();
        bindings.getCleanBinding().getPreClean().addBinding( newMojoBinding( "goal", "artifact", "pre-clean" ) );
        bindings.getCleanBinding().getClean().addBinding( newMojoBinding( "goal", "artifact", "clean" ) );
        bindings.getCleanBinding().getClean().addBinding( newMojoBinding( "goal", "artifact", "post-clean" ) );

        List tasks = new ArrayList( 2 );
        tasks.add( "clean" );
        tasks.add( "post-clean" );

        List result = LifecycleUtils.assembleMojoBindingList( tasks, bindings );

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
        throws LifecycleSpecificationException
    {
        LifecycleBindings bindings = new LifecycleBindings();
        bindings.getCleanBinding().getPreClean().addBinding( newMojoBinding( "goal", "artifact", "pre-clean" ) );
        bindings.getCleanBinding().getClean().addBinding( newMojoBinding( "goal", "artifact", "clean" ) );

        List tasks = new ArrayList( 2 );
        tasks.add( "clean" );
        tasks.add( "post-clean" );

        List result = LifecycleUtils.assembleMojoBindingList( tasks, bindings );

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
        throws LifecycleSpecificationException
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

        List result = LifecycleUtils.assembleMojoBindingList( tasks, bindings );

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
