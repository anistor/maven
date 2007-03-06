package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.lifecycle.LifecycleExecutionException;

import java.util.List;

public interface LifecyclePlanner
{

    List constructExecutionPlan()
        throws LifecycleExecutionException;

    Lifecycle getLifecycleForPhase( String task )
        throws LifecycleExecutionException;

    boolean isLifecyclePhase( String task )
        throws LifecycleExecutionException;

}
