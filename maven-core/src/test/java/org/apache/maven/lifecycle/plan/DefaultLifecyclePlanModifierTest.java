package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class DefaultLifecyclePlanModifierTest
    extends TestCase
{

    public void testModifyBindings_AddTwoMojosAfterExistingCompileMojo()
        throws LifecyclePlannerException
    {
        MojoBinding mojo = newMojo( "org.apache.maven.plugins", "maven-compiler-plugin", "compile" );

        List additions = new ArrayList();
        additions.add( newMojo( "group", "artifact", "clean" ) );
        additions.add( newMojo( "group", "artifact", "compile" ) );

        LifecycleBindings target = new LifecycleBindings();
        
        assertEquals( 0, target.getBuildBinding().getCompile().getBindings().size() );
        
        target.getBuildBinding().getCompile().addBinding( mojo );
        
        assertEquals( 1, target.getBuildBinding().getCompile().getBindings().size() );

        target = new DefaultLifecyclePlanModifier( mojo, additions ).modifyBindings( target );

        assertEquals( 3, target.getBuildBinding().getCompile().getBindings().size() );
        
        assertMojo( mojo.getGroupId(), mojo.getArtifactId(), mojo.getGoal(),
                    (MojoBinding) target.getBuildBinding().getCompile().getBindings().get( 0 ) );
        
        assertMojo( "group", "artifact", "clean", (MojoBinding) target.getBuildBinding().getCompile().getBindings().get( 1 ) );
        
        assertMojo( "group", "artifact", "compile", (MojoBinding) target.getBuildBinding().getCompile().getBindings().get( 2 ) );
    }

    public void testModifyBindings_AddTwoMojosBetweenTwoExistingCompileMojos()
        throws LifecyclePlannerException
    {
        MojoBinding mojo = newMojo( "org.apache.maven.plugins", "maven-compiler-plugin", "compile" );
        MojoBinding mojo2 = newMojo( "org.apache.maven.plugins", "maven-compiler-plugin", "compile2" );

        List additions = new ArrayList();
        additions.add( newMojo( "group", "artifact", "clean" ) );
        additions.add( newMojo( "group", "artifact", "compile" ) );

        LifecycleBindings target = new LifecycleBindings();
        
        assertEquals( 0, target.getBuildBinding().getCompile().getBindings().size() );
        
        target.getBuildBinding().getCompile().addBinding( mojo );
        target.getBuildBinding().getCompile().addBinding( mojo2 );
        
        assertEquals( 2, target.getBuildBinding().getCompile().getBindings().size() );

        target = new DefaultLifecyclePlanModifier( mojo, additions ).modifyBindings( target );

        assertEquals( 4, target.getBuildBinding().getCompile().getBindings().size() );
        
        assertMojo( mojo.getGroupId(), mojo.getArtifactId(), mojo.getGoal(),
                    (MojoBinding) target.getBuildBinding().getCompile().getBindings().get( 0 ) );
        
        assertMojo( "group", "artifact", "clean", (MojoBinding) target.getBuildBinding().getCompile().getBindings().get( 1 ) );
        
        assertMojo( "group", "artifact", "compile", (MojoBinding) target.getBuildBinding().getCompile().getBindings().get( 2 ) );
        
        assertMojo( mojo2.getGroupId(), mojo2.getArtifactId(), mojo2.getGoal(),
                    (MojoBinding) target.getBuildBinding().getCompile().getBindings().get( 3 ) );
    }

    public void testModifyBindings_AddTwoNormalPlusTwoModifierModifiedMojosBetweenTwoExistingCompileMojos()
        throws LifecyclePlannerException
    {
        MojoBinding mojo = newMojo( "org.apache.maven.plugins", "maven-compiler-plugin", "compile" );
        MojoBinding mojo2 = newMojo( "org.apache.maven.plugins", "maven-compiler-plugin", "compile2" );

        List modAdditions = new ArrayList();
        modAdditions.add( newMojo( "group2", "artifact", "clean" ) );
        modAdditions.add( newMojo( "group2", "artifact", "compile" ) );

        MojoBinding mojo3 = newMojo( "group", "artifact", "clean" );

        List additions = new ArrayList();
        additions.add( mojo3 );
        additions.add( newMojo( "group", "artifact", "compile" ) );

        LifecycleBindings target = new LifecycleBindings();

        assertEquals( 0, target.getBuildBinding().getCompile().getBindings().size() );
        
        target.getBuildBinding().getCompile().addBinding( mojo );
        target.getBuildBinding().getCompile().addBinding( mojo2 );
        
        assertEquals( 2, target.getBuildBinding().getCompile().getBindings().size() );

        LifecyclePlanModifier modder = new DefaultLifecyclePlanModifier( mojo, additions );
        modder.addModifier( new DefaultLifecyclePlanModifier( mojo3, modAdditions ) );

        target = modder.modifyBindings( target );

        assertEquals( 6, target.getBuildBinding().getCompile().getBindings().size() );
        
        assertMojo( mojo.getGroupId(), mojo.getArtifactId(), mojo.getGoal(),
                    (MojoBinding) target.getBuildBinding().getCompile().getBindings().get( 0 ) );
        
        assertMojo( mojo3.getGroupId(), mojo3.getArtifactId(), mojo3.getGoal(),
                    (MojoBinding) target.getBuildBinding().getCompile().getBindings().get( 1 ) );
        
        assertMojo( "group2", "artifact", "clean", (MojoBinding) target.getBuildBinding().getCompile().getBindings().get( 2 ) );
        
        assertMojo( "group2", "artifact", "compile", (MojoBinding) target.getBuildBinding().getCompile().getBindings().get( 3 ) );
        
        assertMojo( "group", "artifact", "compile", (MojoBinding) target.getBuildBinding().getCompile().getBindings().get( 4 ) );
        
        assertMojo( mojo2.getGroupId(), mojo2.getArtifactId(), mojo2.getGoal(),
                    (MojoBinding) target.getBuildBinding().getCompile().getBindings().get( 5 ) );
    }

    private void assertMojo( String groupId, String artifactId, String goal, MojoBinding binding )
    {
        assertEquals( groupId, binding.getGroupId() );
        assertEquals( artifactId, binding.getArtifactId() );
        assertEquals( goal, binding.getGoal() );
    }

    private MojoBinding newMojo( String groupId, String artifactId, String goal )
    {
        MojoBinding binding = new MojoBinding();
        binding.setGroupId( groupId );
        binding.setArtifactId( artifactId );
        binding.setGoal( goal );

        return binding;
    }
}
