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

import org.apache.maven.Maven;
import org.apache.maven.MavenTools;
import org.apache.maven.SettingsConfigurationException;
import org.apache.maven.BuildFailureException;
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
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.mapping.Lifecycle;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.reactor.MavenExecutionException;
import org.apache.maven.settings.Settings;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;

// collapse embed request into request
// take the stuff out of the start() and make it part of the request
// remove settings from the core
// there should be a general interaction listener and a noop by default

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

    private PlexusContainer container;

    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    private Maven maven;

    private MavenTools mavenTools;

    private MavenProjectBuilder mavenProjectBuilder;

    private ArtifactRepositoryFactory artifactRepositoryFactory;

    private WagonManager wagonManager;

    private MavenXpp3Reader modelReader;

    private MavenXpp3Writer modelWriter;

    private ProfileManager profileManager;

    private PluginDescriptorBuilder pluginDescriptorBuilder;

    private ArtifactFactory artifactFactory;

    private ArtifactResolver artifactResolver;

    private ArtifactRepositoryLayout defaultArtifactRepositoryLayout;

    private Settings settings;

    private ArtifactRepository localRepository;

    private LifecycleExecutor lifecycleExecutor;

    private PluginManager pluginManager;

    // ----------------------------------------------------------------------
    // Configuration
    // ----------------------------------------------------------------------

    private ClassWorld classWorld;

    private MavenEmbedderLogger logger;

    // ----------------------------------------------------------------------------
    // Constructor
    // ----------------------------------------------------------------------------

    public MavenEmbedder( ClassWorld classWorld,
                          MavenEmbedderLogger logger )
        throws MavenEmbedderException
    {
        this.classWorld = classWorld;

        this.logger = logger;

        if ( classWorld == null )
        {
            throw new IllegalStateException(
                "A classWorld or classloader must be specified using setClassLoader|World(ClassLoader)." );
        }

        try
        {
            container = new DefaultPlexusContainer( null, null, null, classWorld );
        }
        catch ( PlexusContainerException e )
        {
            throw new MavenEmbedderException( "Error starting Maven embedder.", e );
        }

        if ( logger != null )
        {
            container.setLoggerManager( new MavenEmbedderLoggerManager( new PlexusLoggerAdapter( logger ) ) );
        }

        try
        {
            // ----------------------------------------------------------------------
            // Lookup each of the components we need to provide the desired
            // client interface.
            // ----------------------------------------------------------------------

            //TODO needs to be a component
            modelReader = new MavenXpp3Reader();

            //TODO needs to be a component
            modelWriter = new MavenXpp3Writer();

            maven = (Maven) container.lookup( Maven.ROLE );

            mavenTools = (MavenTools) container.lookup( MavenTools.ROLE );

            //TODO needs to be a component
            pluginDescriptorBuilder = new PluginDescriptorBuilder();

            mavenProjectBuilder = (MavenProjectBuilder) container.lookup( MavenProjectBuilder.ROLE );

            // ----------------------------------------------------------------------
            // Artifact related components
            // ----------------------------------------------------------------------

            artifactRepositoryFactory = (ArtifactRepositoryFactory) container.lookup( ArtifactRepositoryFactory.ROLE );

            artifactFactory = (ArtifactFactory) container.lookup( ArtifactFactory.ROLE );

            artifactResolver = (ArtifactResolver) container.lookup( ArtifactResolver.ROLE );

            defaultArtifactRepositoryLayout =
                (ArtifactRepositoryLayout) container.lookup( ArtifactRepositoryLayout.ROLE, DEFAULT_LAYOUT_ID );

            wagonManager = (WagonManager) container.lookup( WagonManager.ROLE );

            // Components for looking up plugin metadata

            lifecycleExecutor = (LifecycleExecutor) container.lookup( LifecycleExecutor.ROLE );

            pluginManager = (PluginManager) container.lookup( PluginManager.ROLE );
        }
        catch ( ComponentLookupException e )
        {
            throw new MavenEmbedderException( "Cannot lookup required component.", e );
        }

    }

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    public void setClassWorld( ClassWorld classWorld )
    {
        this.classWorld = classWorld;
    }

    public ClassWorld getClassWorld()
    {
        return classWorld;
    }

    public File getLocalRepositoryDirectory()
    {
        return new File( getLocalRepositoryPath( settings ) );
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public MavenEmbedderLogger getLogger()
    {
        return logger;
    }

    // ----------------------------------------------------------------------
    // Embedder Client Contract
    // ----------------------------------------------------------------------

    // ----------------------------------------------------------------------
    // Model
    // ----------------------------------------------------------------------

    public Model readModel( File model )
        throws XmlPullParserException, IOException
    {
        return modelReader.read( new FileReader( model ) );
    }

    public void writeModel( Writer writer,
                            Model model )
        throws IOException
    {
        modelWriter.write( writer, model );
    }

    // ----------------------------------------------------------------------
    // Project
    // ----------------------------------------------------------------------

    /**
     * read the project.
     */
    public MavenProject readProject( File mavenProject )
        throws ProjectBuildingException
    {
        return mavenProjectBuilder.build( mavenProject, localRepository, profileManager );
    }

    /**
     * This method is used to grab the list of dependencies that belong to a project so that a UI
     * can be populated. For example, a list of libraries that are used by an Eclipse, Netbeans, or
     * IntelliJ project.
     */
    // Not well formed exceptions to point people at errors
    // line number in the originating POM so that errors can be shown
    // Need to walk down the tree of dependencies and find all the errors and report in the result
    // validate the request
    // for dependency errors: identifier, path
    // unable to see why you can't get a resource from the repository
    // short message or error id
    // completely obey the same settings used by the CLI, should work exactly the same as the
    //   command line. right now they are very different
    public MavenExecutionResult readProjectWithDependencies( MavenExecutionRequest request )
    {
        MavenProject project = null;

        // How can we get rid of the profile manager from the request

        try
        {
            project = mavenProjectBuilder.buildWithDependencies( new File( request.getPomFile() ),
                                                                 request.getLocalRepository(), profileManager,
                                                                 request.getTransferListener() );
        }
        catch ( ProjectBuildingException e )
        {
            return new DefaultMavenExecutionResult( project, Collections.singletonList( e ) );
        }
        catch ( ArtifactResolutionException e )
        {
            return new DefaultMavenExecutionResult( project, Collections.singletonList( e ) );
        }
        catch ( ArtifactNotFoundException e )
        {
            return new DefaultMavenExecutionResult( project, Collections.singletonList( e ) );
        }

        return new DefaultMavenExecutionResult( project, Collections.EMPTY_LIST );
    }

    // ----------------------------------------------------------------------
    // Artifacts
    // ----------------------------------------------------------------------

    public Artifact createArtifact( String groupId,
                                    String artifactId,
                                    String version,
                                    String scope,
                                    String type )
    {
        return artifactFactory.createArtifact( groupId, artifactId, version, scope, type );
    }

    public Artifact createArtifactWithClassifier( String groupId,
                                                  String artifactId,
                                                  String version,
                                                  String type,
                                                  String classifier )
    {
        return artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, type, classifier );
    }

    public void resolve( Artifact artifact,
                         List remoteRepositories,
                         ArtifactRepository localRepository )
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
            InputStream is = container.getContainerRealm().getResourceAsStream(
                "/plugins/" + summaryPluginDescriptor.getArtifactId() + ".xml" );

            if ( is == null )
            {
                throw new MavenEmbedderException( "Cannot find summary plugin descriptor." );
            }

            pluginDescriptor = pluginDescriptorBuilder.build( new InputStreamReader( is ) );
        }
        catch ( PlexusConfigurationException e )
        {
            throw new MavenEmbedderException( "Error retrieving plugin descriptor.", e );
        }

        return pluginDescriptor;
    }

    private SummaryPluginDescriptor makeMockPlugin( String groupId,
                                                    String artifactId,
                                                    String name )
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

        ComponentDescriptor descriptor = container.getComponentDescriptor( LifecycleExecutor.ROLE );

        PlexusConfiguration configuration = descriptor.getConfiguration();

        PlexusConfiguration[] phasesConfigurations =
            configuration.getChild( "lifecycles" ).getChild( 0 ).getChild( "phases" ).getChildren( "phase" );

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
        return createLocalRepository( mavenTools.getLocalRepositoryPath( settings ), DEFAULT_LOCAL_REPO_ID );
    }

    public ArtifactRepository createLocalRepository( String url,
                                                     String repositoryId )
    {
        if ( !url.startsWith( "file:" ) )
        {
            url = "file://" + url;
        }

        return createRepository( url, repositoryId );
    }

    public ArtifactRepository createRepository( String url,
                                                String repositoryId )
    {
        // snapshots vs releases
        // offline = to turning the update policy off

        //TODO: we'll need to allow finer grained creation of repositories but this will do for now

        String updatePolicyFlag = ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS;

        String checksumPolicyFlag = ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN;

        ArtifactRepositoryPolicy snapshotsPolicy =
            new ArtifactRepositoryPolicy( true, updatePolicyFlag, checksumPolicyFlag );

        ArtifactRepositoryPolicy releasesPolicy =
            new ArtifactRepositoryPolicy( true, updatePolicyFlag, checksumPolicyFlag );

        return artifactRepositoryFactory.createArtifactRepository( repositoryId, url, defaultArtifactRepositoryLayout,
                                                                   snapshotsPolicy, releasesPolicy );
    }

    // ----------------------------------------------------------------------
    //  Lifecycle
    // ----------------------------------------------------------------------

    public MavenExecutionResult execute( MavenExecutionRequest request )
        throws MavenEmbedderException, MavenExecutionException
    {
        profileManager = new DefaultProfileManager( container, request.getProperties() );

        profileManager.loadSettingsProfiles( settings );

        localRepository = createLocalRepository( settings );

        profileManager.explicitlyActivate( request.getActiveProfiles() );

        profileManager.explicitlyDeactivate( request.getInactiveProfiles() );

        return maven.execute( request );
    }

    // ----------------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------------

    public void stop()
        throws MavenEmbedderException
    {
        try
        {
            System.out.println( "container = " + container );
            container.release( mavenProjectBuilder );

            container.release( artifactRepositoryFactory );
        }
        catch ( ComponentLifecycleException e )
        {
            throw new MavenEmbedderException( "Cannot stop the embedder.", e );
        }
    }

    // ----------------------------------------------------------------------
    // Start of new embedder API
    // ----------------------------------------------------------------------

    public Settings buildSettings( File userSettingsPath,
                                   File globalSettingsPath,
                                   boolean interactive,
                                   boolean offline,
                                   boolean usePluginRegistry,
                                   Boolean pluginUpdateOverride )
        throws SettingsConfigurationException
    {
        return mavenTools.buildSettings( userSettingsPath, globalSettingsPath, interactive, offline, usePluginRegistry,
                                         pluginUpdateOverride );
    }

    public Settings buildSettings( File userSettingsPath,
                                   File globalSettingsPath,
                                   Boolean pluginUpdateOverride )
        throws SettingsConfigurationException
    {
        return mavenTools.buildSettings( userSettingsPath, globalSettingsPath, pluginUpdateOverride );
    }

    public File getUserSettingsPath( String optionalSettingsPath )
    {
        return mavenTools.getUserSettingsPath( optionalSettingsPath );
    }

    public File getGlobalSettingsPath()
    {
        return mavenTools.getGlobalSettingsPath();
    }

    public String getLocalRepositoryPath( Settings settings )
    {
        return mavenTools.getLocalRepositoryPath( settings );
    }

    // ----------------------------------------------------------------------------
    // Lifecycle Metadata
    // ----------------------------------------------------------------------------

    public Map getLifecycleMappings( MavenProject project,
                                     String lifecycle,
                                     String lastPhase )
        throws LifecycleExecutionException, BuildFailureException, PluginNotFoundException
    {
        MavenSession session =
            new MavenSession( container, settings, getLocalRepository(), null, null, null, null, null, new Date() );

        Map lifecycleMappings = lifecycleExecutor.constructLifecycleMappings( session, project, lifecycle, lastPhase );

        Map phases = new LinkedHashMap();

        for ( Iterator i = lifecycleMappings.keySet().iterator(); i.hasNext(); )
        {
            String lifecycleId = (String) i.next();

            List mojos = (List) lifecycleMappings.get( lifecycleId );

            Map executions = new LinkedHashMap();

            String groupId;

            String artifactId;

            String goalId;

            for ( Iterator j = mojos.iterator(); j.hasNext(); )
            {
                MojoExecution mojoExecution = (MojoExecution) j.next();

                MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

                goalId = mojoDescriptor.getGoal();

                groupId = mojoDescriptor.getPluginDescriptor().getGroupId();

                artifactId = mojoDescriptor.getPluginDescriptor().getArtifactId();

                String executionId = mojoExecution.getExecutionId();

                Xpp3Dom goalConfigurationDom = project.getGoalConfiguration( groupId, artifactId, executionId, goalId );

                XmlPlexusConfiguration pomConfiguration;

                if ( goalConfigurationDom == null )
                {
                    pomConfiguration = new XmlPlexusConfiguration( "configuration" );
                }
                else
                {
                    pomConfiguration = new XmlPlexusConfiguration( goalConfigurationDom );
                }

                PlexusConfiguration mergedConfiguration =
                    pluginManager.mergeMojoConfiguration( pomConfiguration, mojoDescriptor );

                executions.put( groupId + ":" + artifactId + ":" + goalId, mergedConfiguration );

            }

            phases.put( lifecycleId, executions );
        }

        return lifecycleMappings;
    }
}

