package org.apache.maven.embedder;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.CommonMavenObjectFactory;
import org.apache.maven.Maven;
import org.apache.maven.SettingsConfigurationException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.reactor.MavenExecutionException;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.settings.RuntimeInfo;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.events.TransferListener;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.classworlds.DuplicateRealmException;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.embed.Embedder;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class intended to be used by clients who wish to embed Maven into their applications
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 */
public class MavenEmbedder
{
    public static final String userHome = System.getProperty( "user.home" );

    // ----------------------------------------------------------------------
    // Embedder
    // ----------------------------------------------------------------------

    private Embedder embedder;

    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    private MavenProjectBuilder mavenProjectBuilder;

    private ArtifactRepositoryFactory artifactRepositoryFactory;

    private MavenSettingsBuilder settingsBuilder;

    private LifecycleExecutor lifecycleExecutor;

    private WagonManager wagonManager;

    private MavenXpp3Reader modelReader;

    private MavenXpp3Writer modelWriter;

    private ProfileManager profileManager;

    private PluginDescriptorBuilder pluginDescriptorBuilder;

    private ArtifactFactory artifactFactory;

    private ArtifactResolver artifactResolver;

    private ArtifactRepositoryLayout defaultArtifactRepositoryLayout;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private Maven maven;

    private CommonMavenObjectFactory mavenObjectFactory;

    // ----------------------------------------------------------------------
    // Configuration
    // ----------------------------------------------------------------------

    private Settings settings;

    private ArtifactRepository localRepository;

    private File localRepositoryDirectory;

    private ClassLoader classLoader;

    private ClassWorld classWorld;

    private MavenEmbedderLogger logger;

    // ----------------------------------------------------------------------
    // User options
    // ----------------------------------------------------------------------

    // release plugin uses this but in IDE there will probably always be some form of interaction.
    private boolean interactiveMode;

    private boolean offline;

    private String globalChecksumPolicy;

    /**
     * This option determines whether the embedder is to be aligned to the user
     * installation.
     */
    private boolean alignWithUserInstallation;

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    public void setInteractiveMode( boolean interactiveMode )
    {
        this.interactiveMode = interactiveMode;
    }

    public boolean isInteractiveMode()
    {
        return interactiveMode;
    }

    public void setOffline( boolean offline )
    {
        this.offline = offline;
    }

    public boolean isOffline()
    {
        return offline;
    }

    public void setGlobalChecksumPolicy( String globalChecksumPolicy )
    {
        this.globalChecksumPolicy = globalChecksumPolicy;
    }

    public String getGlobalChecksumPolicy()
    {
        return globalChecksumPolicy;
    }

    public boolean isAlignWithUserInstallation()
    {
        return alignWithUserInstallation;
    }

    public void setAlignWithUserInstallation( boolean alignWithUserInstallation )
    {
        this.alignWithUserInstallation = alignWithUserInstallation;
    }

    /**
     * Set the classloader to use with the maven embedder.
     *
     * @param classLoader
     */
    public void setClassLoader( ClassLoader classLoader )
    {
        this.classLoader = classLoader;
    }

    public ClassLoader getClassLoader()
    {
        return classLoader;
    }

    public void setClassWorld( ClassWorld classWorld )
    {
        this.classWorld = classWorld;
    }

    public ClassWorld getClassWorld()
    {
        return classWorld;
    }

    public void setLocalRepositoryDirectory( File localRepositoryDirectory )
    {
        this.localRepositoryDirectory = localRepositoryDirectory;
    }

    public File getLocalRepositoryDirectory()
    {
        return localRepositoryDirectory;
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public MavenEmbedderLogger getLogger()
    {
        return logger;
    }

    public void setLogger( MavenEmbedderLogger logger )
    {
        this.logger = logger;
    }

    // ----------------------------------------------------------------------
    // Embedder Client Contract
    // ----------------------------------------------------------------------

    // ----------------------------------------------------------------------
    // Model
    // ----------------------------------------------------------------------

    public Model readModel( File model )
        throws XmlPullParserException, FileNotFoundException, IOException
    {
        return modelReader.read( new FileReader( model ) );
    }

    public void writeModel( Writer writer, Model model )
        throws IOException
    {
        modelWriter.write( writer, model );
    }

    // ----------------------------------------------------------------------
    // Project
    // ----------------------------------------------------------------------

    public MavenProject readProject( File mavenProject )
        throws ProjectBuildingException
    {
        return mavenProjectBuilder.build( mavenProject, localRepository, profileManager );
    }

    public MavenProject readProjectWithDependencies( File mavenProject, TransferListener transferListener )
        throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException
    {
        return mavenProjectBuilder.buildWithDependencies( mavenProject, localRepository, profileManager, transferListener );
    }

    public MavenProject readProjectWithDependencies( File mavenProject )
        throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException
    {
        return mavenProjectBuilder.buildWithDependencies( mavenProject, localRepository, profileManager );
    }

    public List collectProjects( File basedir, String[] includes, String[] excludes )
        throws MojoExecutionException
    {
        List projects = new ArrayList();

        List poms = getPomFiles( basedir, includes, excludes );

        for ( Iterator i = poms.iterator(); i.hasNext(); )
        {
            File pom = (File) i.next();

            try
            {
                MavenProject p = readProject( pom );

                projects.add( p );

            }
            catch ( ProjectBuildingException e )
            {
                throw new MojoExecutionException( "Error loading " + pom, e );
            }
        }

        return projects;
    }

    // ----------------------------------------------------------------------
    // Artifacts
    // ----------------------------------------------------------------------

    public Artifact createArtifact( String groupId, String artifactId, String version, String scope, String type )
    {
        return artifactFactory.createArtifact( groupId, artifactId, version, scope, type );
    }

    public Artifact createArtifactWithClassifier( String groupId, String artifactId, String version, String type, String classifier )
    {
        return artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, type, classifier );
    }

