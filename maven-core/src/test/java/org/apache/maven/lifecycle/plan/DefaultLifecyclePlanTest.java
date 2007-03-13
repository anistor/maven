package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.statemgmt.StateManagementUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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

        List plan = new LifecycleBuildPlan( Collections.singletonList( "clean" ), bindings ).getPlanMojoBindings();

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

        List plan = new LifecycleBuildPlan( tasks, bindings ).getPlanMojoBindings();

        assertEquals( 3, plan.size() );
        assertMojo( "group", "artifact", "pre-clean", (MojoBinding) plan.get( 0 ) );
        assertMojo( "group", "artifact", "clean", (MojoBinding) plan.get( 1 ) );
        assertMojo( "group", "artifact", "post-clean", (MojoBinding) plan.get( 2 ) );
    }

    public void testTwoAdditiveTasksInOrder_TwoMojoBindings_OneMojoModifierInsertedBetween_NoDupes()
        throws LifecycleSpecificationException, LifecyclePlannerException
    {
        MojoBinding clean = newMojo( "group", "artifact", "clean" );

        List mods = Collections.singletonList( newMojo( "group", "artifact", "pre-clean" ) );

        BuildPlanModifier modder = new ForkPlanModifier( clean, mods );

        LifecycleBindings bindings = new LifecycleBindings();
        bindings.getCleanBinding().getPreClean().addBinding( clean );
        bindings.getCleanBinding().getClean().addBinding( newMojo( "group", "artifact", "post-clean" ) );

        List tasks = new ArrayList();
        tasks.add( "clean" );
        tasks.add( "post-clean" );

        BuildPlan lifecyclePlan = new LifecycleBuildPlan( tasks, bindings );
        lifecyclePlan.addModifier( modder );
        
        List plan = lifecyclePlan.getPlanMojoBindings();

        assertEquals( 5, plan.size() );
        Iterator it = plan.iterator();
        
        assertMojo( StateManagementUtils.GROUP_ID, StateManagementUtils.ARTIFACT_ID,
                    StateManagementUtils.START_FORKED_EXECUTION_GOAL, (MojoBinding) it.next() );
        
        assertMojo( "group", "artifact", "pre-clean", (MojoBinding) it.next() );
        assertMojo( "group", "artifact", "clean", (MojoBinding) it.next() );
        
        assertMojo( StateManagementUtils.GROUP_ID, StateManagementUtils.ARTIFACT_ID,
                    StateManagementUtils.END_FORKED_EXECUTION_GOAL, (MojoBinding) it.next() );
        
        assertMojo( "group", "artifact", "post-clean", (MojoBinding) it.next() );
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
