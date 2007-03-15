package org.apache.maven.lifecycle.statemgmt;

import org.apache.maven.lifecycle.model.MojoBinding;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public final class StateManagementUtils
{

    public static final String GROUP_ID = "org.apache.maven.plugins.internal";

    public static final String ARTIFACT_ID = "maven-state-management";

    public static final String ORIGIN = "Maven build-state management";

    public static final String END_FORKED_EXECUTION_GOAL = "end-fork";

    public static final String START_FORKED_EXECUTION_GOAL = "start-fork";

    public static final String VERSION = "2.1";

    public static final String CLEAR_FORKED_EXECUTION_GOAL = "clear-fork-context";

    private static int CURRENT_FORK_ID = 0;

    private StateManagementUtils()
    {
    }

    public static MojoBinding createStartForkedExecutionMojoBinding()
    {
        MojoBinding binding = new MojoBinding();

        binding.setGroupId( GROUP_ID );
        binding.setArtifactId( ARTIFACT_ID );
        binding.setVersion( VERSION );
        binding.setGoal( START_FORKED_EXECUTION_GOAL );
        binding.setOrigin( ORIGIN );

        CURRENT_FORK_ID = (int) System.currentTimeMillis();
        
        Xpp3Dom config = new Xpp3Dom( "configuration" );
        Xpp3Dom forkId = new Xpp3Dom( "forkId" );
        forkId.setValue( "" + CURRENT_FORK_ID );
        
        config.addChild( forkId );
        
        binding.setConfiguration( config );

        return binding;
    }

    public static MojoBinding createEndForkedExecutionMojoBinding()
    {
        MojoBinding binding = new MojoBinding();

        binding.setGroupId( GROUP_ID );
        binding.setArtifactId( ARTIFACT_ID );
        binding.setVersion( VERSION );
        binding.setGoal( END_FORKED_EXECUTION_GOAL );
        binding.setOrigin( ORIGIN );

        Xpp3Dom config = new Xpp3Dom( "configuration" );
        Xpp3Dom forkId = new Xpp3Dom( "forkId" );
        forkId.setValue( "" + CURRENT_FORK_ID );
        
        config.addChild( forkId );
        
        binding.setConfiguration( config );

        return binding;
    }

    public static MojoBinding createClearForkedExecutionMojoBinding()
    {
        MojoBinding binding = new MojoBinding();

        binding.setGroupId( GROUP_ID );
        binding.setArtifactId( ARTIFACT_ID );
        binding.setVersion( VERSION );
        binding.setGoal( CLEAR_FORKED_EXECUTION_GOAL );
        binding.setOrigin( ORIGIN );

        Xpp3Dom config = new Xpp3Dom( "configuration" );
        Xpp3Dom forkId = new Xpp3Dom( "forkId" );
        forkId.setValue( "" + CURRENT_FORK_ID );
        
        config.addChild( forkId );
        
        binding.setConfiguration( config );

        return binding;
    }
    
    public static boolean isForkedExecutionStartMarker( MojoBinding binding )
    {
        return GROUP_ID.equals( binding.getGroupId() ) && ARTIFACT_ID.equals( binding.getArtifactId() )
            && START_FORKED_EXECUTION_GOAL.equals( binding.getGoal() );
    }

    public static boolean isForkedExecutionEndMarker( MojoBinding binding )
    {
        return GROUP_ID.equals( binding.getGroupId() ) && ARTIFACT_ID.equals( binding.getArtifactId() )
            && END_FORKED_EXECUTION_GOAL.equals( binding.getGoal() );
    }

    public static boolean isForkedExecutionClearMarker( MojoBinding binding )
    {
        return GROUP_ID.equals( binding.getGroupId() ) && ARTIFACT_ID.equals( binding.getArtifactId() )
            && CLEAR_FORKED_EXECUTION_GOAL.equals( binding.getGoal() );
    }

}
