package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

public class DefaultLifecyclePlanTest
    extends TestCase
{

    public void testSingleTask_TwoMojoBindings()
        throws LifecycleSpecificationException, LifecyclePlannerException
    {
        LifecycleBindings bindings = new LifecycleBindings();
        bindings.getCleanBinding().getPreClean().addBinding( newMojo( "group", "artifact", "pre-clean" ) );
        bindings.getCleanBinding().getClean().addBinding( newMojo( "group", "artifact", "clean" ) );

        List plan = new DefaultLifecyclePlan( Collections.singletonList( "clean" ), bindings ).getPlanMojoBindings();

        assertEquals( 2, plan.size() );
        assertMojo( "group", "artifact", "pre-clean", (MojoBinding) plan.get( 0 ) );
        assertMojo( "group", "artifact", "clean", (MojoBinding) plan.get( 1 ) );
    }

    public void testTwoAdditiveTasksInOrder_ThreeMojoBindings_NoDupes()
        throws LifecycleSpecificationException, LifecyclePlannerException
    {
        LifecycleBindings bindings = new LifecycleBindings();
        bindings.getCleanBinding().getPreClean().addBinding( newMojo( "group", "artifact", "pre-clean" ) );
        bindings.getCleanBinding().getClean().addBinding( newMojo( "group", "artifact", "clean" ) );
        bindings.getCleanBinding().getClean().addBinding( newMojo( "group", "artifact", "post-clean" ) );

        List tasks = new ArrayList();
        tasks.add( "clean" );
        tasks.add( "post-clean" );

        List plan = new DefaultLifecyclePlan( tasks, bindings ).getPlanMojoBindings();

        assertEquals( 3, plan.size() );
        assertMojo( "group", "artifact", "pre-clean", (MojoBinding) plan.get( 0 ) );
        assertMojo( "group", "artifact", "clean", (MojoBinding) plan.get( 1 ) );
        assertMojo( "group", "artifact", "post-clean", (MojoBinding) plan.get( 2 ) );
    }

    public void testTwoAdditiveTasksInOrder_TwoMojoBindings_OneMojoModifierInsertedBetween_NoDupes()
        throws LifecycleSpecificationException, LifecyclePlannerException
    {
        MojoBinding preClean = newMojo( "group", "artifact", "pre-clean" );

        List mods = Collections.singletonList( newMojo( "group", "artifact", "clean" ) );

        LifecyclePlanModifier modder = new DefaultLifecyclePlanModifier( preClean, mods );

        LifecycleBindings bindings = new LifecycleBindings();
        bindings.getCleanBinding().getPreClean().addBinding( preClean );
        bindings.getCleanBinding().getClean().addBinding( newMojo( "group", "artifact", "post-clean" ) );

        List tasks = new ArrayList();
        tasks.add( "clean" );
        tasks.add( "post-clean" );

        LifecyclePlan lifecyclePlan = new DefaultLifecyclePlan( tasks, bindings );
        lifecyclePlan.addModifier( modder );
        
        List plan = lifecyclePlan.getPlanMojoBindings();

        assertEquals( 3, plan.size() );
        assertMojo( "group", "artifact", "pre-clean", (MojoBinding) plan.get( 0 ) );
        assertMojo( "group", "artifact", "clean", (MojoBinding) plan.get( 1 ) );
        assertMojo( "group", "artifact", "post-clean", (MojoBinding) plan.get( 2 ) );
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
