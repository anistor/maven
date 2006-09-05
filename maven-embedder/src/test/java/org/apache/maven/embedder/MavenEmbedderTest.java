package org.apache.maven.embedder;

import junit.framework.TestCase;
import org.apache.maven.artifact.Artifact;
//import org.apache.maven.cli.ConsoleDownloadMonitor;
import org.apache.maven.model.Model;
import org.apache.maven.monitor.event.DefaultEventMonitor;
import org.apache.maven.monitor.event.EventMonitor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.classworlds.ClassWorld;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.net.URLClassLoader;
import java.net.URL;

public class MavenEmbedderTest
    extends TestCase
{
    private String basedir;

    private MavenEmbedder embedder;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        basedir = System.getProperty( "basedir" );

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        //checkClassLoader( classLoader );

        embedder = new MavenEmbedder( new ClassWorld( "plexus.core", classLoader ),
                                      new MavenEmbedderConsoleLogger() );
    }

    public void checkClassLoader( ClassLoader classLoader )
    {
        System.out.println( "classLoader = " + classLoader );

        if ( classLoader instanceof URLClassLoader )
        {
            URLClassLoader loader = (URLClassLoader) classLoader;

            URL[] urls = loader.getURLs();

            for ( int i = 0; i < urls.length; i++ )
            {
                URL url = urls[i];

                System.out.println( "url = " + url );
            }
        }
    }

    protected void tearDown()
        throws Exception
    {
        embedder.stop();
    }

    public void xtestMavenEmbedder()
        throws Exception
    {
        modelReadingTest();

        projectReadingTest();
    }

    // ----------------------------------------------------------------------
    // Goal/Phase execution tests
    // ----------------------------------------------------------------------

    public void testPhaseExecution()
        throws Exception
    {
        File testDirectory = new File( basedir, "src/test/embedder-test-project" );

        File targetDirectory = new File( basedir, "target/embedder-test-project" );

        FileUtils.copyDirectoryStructure( testDirectory, targetDirectory );

        File pomFile = new File( targetDirectory, "pom.xml" );

        EventMonitor eventMonitor =
            new DefaultEventMonitor( new PlexusLoggerAdapter( new MavenEmbedderConsoleLogger() ) );

        MavenExecutionRequest request = createRequest();

        MavenExecutionResult result = embedder.execute( request );

        File jar = new File( targetDirectory, "target/embedder-test-project-1.0-SNAPSHOT.jar" );

        assertTrue( jar.exists() );
    }

    protected MavenExecutionRequest createRequest()
    {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest()
            .setBasedir( baseDirectory )
            .setGoals( goals )
            .setLocalRepositoryPath( localRepositoryPath )
            .setProperties( executionProperties )
            .setFailureBehavior( reactorFailureBehaviour )
            .setRecursive( recursive )
            .setReactorActive( reactorActive )
            .setPomFile( alternatePomFile )
            .setShowErrors( showErrors )
            .setInteractive( interactive )
            .addActiveProfiles( activeProfiles )
            .addInactiveProfiles( inactiveProfiles )
            .setLoggingLevel( loggingLevel )
            .activateDefaultEventMonitor()
            .setSettings( settings )
            .setTransferListener( transferListener )
            .setOffline( offline )
            .setUpdateSnapshots( updateSnapshots )
            .setGlobalChecksumPolicy( globalChecksumPolicy );

        return request;
    }

    // ----------------------------------------------------------------------
    // Test mock plugin metadata
    // ----------------------------------------------------------------------

    // Disable as the mock data appears to be missing now.

    public void xtestMockPluginMetadata()
        throws Exception
    {
        List plugins = embedder.getAvailablePlugins();

        SummaryPluginDescriptor spd = (SummaryPluginDescriptor) plugins.get( 0 );

        assertNotNull( spd );

        PluginDescriptor pd = embedder.getPluginDescriptor( spd );

        assertNotNull( pd );

        assertEquals( "org.apache.embedder.plugins", pd.getGroupId() );
    }

    // ----------------------------------------------------------------------
    // Lifecycle phases
    // ----------------------------------------------------------------------

    public void testRetrievingLifecyclePhases()
        throws Exception
    {
        List phases = embedder.getLifecyclePhases();

        assertEquals( "validate", (String) phases.get( 0 ) );

        assertEquals( "initialize", (String) phases.get( 1 ) );

        assertEquals( "generate-sources", (String) phases.get( 2 ) );
    }

    // ----------------------------------------------------------------------
    // Repository
    // ----------------------------------------------------------------------

    public void testLocalRepositoryRetrieval()
        throws Exception
    {
        assertNotNull( embedder.getLocalRepository().getBasedir() );
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    protected void modelReadingTest()
        throws Exception
    {
        // ----------------------------------------------------------------------
        // Test model reading
        // ----------------------------------------------------------------------

        Model model = embedder.readModel( getPomFile() );

        assertEquals( "org.apache.embedder", model.getGroupId() );
    }

    protected void projectReadingTest()
        throws Exception
    {
        //MavenProject project = embedder.readProjectWithDependencies( getPomFile() );
        MavenProject project = null;

        assertEquals( "org.apache.embedder", project.getGroupId() );

        Set artifacts = project.getArtifacts();

        assertEquals( 1, artifacts.size() );

        Artifact artifact = (Artifact) artifacts.iterator().next();
    }

    // ----------------------------------------------------------------------
    // Internal Utilities
    // ----------------------------------------------------------------------

    protected File getPomFile()
    {
        return new File( basedir, "src/test/resources/pom.xml" );
    }
}
