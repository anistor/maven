package org.apache.maven.lifecycle.statemgmt;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

public class EndForkedExecutionMojo
    extends AbstractMojo
{
    
    private MavenProject project;
    
    private int forkId = -1;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        project.clearExecutionProject();
        getLog().info( "Ending forked execution [fork id: " + forkId + "]" );
    }

}
