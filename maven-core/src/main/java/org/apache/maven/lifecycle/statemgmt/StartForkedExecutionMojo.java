package org.apache.maven.lifecycle.statemgmt;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

public class StartForkedExecutionMojo
    extends AbstractMojo
{
    
    private MavenProject project;
    
    private int forkId = -1;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        project.setExecutionProject( new MavenProject( project ) );
        getLog().info( "Starting forked execution [fork id: " + forkId + "]" );
    }

}
