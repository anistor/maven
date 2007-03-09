package org.apache.maven.lifecycle;

import junit.framework.TestCase;

public class LifecycleUtilsTest
    extends TestCase
{
    
    public void testCreateMojoBindingKey_NoExecId()
    {
        MojoBinding binding = new MojoBinding();
        binding.setGroupId( "group" );
        binding.setArtifactId( "artifact" );
        binding.setGoal( "goal" );
        
        String key = LifecycleUtils.createMojoBindingKey( binding, false );
        
        assertEquals( "group:artifact:goal", key );
    }

    public void testCreateMojoBindingKey_WithExecId()
    {
        MojoBinding binding = new MojoBinding();
        binding.setGroupId( "group" );
        binding.setArtifactId( "artifact" );
        binding.setGoal( "goal" );
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
        MojoBinding binding = new MojoBinding();
        binding.setGroupId( "group" );
        binding.setArtifactId( "artifact" );
        binding.setGoal( "goal" );
        
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
        MojoBinding binding = new MojoBinding();
        binding.setGroupId( "group" );
        binding.setArtifactId( "artifact" );
        binding.setGoal( "goal" );
        binding.setExecutionId( "exec" );
        
        MojoBinding binding2 = new MojoBinding();
        binding2.setGroupId( "group" );
        binding2.setArtifactId( "artifact" );
        binding2.setGoal( "goal" );
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
        MojoBinding binding = new MojoBinding();
        binding.setGroupId( "group" );
        binding.setArtifactId( "artifact" );
        binding.setGoal( "goal" );
        
        MojoBinding binding2 = new MojoBinding();
        binding.setGroupId( "group2" );
        binding.setArtifactId( "artifact" );
        binding.setGoal( "goal" );
        
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
        MojoBinding binding = new MojoBinding();
        binding.setGroupId( "group" );
        binding.setArtifactId( "artifact" );
        binding.setGoal( "goal" );
        // default executionId == 'default'
        
        MojoBinding binding2 = new MojoBinding();
        binding.setGroupId( "group" );
        binding.setArtifactId( "artifact" );
        binding.setGoal( "goal" );
        binding.setExecutionId( "execution" );
        
        LifecycleBindings bindings = new LifecycleBindings();
        
        BuildBinding bb = new BuildBinding();
        
        Phase phase = new Phase();
        phase.addBinding( binding );
        
        bb.setCompile( phase );
        
        bindings.setBuildBinding( bb );
        
        MojoBinding binding3 = LifecycleUtils.findMatchingMojoBinding( binding2, bindings, true );
        
        assertNull( binding3 );
    }

}
