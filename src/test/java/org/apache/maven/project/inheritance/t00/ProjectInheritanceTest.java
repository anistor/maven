package org.apache.maven.project.inheritance.t00;

import org.apache.maven.model.MailingList;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.inheritance.ProjectInheritanceTestCase;

/**
 * A test which demonstrates maven's recursive inheritance where
 * a distinct value is taken from each parent contributing to the
 * the final model of the project being assembled. There is no
 * overriding going on amongst the models being used in this test:
 * each model in the lineage is providing a value that is not present
 * anywhere else in the lineage. We are just making sure that values
 * down in the lineage are bubbling up where they should.
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class ProjectInheritanceTest
    extends ProjectInheritanceTestCase
{
    // ----------------------------------------------------------------------
    //
    // p4 inherits from p3
    // p3 inherits from p2
    // p2 inherits from p1
    // p1 inherits from p0
    // p0 inhertis from super model
    //
    // or we can show it graphically as:
    //
    // p4 ---> p3 ---> p2 ---> p1 ---> p0 --> super model
    //
    // ----------------------------------------------------------------------

    public void testProjectInheritance()
        throws Exception
    {
        MavenProject p4 = projectBuilder.build( projectFile( "p4" ) );

        assertEquals( "p4", p4.getName() );

        // ----------------------------------------------------------------------
        // Value inherited from p3
        // ----------------------------------------------------------------------

        assertEquals( "2000", p4.getInceptionYear() );

        // ----------------------------------------------------------------------
        // Value taken from p2
        // ----------------------------------------------------------------------

        assertEquals( "mailing-list", ((MailingList)p4.getMailingLists().get(0)).getName() );

        // ----------------------------------------------------------------------
        // Value taken from p1
        // ----------------------------------------------------------------------

        assertEquals( "scm-url", p4.getScm().getUrl() );

        // ----------------------------------------------------------------------
        // Value taken from p4
        // ----------------------------------------------------------------------

        assertEquals( "Codehaus", p4.getOrganization().getName() );

        // ----------------------------------------------------------------------
        // Value taken from super model
        // ----------------------------------------------------------------------

        assertEquals( "4.0.0", p4.getModelVersion() );


        assertEquals( "4.0.0", p4.getModelVersion() );
    }
}
