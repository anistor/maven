package org.apache.maven.lifecycle;

import junit.framework.TestCase;

public class LifecycleUtilsTest
    extends TestCase
{

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
    
    private MojoBinding newMojoBinding( String groupId, String artifactId, String goal )
    {
        MojoBinding binding = new MojoBinding();
        binding.setGroupId( groupId );
        binding.setArtifactId( artifactId );
        binding.setGoal( goal );

        return binding;
    }

}