    public void resolve( Artifact artifact, List remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        artifactResolver.resolve( artifact, remoteRepositories, localRepository );
    }

    // ----------------------------------------------------------------------
    // Plugins
    // ----------------------------------------------------------------------

    public List getAvailablePlugins()
    {
        List plugins = new ArrayList();

        plugins.add( makeMockPlugin( "org.apache.maven.plugins", "maven-jar-plugin", "Maven Jar Plug-in" ) );

        plugins.add( makeMockPlugin( "org.apache.maven.plugins", "maven-compiler-plugin", "Maven Compiler Plug-in" ) );

        return plugins;
    }

    public PluginDescriptor getPluginDescriptor( SummaryPluginDescriptor summaryPluginDescriptor )
        throws MavenEmbedderException
    {
        PluginDescriptor pluginDescriptor;

        try
        {
            InputStream is = classLoader.getResourceAsStream( "/plugins/" + summaryPluginDescriptor.getArtifactId() + ".xml" );

            pluginDescriptor = pluginDescriptorBuilder.build( new InputStreamReader( is ) );
        }
        catch ( PlexusConfigurationException e )
        {
            throw new MavenEmbedderException( "Error retrieving plugin descriptor.", e );
        }

        return pluginDescriptor;
    }

    private SummaryPluginDescriptor makeMockPlugin( String groupId, String artifactId, String name )
    {
        return new SummaryPluginDescriptor( groupId, artifactId, name );
    }

    // ----------------------------------------------------------------------
    // Execution of phases/goals
    // ----------------------------------------------------------------------

    // ----------------------------------------------------------------------
    // Lifecycle information
    // ----------------------------------------------------------------------

    public List getLifecyclePhases()
        throws MavenEmbedderException
    {
        List phases = new ArrayList();

        ComponentDescriptor descriptor = embedder.getContainer().getComponentDescriptor( LifecycleExecutor.ROLE );

        PlexusConfiguration configuration = descriptor.getConfiguration();

        PlexusConfiguration[] phasesConfigurations = configuration.getChild( "lifecycles" ).getChild( 0 ).getChild( "phases" ).getChildren( "phase" );

        try
        {
            for ( int i = 0; i < phasesConfigurations.length; i++ )
            {
                phases.add( phasesConfigurations[i].getValue() );
            }
        }
        catch ( PlexusConfigurationException e )
        {
            throw new MavenEmbedderException( "Cannot retrieve default lifecycle phasesConfigurations.", e );
        }

        return phases;
    }

    // ----------------------------------------------------------------------
    // Remote Repository
    // ----------------------------------------------------------------------

    // ----------------------------------------------------------------------
    // Local Repository
    // ----------------------------------------------------------------------

    public static final String DEFAULT_LOCAL_REPO_ID = "local";

    public static final String DEFAULT_LAYOUT_ID = "default";

    public ArtifactRepository createLocalRepository( File localRepository )
        throws ComponentLookupException
    {
        return createLocalRepository( localRepository.getAbsolutePath(), DEFAULT_LOCAL_REPO_ID );
    }

    public ArtifactRepository createLocalRepository( Settings settings )
    {
        return createLocalRepository( settings.getLocalRepository(), DEFAULT_LOCAL_REPO_ID );
    }

    public ArtifactRepository createLocalRepository( String url, String repositoryId )
    {
        if ( !url.startsWith( "file:" ) )
        {
            url = "file://" + url;
        }

        return createRepository( url, repositoryId );
    }

    public ArtifactRepository createRepository( String url, String repositoryId )
    {
        // snapshots vs releases
        // offline = to turning the update policy off

        //TODO: we'll need to allow finer grained creation of repositories but this will do for now

        String updatePolicyFlag = ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS;

        String checksumPolicyFlag = ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN;

        ArtifactRepositoryPolicy snapshotsPolicy = new ArtifactRepositoryPolicy( true, updatePolicyFlag, checksumPolicyFlag );

        ArtifactRepositoryPolicy releasesPolicy = new ArtifactRepositoryPolicy( true, updatePolicyFlag, checksumPolicyFlag );

        return artifactRepositoryFactory.createArtifactRepository( repositoryId, url, defaultArtifactRepositoryLayout, snapshotsPolicy, releasesPolicy );
    }

