package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;

import junit.framework.TestCase;

public class LifecycleOverlayPlanModifierTest
    extends TestCase
{

    public void testModifyEmptyLifecycleBindings_AddTwoMojos()
        throws LifecyclePlannerException
    {
        LifecycleBindings overlay = new LifecycleBindings();
        overlay.getCleanBinding().getClean().addBinding( newMojo( "group", "artifact", "clean" ) );
        overlay.getBuildBinding().getCompile().addBinding( newMojo( "group", "artifact", "compile" ) );

        LifecycleBindings target = new LifecycleBindings();
        assertEquals( 0, target.getCleanBinding().getClean().getBindings().size() );
        assertEquals( 0, target.getBuildBinding().getCompile().getBindings().size() );

        target = new LifecycleOverlayPlanModifier( overlay ).modifyBindings( target );

        assertEquals( 1, target.getCleanBinding().getClean().getBindings().size() );
        assertMojo( "group", "artifact", "clean", (MojoBinding) target.getCleanBinding().getClean().getBindings().get( 0 ) );

        assertEquals( 1, target.getBuildBinding().getCompile().getBindings().size() );
        assertMojo( "group", "artifact", "compile", (MojoBinding) target.getBuildBinding().getCompile().getBindings().get( 0 ) );
    }

    public void testModifyExistingLifecycleBindings_AddTwoMojosToExistingTwoMojosInSamePhases()
        throws LifecyclePlannerException
    {
        LifecycleBindings overlay = new LifecycleBindings();
        overlay.getCleanBinding().getClean().addBinding( newMojo( "group", "artifact", "clean" ) );
        overlay.getBuildBinding().getCompile().addBinding( newMojo( "group", "artifact", "compile" ) );

        LifecycleBindings target = new LifecycleBindings();
        target.getCleanBinding().getClean().addBinding( newMojo( "org.apache.maven.plugins", "maven-clean-plugin", "clean" ) );
        target.getBuildBinding().getCompile().addBinding(
                                                          newMojo( "org.apache.maven.plugins", "maven-compiler-plugin", "compile" ) );

        assertEquals( 1, target.getCleanBinding().getClean().getBindings().size() );
        assertEquals( 1, target.getBuildBinding().getCompile().getBindings().size() );

        target = new LifecycleOverlayPlanModifier( overlay ).modifyBindings( target );

        assertEquals( 2, target.getCleanBinding().getClean().getBindings().size() );
        assertMojo( "org.apache.maven.plugins", "maven-clean-plugin", "clean",
                    (MojoBinding) target.getCleanBinding().getClean().getBindings().get( 0 ) );
        assertMojo( "group", "artifact", "clean", (MojoBinding) target.getCleanBinding().getClean().getBindings().get( 1 ) );

        assertEquals( 2, target.getBuildBinding().getCompile().getBindings().size() );
        assertMojo( "org.apache.maven.plugins", "maven-compiler-plugin", "compile",
                    (MojoBinding) target.getBuildBinding().getCompile().getBindings().get( 0 ) );
        assertMojo( "group", "artifact", "compile", (MojoBinding) target.getBuildBinding().getCompile().getBindings().get( 1 ) );
    }

    public void testModifyExistingLifecycleBindings_AddTwoMojosDirectlyAndTwoViaModifiersModifier()
        throws LifecyclePlannerException
    {
        LifecycleBindings modifier = new LifecycleBindings();
        modifier.getCleanBinding().getClean().addBinding( newMojo( "group2", "artifact", "clean" ) );
        modifier.getBuildBinding().getCompile().addBinding( newMojo( "group2", "artifact", "compile" ) );

        LifecycleBindings overlay = new LifecycleBindings();
        overlay.getCleanBinding().getClean().addBinding( newMojo( "group", "artifact", "clean" ) );
        overlay.getBuildBinding().getCompile().addBinding( newMojo( "group", "artifact", "compile" ) );

        LifecycleBindings target = new LifecycleBindings();
        target.getCleanBinding().getClean().addBinding( newMojo( "org.apache.maven.plugins", "maven-clean-plugin", "clean" ) );
        target.getBuildBinding().getCompile().addBinding(
                                                          newMojo( "org.apache.maven.plugins", "maven-compiler-plugin", "compile" ) );

        assertEquals( 1, target.getCleanBinding().getClean().getBindings().size() );
        assertEquals( 1, target.getBuildBinding().getCompile().getBindings().size() );

        LifecyclePlanModifier modder = new LifecycleOverlayPlanModifier( overlay );
        modder.addModifier( new LifecycleOverlayPlanModifier( modifier ) );
        
        target = modder.modifyBindings( target );

        assertEquals( 3, target.getCleanBinding().getClean().getBindings().size() );
        assertMojo( "org.apache.maven.plugins", "maven-clean-plugin", "clean",
                    (MojoBinding) target.getCleanBinding().getClean().getBindings().get( 0 ) );
        assertMojo( "group", "artifact", "clean", (MojoBinding) target.getCleanBinding().getClean().getBindings().get( 1 ) );
        assertMojo( "group2", "artifact", "clean", (MojoBinding) target.getCleanBinding().getClean().getBindings().get( 2 ) );

        assertEquals( 3, target.getBuildBinding().getCompile().getBindings().size() );
        assertMojo( "org.apache.maven.plugins", "maven-compiler-plugin", "compile",
                    (MojoBinding) target.getBuildBinding().getCompile().getBindings().get( 0 ) );
        assertMojo( "group", "artifact", "compile", (MojoBinding) target.getBuildBinding().getCompile().getBindings().get( 1 ) );
        assertMojo( "group2", "artifact", "compile", (MojoBinding) target.getBuildBinding().getCompile().getBindings().get( 2 ) );
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
