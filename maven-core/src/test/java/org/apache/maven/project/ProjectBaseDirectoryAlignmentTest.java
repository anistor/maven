package org.apache.maven.project;

import org.apache.maven.model.Build;
import org.apache.maven.model.Resource;

import java.io.File;

public class ProjectBaseDirectoryAlignmentTest
    extends AbstractProjectTestCase
{        
    
    private String dir = "src/test/resources/projects/base-directory-alignment/";
        
    public void testProjectDirectoryBaseDirectoryAlignment()
        throws Exception
    {
        File f = getTestFile( dir + "project-which-needs-directory-alignment.xml" );
        
        MavenProject project = projectBuilder.build( getMavenLocalHome(), f, false );

        assertNotNull( "Test project can't be null!", project );

        assertTrue( project.getBuild().getSourceDirectory().startsWith( getBasedir() ) );

        assertTrue( project.getBuild().getUnitTestSourceDirectory().startsWith( getBasedir() ) );

        Build build = project.getBuild();

        Resource resource = (Resource) build.getResources().get( 0 );

        assertTrue( resource.getDirectory().startsWith( getBasedir() ) );
    }

    /* TODO: why commented out? Gives a Wagonhttp warning and can't find parent POM
    public void testProjectDirectoryBaseDirectoryAlignmentInheritance()
        throws Exception
    {
        File f = new File( basedir, dir + "project-which-needs-directory-alignment-child.xml" );

        MavenProject project = projectBuilder.build( f, false );

        assertNotNull( "Test project can't be null!", project );

        assertTrue( project.getBuild().getSourceDirectory().startsWith( basedir ) );

        assertTrue( project.getBuild().getUnitTestSourceDirectory().startsWith( basedir ) );

        Build build = project.getBuild();

        Resource resource = (Resource) build.getResources().get( 0 );

        assertTrue( resource.getDirectory().startsWith( basedir ) );
    }

    public void testProjectDirectoryBaseDirectoryAlignmentInheritanceWithParentOneDirectoryUp()
        throws Exception
    {
        File f = new File( basedir, dir + "subproject/project-which-needs-directory-alignment-child.xml" );

        MavenProject project = projectBuilder.build( f, false );

        assertNotNull( "Test project can't be null!", project );

        assertTrue( project.getBuild().getSourceDirectory().startsWith( basedir ) );

        assertTrue( project.getBuild().getUnitTestSourceDirectory().startsWith( basedir ) );

        Build build = project.getBuild();

        Resource resource = (Resource) build.getResources().get( 0 );

        // We should not be picking up the parents values here.

        assertTrue( resource.getDirectory().indexOf( ".." ) < 0 );
    }
    */
}