    // ----------------------------------------------------------------------
    // Internal utility code
    // ----------------------------------------------------------------------

    private List getPomFiles( File basedir, String[] includes, String[] excludes )
    {
        DirectoryScanner scanner = new DirectoryScanner();

        scanner.setBasedir( basedir );

        scanner.setIncludes( includes );

        scanner.setExcludes( excludes );

        scanner.scan();

        List poms = new ArrayList();

        for ( int i = 0; i < scanner.getIncludedFiles().length; i++ )
        {
            poms.add( new File( basedir, scanner.getIncludedFiles()[i] ) );
        }

        return poms;
    }

    // ----------------------------------------------------------------------
    //  Lifecycle
    // ----------------------------------------------------------------------

    public void start()
        throws MavenEmbedderException
    {
        detectUserInstallation();

        // ----------------------------------------------------------------------
        // Set the maven.home system property which is need by components like
        // the plugin registry builder.
        // ----------------------------------------------------------------------

        if ( classWorld == null && classLoader == null )
        {
            throw new IllegalStateException( "A classWorld or classloader must be specified using setClassLoader|World(ClassLoader)." );
        }

        embedder = new Embedder();

        if ( logger != null )
        {
            embedder.setLoggerManager( new MavenEmbedderLoggerManager( new PlexusLoggerAdapter( logger ) ) );
        }

        try
        {
            if ( classWorld == null )
            {
                classWorld = new ClassWorld();

                classWorld.newRealm( "plexus.core", classLoader );
            }

            embedder.start( classWorld );

            // ----------------------------------------------------------------------
            // Lookup each of the components we need to provide the desired
            // client interface.
            // ----------------------------------------------------------------------

            modelReader = new MavenXpp3Reader();

            modelWriter = new MavenXpp3Writer();

            maven = (Maven) embedder.lookup( Maven.ROLE );

            mavenObjectFactory = (CommonMavenObjectFactory) embedder.lookup( CommonMavenObjectFactory.ROLE );

            pluginDescriptorBuilder = new PluginDescriptorBuilder();

            profileManager = new DefaultProfileManager( embedder.getContainer() );

            mavenProjectBuilder = (MavenProjectBuilder) embedder.lookup( MavenProjectBuilder.ROLE );

            // ----------------------------------------------------------------------
            // Artifact related components
            // ----------------------------------------------------------------------

            artifactRepositoryFactory = (ArtifactRepositoryFactory) embedder.lookup( ArtifactRepositoryFactory.ROLE );

            artifactFactory = (ArtifactFactory) embedder.lookup( ArtifactFactory.ROLE );

            artifactResolver = (ArtifactResolver) embedder.lookup( ArtifactResolver.ROLE );

            defaultArtifactRepositoryLayout = (ArtifactRepositoryLayout) embedder.lookup( ArtifactRepositoryLayout.ROLE, DEFAULT_LAYOUT_ID );

            lifecycleExecutor = (LifecycleExecutor) embedder.lookup( LifecycleExecutor.ROLE );

            wagonManager = (WagonManager) embedder.lookup( WagonManager.ROLE );

            profileManager.loadSettingsProfiles( settings );
        }
        catch ( PlexusContainerException e )
        {
            throw new MavenEmbedderException( "Cannot start Plexus embedder.", e );
        }
        catch ( DuplicateRealmException e )
        {
            throw new MavenEmbedderException( "Cannot create Classworld realm for the embedder.", e );
        }
        catch ( ComponentLookupException e )
        {
            throw new MavenEmbedderException( "Cannot lookup required component.", e );
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private void detectUserInstallation()
    {
        if ( new File( userHome, ".m2" ).exists() )
        {
            alignWithUserInstallation = true;
        }
    }

    // ----------------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------------

    public void stop()
        throws MavenEmbedderException
    {
        try
        {
            embedder.release( mavenProjectBuilder );

            embedder.release( artifactRepositoryFactory );

            embedder.release( settingsBuilder );

            embedder.release( lifecycleExecutor );
        }
        catch ( ComponentLifecycleException e )
        {
            throw new MavenEmbedderException( "Cannot stop the embedder.", e );
        }
    }

    // ----------------------------------------------------------------------
    // Start of new embedder API
    // ----------------------------------------------------------------------

    public void execute( MavenExecutionRequest request )
        throws MavenExecutionException
    {
        maven.execute(  request );
    }

    public Settings buildSettings( File userSettingsPath,
                                   File globalSettingsPath,
                                   boolean interactive,
                                   boolean offline,
                                   boolean usePluginRegistry,
                                   Boolean pluginUpdateOverride )
        throws SettingsConfigurationException
    {
        return mavenObjectFactory.buildSettings( userSettingsPath,
                                                 globalSettingsPath,
                                                 interactive,
                                                 offline,
                                                 usePluginRegistry,
                                                 pluginUpdateOverride );
    }
}
