package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.project.MavenProject;

import java.util.List;

public interface BuildPlanner
{

    BuildPlan constructLifecyclePlan( List tasks, MavenProject project )
        throws LifecycleLoaderException, LifecycleSpecificationException, LifecyclePlannerException;
}
