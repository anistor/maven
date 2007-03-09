package org.apache.maven.lifecycle;

import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.codehaus.plexus.PlexusTestCase;

import java.util.List;

public class LegacyLifecycleMappingParserTest
    extends PlexusTestCase
{
    
    private LegacyLifecycleParsingTestComponent testComponent;
    
    public void setUp() throws Exception
    {
        super.setUp();
        
        testComponent = (LegacyLifecycleParsingTestComponent) lookup( LegacyLifecycleParsingTestComponent.ROLE, "default" );
    }
    
    public void tearDown() throws Exception
    {
        release( testComponent );
        
        super.tearDown();
    }
    
    public void testParseDefaultMappings_UsingExistingDefaultMappings() throws LifecycleSpecificationException
    {
        List lifecycles = testComponent.getLifecycles();
        LifecycleBindings bindings = LegacyLifecycleMappingParser.parseDefaultMappings( lifecycles );
        
//        <clean>org.apache.maven.plugins:maven-clean-plugin:clean</clean>
        List cleanPhase = bindings.getCleanBinding().getClean().getBindings();
        assertEquals( 1, cleanPhase.size() );
        
        MojoBinding binding = (MojoBinding) cleanPhase.get( 0 );
        assertEquals( "org.apache.maven.plugins", binding.getGroupId() );
        assertEquals( "maven-clean-plugin", binding.getArtifactId() );
        assertEquals( "clean", binding.getGoal() );
        
//        <site>org.apache.maven.plugins:maven-site-plugin:site</site>
        List sitePhase = bindings.getSiteBinding().getSite().getBindings();
        assertEquals( 1, sitePhase.size() );
        
        binding = (MojoBinding) sitePhase.get( 0 );
        assertEquals( "org.apache.maven.plugins", binding.getGroupId() );
        assertEquals( "maven-site-plugin", binding.getArtifactId() );
        assertEquals( "site", binding.getGoal() );
        
//      <site-deploy>org.apache.maven.plugins:maven-site-plugin:deploy</site-deploy>
        List siteDeployPhase = bindings.getSiteBinding().getSiteDeploy().getBindings();
        assertEquals( 1, siteDeployPhase.size() );
        
        binding = (MojoBinding) siteDeployPhase.get( 0 );
        assertEquals( "org.apache.maven.plugins", binding.getGroupId() );
        assertEquals( "maven-site-plugin", binding.getArtifactId() );
        assertEquals( "deploy", binding.getGoal() );
    }

}
