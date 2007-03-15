package org.apache.maven.lifecycle;

import org.apache.maven.context.BuildContext;
import org.apache.maven.context.BuildContextManager;
import org.apache.maven.context.ManagedBuildData;
import org.apache.maven.project.MavenProject;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class LifecycleExecutionContext
    implements ManagedBuildData
{
    
    public static final String BUILD_CONTEXT_KEY = LifecycleExecutionContext.class.getName();
    
    private static final String CURRENT_PROJECT_KEY = "current-project";
    private static final String PROJECT_STACK_KEY = "fork-project-stack";
    
    private MavenProject currentProject;
    private Stack forkedProjectStack = new Stack();
    
    public LifecycleExecutionContext( MavenProject project )
    {
        this.currentProject = project;
    }
    
    private LifecycleExecutionContext()
    {
        // used for retrieval.
    }

    public Map getData()
    {
        Map data = new HashMap();
        data.put( CURRENT_PROJECT_KEY, currentProject );
        data.put( PROJECT_STACK_KEY, forkedProjectStack );
        
        return data;
    }

    public String getStorageKey()
    {
        return BUILD_CONTEXT_KEY;
    }

    public void setData( Map data )
    {
        this.currentProject = (MavenProject) data.get( CURRENT_PROJECT_KEY );
        this.forkedProjectStack = (Stack) data.get( PROJECT_STACK_KEY );
    }
    
    public void addForkedProject( MavenProject project )
    {
        forkedProjectStack.push( project );
    }
    
    public MavenProject removeForkedProject()
    {
        if ( !forkedProjectStack.isEmpty() )
        {
            MavenProject lastCurrent = currentProject;
            currentProject = (MavenProject) forkedProjectStack.pop();
            
            return lastCurrent;
        }
        
        return null;
    }
    
    public MavenProject getCurrentProject()
    {
        return currentProject;
    }
    
    public static LifecycleExecutionContext read( BuildContextManager buildContextManager )
    {
        BuildContext buildContext = buildContextManager.readBuildContext( true );
        
        LifecycleExecutionContext ctx = new LifecycleExecutionContext();
        if ( buildContext.retrieve( ctx ) )
        {
            return ctx;
        }
        
        return null;
    }
    
    public static void delete( BuildContextManager buildContextManager )
    {
        BuildContext buildContext = buildContextManager.readBuildContext( false );
        
        if ( buildContext != null )
        {
            buildContext.delete( BUILD_CONTEXT_KEY );
        }
    }
    
    public void store( BuildContextManager buildContextManager )
    {
        BuildContext buildContext = buildContextManager.readBuildContext( true );
        buildContext.store( this );
        buildContextManager.storeBuildContext( buildContext );
    }

}
