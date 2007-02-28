package org.apache.maven.embedder.configuration;

import org.codehaus.plexus.PlexusTestCase;
import org.apache.maven.embedder.MavenEmbedder;

import java.io.File;

/** @author Jason van Zyl */
public class ValidateConfigurationTest
    extends PlexusTestCase
{
    public void testConfigurationOnlyUserSettingsAreActiveAndItIsValid()
    {
        File user = new File( getBasedir(), "src/test/resources/settings/valid-settings.xml" );

        Configuration configuration = new DefaultConfiguration()
            .setUserSettingsFile( user );

        ConfigurationValidationResult result = MavenEmbedder.validateConfiguration( configuration );

        assertTrue( result.isUserSettingsFileParses() );

        assertTrue( result.isUserSettingsFileParses() );
    }

    public void testConfigurationOnlyGlobalSettingsAreActiveAndItIsValid()
    {
        File global = new File( getBasedir(), "src/test/resources/settings/valid-settings.xml" );

        Configuration configuration = new DefaultConfiguration()
            .setUserSettingsFile( global );

        ConfigurationValidationResult result = MavenEmbedder.validateConfiguration( configuration );

        assertTrue( result.isGlobalSettingsFilePresent() );

        assertTrue( result.isGlobalSettingsFileParses() );
    }
}
