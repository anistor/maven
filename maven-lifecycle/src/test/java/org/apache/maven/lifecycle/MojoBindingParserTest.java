package org.apache.maven.lifecycle;

import junit.framework.TestCase;

public class MojoBindingParserTest
    extends TestCase
{

    public void testPrefixGoalSpec_PrefixReferenceAllowed()
        throws LifecycleSpecificationException
    {
        String spec = "prefix:goal";

        MojoBinding binding = MojoBindingParser.parseMojoBinding( spec, true );

        assertTrue( binding instanceof PrefixedMojoBinding );
        assertEquals( "prefix", ( (PrefixedMojoBinding) binding ).getPrefix() );
        assertEquals( "goal", binding.getGoal() );
    }

    public void testPrefixGoalSpec_PrefixReferenceNotAllowed()
    {
        String spec = "prefix:goal";

        try
        {
            MojoBindingParser.parseMojoBinding( spec, false );

            fail( "Should fail when prefix references are not allowed." );
        }
        catch ( LifecycleSpecificationException e )
        {
            // expected.
        }
    }

    public void testGroupIdArtifactIdGoalSpec_ShouldParseCorrectly()
        throws LifecycleSpecificationException
    {
        String spec = "group:artifact:goal";

        MojoBinding binding = MojoBindingParser.parseMojoBinding( spec, false );

        assertEquals( "group", binding.getGroupId() );
        assertEquals( "artifact", binding.getArtifactId() );
        assertNull( binding.getVersion() );
        assertEquals( "goal", binding.getGoal() );
    }

    public void testGroupIdArtifactIdVersionGoalSpec_ShouldParseCorrectly()
        throws LifecycleSpecificationException
    {
        String spec = "group:artifact:version:goal";

        MojoBinding binding = MojoBindingParser.parseMojoBinding( spec, false );

        assertEquals( "group", binding.getGroupId() );
        assertEquals( "artifact", binding.getArtifactId() );
        assertEquals( "version", binding.getVersion() );
        assertEquals( "goal", binding.getGoal() );
    }

    public void testSpecWithTooManyParts_ShouldFail()
    {
        String spec = "group:artifact:version:type:goal";

        try
        {
            MojoBindingParser.parseMojoBinding( spec, false );
            
            fail( "Should fail because spec has too many parts (type part is not allowed)." );
        }
        catch ( LifecycleSpecificationException e )
        {
            // expected
        }
    }

}
