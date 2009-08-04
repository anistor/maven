 package org.apache.maven.lifecycle;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.maven.ProjectDependenciesResolver;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataReadException;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.BuildSuccess;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.CycleDetectedInPluginGraphException;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.lifecycle.Execution;
import org.apache.maven.plugin.lifecycle.Phase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

//TODO: The configuration for the lifecycle needs to be externalized so that I can use the annotations properly for the wiring and reference and external source for the lifecycle configuration.
//TODO: check for online status in the build plan and die if necessary

/**
 * @author Jason van Zyl
 */
public class DefaultLifecycleExecutor
    implements LifecycleExecutor, Initializable
{
    @Requirement
    private Logger logger;

    @Requirement
    private PluginManager pluginManager;

    @Requirement
    protected RepositorySystem repositorySystem;

    @Requirement
    private ProjectDependenciesResolver projectDependenciesResolver;
            
    // @Configuration(source="org/apache/maven/lifecycle/lifecycles.xml")    
    private List<Lifecycle> lifecycles;

    /**
     * We use this to display all the lifecycles available and their phases to users. Currently this is primarily
     * used in the IDE integrations where a UI is presented to the user and they can select the lifecycle phase
     * they would like to execute.
     */
    private Map<String,Lifecycle> lifecycleMap;
    
    /**
     * We use this to map all phases to the lifecycle that contains it. This is used so that a user can specify the 
     * phase they want to execute and we can easily determine what lifecycle we need to run.
     */
    private Map<String, Lifecycle> phaseToLifecycleMap;

    /**
     * These mappings correspond to packaging types, like WAR packaging, which configure a particular mojos
     * to run in a given phase.
     */
    @Requirement
    private Map<String, LifecycleMapping> lifecycleMappings;
    
    public void execute( MavenSession session )
    {
        // TODO: Use a listener here instead of loggers
        
        logger.info( "Build Order:" );
        
        logger.info( "" );
        
        for( MavenProject project : session.getProjects() )
        {
            logger.info( project.getName() );
        }
        
        logger.info( "" );
        
        MavenProject rootProject = session.getTopLevelProject();

        List<String> goals = session.getGoals();

        if ( goals.isEmpty() && rootProject != null )
        {
            String goal = rootProject.getDefaultGoal();

            if ( goal != null )
            {
                goals = Collections.singletonList( goal );
            }
        }

        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();

        MavenExecutionResult result = session.getResult();

        for ( MavenProject currentProject : session.getProjects() )
        {
            if ( session.isBlackListed( currentProject ) )
            {
                logger.info( "Skipping " + currentProject.getName() );
                logger.info( "This project has been banned from the build due to previous failures." );

                continue;
            }

            logger.info( "Building " + currentProject.getName() );

            long buildStartTime = System.currentTimeMillis();

            try
            {
                session.setCurrentProject( currentProject );

                ClassRealm projectRealm = currentProject.getClassRealm();
                if ( projectRealm != null )
                {
                    Thread.currentThread().setContextClassLoader( projectRealm );
                }

                MavenExecutionPlan executionPlan =
                    calculateExecutionPlan( session, goals.toArray( new String[goals.size()] ) );

                //TODO: once we have calculated the build plan then we should accurately be able to download
                // the project dependencies. Having it happen in the plugin manager is a tangled mess. We can optimize this
                // later by looking at the build plan. Would be better to just batch download everything required by the reactor.

                projectDependenciesResolver.resolve( currentProject, executionPlan.getRequiredResolutionScopes(), session.getLocalRepository(), currentProject.getRemoteArtifactRepositories() );

                if ( logger.isDebugEnabled() )
                {
                    logger.debug( "=== BUILD PLAN ===" );
                    logger.debug( "Project:       " + currentProject );
                    for ( MojoExecution mojoExecution : executionPlan.getExecutions() )
                    {
                        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();
                        PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();
                        logger.debug( "------------------" );
                        logger.debug( "Goal:          " + pluginDescriptor.getGroupId() + ':' + pluginDescriptor.getArtifactId() + ':' + pluginDescriptor.getVersion() + ':' + mojoDescriptor.getGoal()
                            + ':' + mojoExecution.getExecutionId() );
                        logger.debug( "Configuration: " + String.valueOf( mojoExecution.getConfiguration() ) );
                    }
                    logger.debug( "==================" );
                }

                for ( MojoExecution mojoExecution : executionPlan.getExecutions() )
                {
                    execute( currentProject, session, mojoExecution );
                }

                long buildEndTime = System.currentTimeMillis();

                result.addBuildSummary( new BuildSuccess( currentProject, buildEndTime - buildStartTime ) );
            }
            catch ( Exception e )
            {
                result.addException( e );

                long buildEndTime = System.currentTimeMillis();

                result.addBuildSummary( new BuildFailure( currentProject, buildEndTime - buildStartTime, e ) );

                if ( MavenExecutionRequest.REACTOR_FAIL_NEVER.equals( session.getReactorFailureBehavior() ) )
                {
                    // continue the build
                }
                else if ( MavenExecutionRequest.REACTOR_FAIL_AT_END.equals( session.getReactorFailureBehavior() ) )
                {
                    // continue the build but ban all projects that depend on the failed one
                    session.blackList( currentProject );
                }
                else if ( MavenExecutionRequest.REACTOR_FAIL_FAST.equals( session.getReactorFailureBehavior() ) )
                {
                    // abort the build
                    return;
                }
                else
                {
                    throw new IllegalArgumentException( "invalid reactor failure behavior "
                        + session.getReactorFailureBehavior() );
                }
            }
            finally
            {
                session.setCurrentProject( null );

                Thread.currentThread().setContextClassLoader( oldContextClassLoader );
            }
        }        
    }        

    private void execute( MavenProject project, MavenSession session, MojoExecution mojoExecution )
        throws MojoFailureException, MojoExecutionException, PluginConfigurationException, PluginManagerException
    {
        MavenProject executionProject = null;

        List<MojoExecution> forkedExecutions = mojoExecution.getForkedExecutions();

        if ( !forkedExecutions.isEmpty() )
        {
            executionProject = project.clone();

            session.setCurrentProject( executionProject );
            try
            {
                for ( MojoExecution forkedExecution : forkedExecutions )
                {
                    execute( executionProject, session, forkedExecution );
                }
            }
            finally
            {
                session.setCurrentProject( project );
            }
        }

        project.setExecutionProject( executionProject );

        logger.info( executionDescription( mojoExecution, project ) );

        pluginManager.executeMojo( session, mojoExecution );
    }

    public MavenExecutionPlan calculateExecutionPlan( MavenSession session, String... tasks )
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
        CycleDetectedInPluginGraphException, MojoNotFoundException, NoPluginFoundForPrefixException,
        InvalidPluginDescriptorException, PluginManagerException, LifecyclePhaseNotFoundException,
        LifecycleNotFoundException
    {
        MavenProject project = session.getCurrentProject();

        List<MojoExecution> lifecyclePlan = new ArrayList<MojoExecution>();

        Set<String> requiredDependencyResolutionScopes = new HashSet<String>();

        for ( String task : tasks )
        {
            if ( task.indexOf( ":" ) > 0 )
            {
                calculateExecutionForIndividualGoal( session, lifecyclePlan, task );
            }
            else
            {
                calculateExecutionForLifecyclePhase( session, lifecyclePlan, task );
            }
        }

        // 7. Now we create the correct configuration for the mojo to execute.
        // 
        for ( MojoExecution mojoExecution : lifecyclePlan )
        {
            // These are bits that look like this:
            //
            // org.apache.maven.plugins:maven-remote-resources-plugin:1.0:process
            //                        

            MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

            if ( mojoDescriptor == null )
            {
                mojoDescriptor =
                    pluginManager.getMojoDescriptor( mojoExecution.getPlugin(), mojoExecution.getGoal(),
                                                     session.getLocalRepository(),
                                                     project.getPluginArtifactRepositories() );

                mojoExecution.setMojoDescriptor( mojoDescriptor );
            }

            PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();
            if ( pluginDescriptor.getPlugin().isExtensions() )
            {
                pluginDescriptor.setClassRealm( pluginManager.getPluginRealm( session, pluginDescriptor ) );
            }

            populateMojoExecutionConfiguration( project, mojoExecution,
                                                MojoExecution.Source.CLI.equals( mojoExecution.getSource() ) );

            calculateForkedExecutions( mojoExecution, session, project, new HashSet<MojoDescriptor>() );

            collectDependencyResolutionScopes( requiredDependencyResolutionScopes, mojoExecution );
        }

        return new MavenExecutionPlan( lifecyclePlan, requiredDependencyResolutionScopes );
    }      

    private void collectDependencyResolutionScopes( Collection<String> requiredDependencyResolutionScopes,
                                                    MojoExecution mojoExecution )
    {
        String requiredDependencyResolutionScope = mojoExecution.getMojoDescriptor().isDependencyResolutionRequired();

        if ( StringUtils.isNotEmpty( requiredDependencyResolutionScope ) )
        {
            requiredDependencyResolutionScopes.add( requiredDependencyResolutionScope );
        }

        for ( MojoExecution forkedExecution : mojoExecution.getForkedExecutions() )
        {
            collectDependencyResolutionScopes( requiredDependencyResolutionScopes, forkedExecution );
        }
    }

    private void calculateExecutionForIndividualGoal( MavenSession session, List<MojoExecution> lifecyclePlan, String goal ) 
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException, CycleDetectedInPluginGraphException, MojoNotFoundException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException
    {
        // If this is a goal like "mvn modello:java" and the POM looks like the following:
        //
        // <project>
        //   <modelVersion>4.0.0</modelVersion>
        //   <groupId>org.apache.maven.plugins</groupId>
        //   <artifactId>project-plugin-level-configuration-only</artifactId>
        //   <version>1.0.1</version>
        //   <build>
        //     <plugins>
        //       <plugin>
        //         <groupId>org.codehaus.modello</groupId>
        //         <artifactId>modello-maven-plugin</artifactId>
        //         <version>1.0.1</version>
        //         <configuration>
        //           <version>1.1.0</version>
        //           <models>
        //             <model>src/main/mdo/remote-resources.mdo</model>
        //           </models>
        //         </configuration>
        //       </plugin>
        //     </plugins>
        //   </build>
        // </project>                
        //
        // We want to 
        //
        // - take the plugin/configuration in the POM and merge it with the plugin's default configuration found in its plugin.xml
        // - attach that to the MojoExecution for its configuration
        // - give the MojoExecution an id of default-<goal>.
        
        MojoDescriptor mojoDescriptor = getMojoDescriptor( goal, session );

        MojoExecution mojoExecution = new MojoExecution( mojoDescriptor, "default-cli", MojoExecution.Source.CLI );

        lifecyclePlan.add( mojoExecution );        
    }
    
    // 1. Find the lifecycle given the phase (default lifecycle when given install)
    // 2. Find the lifecycle mapping that corresponds to the project packaging (jar lifecycle mapping given the jar packaging)
    // 3. Find the mojos associated with the lifecycle given the project packaging (jar lifecycle mapping for the default lifecycle)
    // 4. Bind those mojos found in the lifecycle mapping for the packaging to the lifecycle
    // 5. Bind mojos specified in the project itself to the lifecycle    
    private void calculateExecutionForLifecyclePhase( MavenSession session, List<MojoExecution> lifecyclePlan,
                                                      String lifecyclePhase )
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
        CycleDetectedInPluginGraphException, MojoNotFoundException, NoPluginFoundForPrefixException,
        InvalidPluginDescriptorException, LifecyclePhaseNotFoundException
    {
        Map<String, List<MojoExecution>> phaseToMojoMapping = calculateLifecycleMappings( session, lifecyclePhase );

        for ( List<MojoExecution> mojoExecutions : phaseToMojoMapping.values() )
        {
            lifecyclePlan.addAll( mojoExecutions );
        }
    }

    private Map<String, List<MojoExecution>> calculateLifecycleMappings( MavenSession session, String lifecyclePhase )
        throws LifecyclePhaseNotFoundException, PluginNotFoundException, PluginResolutionException,
        PluginDescriptorParsingException, CycleDetectedInPluginGraphException, MojoNotFoundException,
        InvalidPluginDescriptorException
    {
        /*
         * Determine the lifecycle that corresponds to the given phase.
         */

        Lifecycle lifecycle = phaseToLifecycleMap.get( lifecyclePhase );

        if ( lifecycle == null )
        {
            logger.info( "Invalid task '" + lifecyclePhase + "' : you must specify a valid lifecycle phase"
                + ", or a goal in the format <plugin-prefix>:<goal> or"
                + " <plugin-group-id>:<plugin-artifact-id>:<plugin-version>:<goal>" );
            throw new LifecyclePhaseNotFoundException( lifecyclePhase );
        }

        /*
         * Initialize mapping from lifecycle phase to bound mojos. The key set of this map denotes the phases the caller
         * is interested in, i.e. all phases up to and including the specified phase.
         */

        Map<String, List<MojoExecution>> lifecycleMappings = new LinkedHashMap<String, List<MojoExecution>>();

        for ( String phase : lifecycle.getPhases() )
        {
            List<MojoExecution> mojoExecutions = new ArrayList<MojoExecution>();

            // TODO: remove hard coding
            if ( phase.equals( "clean" ) )
            {
                Plugin plugin = new Plugin();
                plugin.setGroupId( "org.apache.maven.plugins" );
                plugin.setArtifactId( "maven-clean-plugin" );
                plugin.setVersion( "2.3" );
                mojoExecutions.add( new MojoExecution( plugin, "clean", "default-clean" ) );
            }

            lifecycleMappings.put( phase, mojoExecutions );

            if ( phase.equals( lifecyclePhase ) )
            {
                break;
            }
        }

        /*
         * Grab plugin executions that are bound to the selected lifecycle phases from project. The effective model of
         * the project already contains the plugin executions induced by the project's packaging type. Remember, all
         * phases of interest and only those are in the lifecyle mapping, if a phase has no value in the map, we are not
         * interested in any of the executions bound to it.
         */

        MavenProject project = session.getCurrentProject();

        for ( Plugin plugin : project.getBuild().getPlugins() )
        {
            for ( PluginExecution execution : plugin.getExecutions() )
            {
                // if the phase is specified then I don't have to go fetch the plugin yet and pull it down
                // to examine the phase it is associated to.
                if ( execution.getPhase() != null )
                {
                    List<MojoExecution> mojoExecutions = lifecycleMappings.get( execution.getPhase() );
                    if ( mojoExecutions != null )
                    {
                        for ( String goal : execution.getGoals() )
                        {
                            MojoExecution mojoExecution = new MojoExecution( plugin, goal, execution.getId() );
                            mojoExecutions.add( mojoExecution );
                        }
                    }
                }
                // if not then i need to grab the mojo descriptor and look at the phase that is specified
                else
                {
                    for ( String goal : execution.getGoals() )
                    {
                        MojoDescriptor mojoDescriptor =
                            pluginManager.getMojoDescriptor( plugin, goal, session.getLocalRepository(),
                                                             project.getPluginArtifactRepositories() );

                        List<MojoExecution> mojoExecutions = lifecycleMappings.get( mojoDescriptor.getPhase() );
                        if ( mojoExecutions != null )
                        {
                            MojoExecution mojoExecution = new MojoExecution( plugin, goal, execution.getId() );
                            mojoExecutions.add( mojoExecution );
                        }
                    }
                }
            }
        }

        return lifecycleMappings;
    }

    private void calculateForkedExecutions( MojoExecution mojoExecution, MavenSession session, MavenProject project,
                                            Collection<MojoDescriptor> alreadyForkedExecutions )
        throws MojoNotFoundException, PluginNotFoundException, PluginResolutionException,
        PluginDescriptorParsingException, CycleDetectedInPluginGraphException, NoPluginFoundForPrefixException,
        InvalidPluginDescriptorException, LifecyclePhaseNotFoundException, LifecycleNotFoundException
    {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        if ( !alreadyForkedExecutions.add( mojoDescriptor ) )
        {
            return;
        }

        PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();

        if ( StringUtils.isNotEmpty( mojoDescriptor.getExecutePhase() ) )
        {
            String forkedPhase = mojoDescriptor.getExecutePhase();

            Map<String, List<MojoExecution>> lifecycleMappings = calculateLifecycleMappings( session, forkedPhase );

            for ( List<MojoExecution> forkedExecutions : lifecycleMappings.values() )
            {
                for ( MojoExecution forkedExecution : forkedExecutions )
                {
                    if ( forkedExecution.getMojoDescriptor() == null )
                    {
                        MojoDescriptor forkedMojoDescriptor =
                            pluginManager.getMojoDescriptor( forkedExecution.getPlugin(), forkedExecution.getGoal(),
                                                             session.getLocalRepository(),
                                                             project.getPluginArtifactRepositories() );

                        forkedExecution.setMojoDescriptor( forkedMojoDescriptor );
                    }

                    populateMojoExecutionConfiguration( project, forkedExecution, false );
                }
            }

            String forkedLifecycle = mojoDescriptor.getExecuteLifecycle();

            if ( StringUtils.isNotEmpty( forkedLifecycle ) )
            {
                org.apache.maven.plugin.lifecycle.Lifecycle lifecycleOverlay;

                try
                {
                    lifecycleOverlay = pluginDescriptor.getLifecycleMapping( forkedLifecycle );
                }
                catch ( IOException e )
                {
                    throw new PluginDescriptorParsingException( pluginDescriptor.getPlugin(), e );
                }
                catch ( XmlPullParserException e )
                {
                    throw new PluginDescriptorParsingException( pluginDescriptor.getPlugin(), e );
                }

                if ( lifecycleOverlay == null )
                {
                    throw new LifecycleNotFoundException( forkedLifecycle );
                }

                for ( Phase phase : lifecycleOverlay.getPhases() )
                {
                    List<MojoExecution> forkedExecutions = lifecycleMappings.get( phase.getId() );
                    if ( forkedExecutions != null )
                    {
                        for ( Execution execution : phase.getExecutions() )
                        {
                            for ( String goal : execution.getGoals() )
                            {
                                MojoDescriptor forkedMojoDescriptor;

                                if ( goal.indexOf( ':' ) < 0 )
                                {
                                    forkedMojoDescriptor = pluginDescriptor.getMojo( goal );
                                    if ( forkedMojoDescriptor == null )
                                    {
                                        throw new MojoNotFoundException( goal, pluginDescriptor );
                                    }
                                }
                                else
                                {
                                    forkedMojoDescriptor = getMojoDescriptor( goal, session );
                                }

                                MojoExecution forkedExecution =
                                    new MojoExecution( forkedMojoDescriptor, mojoExecution.getExecutionId() );

                                Xpp3Dom forkedConfiguration = (Xpp3Dom) execution.getConfiguration();

                                forkedExecution.setConfiguration( forkedConfiguration );

                                populateMojoExecutionConfiguration( project, forkedExecution, true );

                                forkedExecutions.add( forkedExecution );
                            }
                        }

                        Xpp3Dom phaseConfiguration = (Xpp3Dom) phase.getConfiguration();
                        if ( phaseConfiguration != null )
                        {
                            for ( MojoExecution forkedExecution : forkedExecutions )
                            {
                                Xpp3Dom executionConfiguration = forkedExecution.getConfiguration();

                                Xpp3Dom mergedConfiguration =
                                    Xpp3Dom.mergeXpp3Dom( phaseConfiguration, executionConfiguration );

                                forkedExecution.setConfiguration( mergedConfiguration );
                            }
                        }
                    }
                }
            }

            for ( List<MojoExecution> forkedExecutions : lifecycleMappings.values() )
            {
                for ( MojoExecution forkedExecution : forkedExecutions )
                {
                    calculateForkedExecutions( forkedExecution, session, project, alreadyForkedExecutions );

                    mojoExecution.addForkedExecution( forkedExecution );
                }
            }
        }
        else if ( StringUtils.isNotEmpty( mojoDescriptor.getExecuteGoal() ) )
        {
            String forkedGoal = mojoDescriptor.getExecuteGoal();

            MojoDescriptor forkedMojoDescriptor = pluginDescriptor.getMojo( forkedGoal );
            if ( forkedMojoDescriptor == null )
            {
                throw new MojoNotFoundException( forkedGoal, pluginDescriptor );
            }

            MojoExecution forkedExecution = new MojoExecution( forkedMojoDescriptor, forkedGoal );

            populateMojoExecutionConfiguration( project, forkedExecution, true );

            calculateForkedExecutions( forkedExecution, session, project, alreadyForkedExecutions );

            mojoExecution.addForkedExecution( forkedExecution );
        }
    }

    private String executionDescription( MojoExecution me, MavenProject project )
    {
        PluginDescriptor pd = me.getMojoDescriptor().getPluginDescriptor();
        StringBuilder sb = new StringBuilder( 128 );
        sb.append( "Executing " + pd.getArtifactId() + "[" + pd.getVersion() + "]: " + me.getMojoDescriptor().getGoal() + " on " + project.getArtifactId() );        
        return sb.toString();
    }
        
    private void populateMojoExecutionConfiguration( MavenProject project, MojoExecution mojoExecution,
                                                     boolean allowPluginLevelConfig )
    {
        String g = mojoExecution.getGroupId();

        String a = mojoExecution.getArtifactId();

        Plugin plugin = project.getPlugin( g + ":" + a );

        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        if ( plugin != null && StringUtils.isNotEmpty( mojoExecution.getExecutionId() ) )
        {
            for ( PluginExecution e : plugin.getExecutions() )
            {
                if ( mojoExecution.getExecutionId().equals( e.getId() ) )
                {
                    Xpp3Dom executionConfiguration = (Xpp3Dom) e.getConfiguration();

                    Xpp3Dom mojoConfiguration = extractMojoConfiguration( executionConfiguration, mojoDescriptor );

                    mojoConfiguration = Xpp3Dom.mergeXpp3Dom( mojoExecution.getConfiguration(), mojoConfiguration );

                    /*
                     * The model only contains the default configuration for those goals that are present in the plugin
                     * execution. For goals invoked from the CLI or a forked execution, we need to grab the default
                     * parameter values explicitly.
                     */
                    if ( !e.getGoals().contains( mojoExecution.getGoal() ) )
                    {
                        Xpp3Dom defaultConfiguration = getMojoConfiguration( mojoDescriptor );

                        mojoConfiguration = Xpp3Dom.mergeXpp3Dom( mojoConfiguration, defaultConfiguration );
                    }

                    mojoExecution.setConfiguration( mojoConfiguration );

                    return;
                }
            }
        }

        if ( allowPluginLevelConfig )
        {
            Xpp3Dom defaultConfiguration = getMojoConfiguration( mojoDescriptor );

            Xpp3Dom mojoConfiguration = defaultConfiguration;

            if ( plugin != null && plugin.getConfiguration() != null )
            {
                Xpp3Dom pluginConfiguration = (Xpp3Dom) plugin.getConfiguration();
                pluginConfiguration = extractMojoConfiguration( pluginConfiguration, mojoDescriptor );
                mojoConfiguration = Xpp3Dom.mergeXpp3Dom( pluginConfiguration, defaultConfiguration, Boolean.TRUE );
            }

            mojoConfiguration = Xpp3Dom.mergeXpp3Dom( mojoExecution.getConfiguration(), mojoConfiguration );

            mojoExecution.setConfiguration( mojoConfiguration );
        }
    }

    /**
     * Extracts the configuration for a single mojo from the specified execution configuration by discarding any
     * non-applicable parameters. This is necessary because a plugin execution can have multiple goals with different
     * parametes whose default configurations are all aggregated into the execution configuration. However, the
     * underlying configurator will error out when trying to configure a mojo parameter that is specified in the
     * configuration but not present in the mojo instance.
     * 
     * @param executionConfiguration The configuration from the plugin execution, must not be {@code null}.
     * @param mojoDescriptor The descriptor for the mojo being configured, must not be {@code null}.
     * @return The configuration for the mojo, never {@code null}.
     */
    private Xpp3Dom extractMojoConfiguration( Xpp3Dom executionConfiguration, MojoDescriptor mojoDescriptor )
    {
        Xpp3Dom mojoConfiguration = new Xpp3Dom( executionConfiguration.getName() );

        Map<String, Parameter> mojoParameters = mojoDescriptor.getParameterMap();

        Map<String, String> aliases = new HashMap<String, String>();
        if ( mojoDescriptor.getParameters() != null )
        {
            for ( Parameter parameter : mojoDescriptor.getParameters() )
            {
                String alias = parameter.getAlias();
                if ( StringUtils.isNotEmpty( alias ) )
                {
                    aliases.put( alias, parameter.getName() );
                }
            }
        }

        for ( int i = 0; i < executionConfiguration.getChildCount(); i++ )
        {
            Xpp3Dom executionDom = executionConfiguration.getChild( i );
            String paramName = executionDom.getName();

            Xpp3Dom mojoDom;

            if ( mojoParameters.containsKey( paramName ) )
            {
                mojoDom = new Xpp3Dom( executionDom );
            }
            else if ( aliases.containsKey( paramName ) )
            {
                mojoDom = new Xpp3Dom( executionDom, aliases.get( paramName ) );
            }
            else
            {
                continue;
            }

            String implementation = mojoParameters.get( mojoDom.getName() ).getImplementation();
            if ( StringUtils.isNotEmpty( implementation ) )
            {
                mojoDom.setAttribute( "implementation", implementation );
            }

            mojoConfiguration.addChild( mojoDom );
        }

        return mojoConfiguration;
    }
   
    // org.apache.maven.plugins:maven-remote-resources-plugin:1.0:process
    MojoDescriptor getMojoDescriptor( String task, MavenSession session ) 
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException, CycleDetectedInPluginGraphException, MojoNotFoundException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException
    {        
        MavenProject project = session.getCurrentProject();
        
        String goal = null;
        
        Plugin plugin = null;

        StringTokenizer tok = new StringTokenizer( task, ":" );
        
        int numTokens = tok.countTokens();
        
        if ( numTokens == 4 )
        {
            // We have everything that we need
            //
            // org.apache.maven.plugins:maven-remote-resources-plugin:1.0:process
            //
            // groupId
            // artifactId
            // version
            // goal
            //
            plugin = new Plugin();
            plugin.setGroupId( tok.nextToken() );
            plugin.setArtifactId( tok.nextToken() );
            plugin.setVersion( tok.nextToken() );
            goal = tok.nextToken();
            
        }
        else if ( numTokens == 3 )
        {
            // We have everything that we need except the version
            //
            // org.apache.maven.plugins:maven-remote-resources-plugin:???:process
            //
            // groupId
            // artifactId
            // ???
            // goal
            //
            plugin = new Plugin();
            plugin.setGroupId( tok.nextToken() );
            plugin.setArtifactId( tok.nextToken() );
            goal = tok.nextToken();
        }
        else if ( numTokens == 2 )
        {
            // We have a prefix and goal
            //
            // idea:idea
            //
            String prefix = tok.nextToken();
            goal = tok.nextToken();

            // This is the case where someone has executed a single goal from the command line
            // of the form:
            //
            // mvn remote-resources:process
            //
            // From the metadata stored on the server which has been created as part of a standard
            // Maven plugin deployment we will find the right PluginDescriptor from the remote
            // repository.
            
            plugin = findPluginForPrefix( prefix, session );
        }

        injectPluginDeclarationFromProject( plugin, project );

        // If there is no version to be found then we need to look in the repository metadata for
        // this plugin and see what's specified as the latest release.
        //
        if ( plugin.getVersion() == null )
        {
            resolvePluginVersion( plugin, session.getLocalRepository(), project.getPluginArtifactRepositories() );
        }
        
        return pluginManager.getMojoDescriptor( plugin, goal, session.getLocalRepository(), project.getPluginArtifactRepositories() );
    }

    private void resolvePluginVersion( Plugin plugin, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws PluginNotFoundException
    {
        File artifactMetadataFile = null;
        
        String localPath; 
        
        // Search in remote repositories for a (released) version.
        //
        // maven-metadata-{central|nexus|...}.xml 
        //
        //TODO: we should cycle through the repositories but take the repository which actually
        // satisfied the prefix.
        for ( ArtifactRepository repository : remoteRepositories )
        {
            localPath = plugin.getGroupId().replace( '.', '/' ) + "/" + plugin.getArtifactId() + "/maven-metadata-" + repository.getId() + ".xml";

            artifactMetadataFile = new File( localRepository.getBasedir(), localPath );

            if ( !artifactMetadataFile.exists() /* || user requests snapshot updates */)
            {
                try
                {
                    String remotePath = plugin.getGroupId().replace( '.', '/' ) + "/" + plugin.getArtifactId() + "/maven-metadata.xml";

                    repositorySystem.retrieve( repository, artifactMetadataFile, remotePath, null );
                }
                catch ( TransferFailedException e )
                {
                    continue;
                }
                catch ( ResourceDoesNotExistException e )
                {
                    continue;
                }
            }

            break;
        }

        // Search in the local repositiory for a (development) version
        //
        // maven-metadata-local.xml
        //
        if ( artifactMetadataFile == null || !artifactMetadataFile.exists() )
        {
            localPath =
                plugin.getGroupId().replace( '.', '/' ) + "/" + plugin.getArtifactId() + "/maven-metadata-"
                    + localRepository.getId() + ".xml";

            artifactMetadataFile = new File( localRepository.getBasedir(), localPath );
        }

        if ( artifactMetadataFile.exists() )
        {
            logger.debug( "Extracting version for plugin " + plugin.getKey() + " from " + artifactMetadataFile );

            try
            {
                Metadata pluginMetadata = readMetadata( artifactMetadataFile );

                if ( pluginMetadata.getVersioning() != null )
                {
                    String release = pluginMetadata.getVersioning().getRelease();

                    if ( StringUtils.isNotEmpty( release ) )
                    {
                        plugin.setVersion( release );
                    }
                    else
                    {
                        String latest = pluginMetadata.getVersioning().getLatest();

                        if ( StringUtils.isNotEmpty( latest ) )
                        {
                            plugin.setVersion( latest );
                        }
                    }
                }
            }
            catch ( RepositoryMetadataReadException e )
            {
                logger.warn( "Error reading plugin metadata: ", e );
            }
        }

        if ( StringUtils.isEmpty( plugin.getVersion() ) )
        {
            throw new PluginNotFoundException( plugin, remoteRepositories );
        }
    }

    private void injectPluginDeclarationFromProject( Plugin plugin, MavenProject project )
    {
        Plugin pluginInPom = findPlugin( plugin, project.getBuildPlugins() );

        if ( pluginInPom == null && project.getPluginManagement() != null )
        {
            pluginInPom = findPlugin( plugin, project.getPluginManagement().getPlugins() );
        }

        if ( pluginInPom != null )
        {
            if ( plugin.getVersion() == null )
            {
                plugin.setVersion( pluginInPom.getVersion() );
            }

            plugin.setDependencies( new ArrayList<Dependency>( pluginInPom.getDependencies() ) );
        }
    }

    private Plugin findPlugin( Plugin plugin, Collection<Plugin> plugins )
    {
        for ( Plugin p : plugins )
        {
            if ( p.getKey().equals( plugin.getKey() ) )
            {
                return p;
            }
        }
        return null;
    }

    public void initialize()
        throws InitializationException
    {
        lifecycleMap = new HashMap<String,Lifecycle>();
        
        // If people are going to make their own lifecycles then we need to tell people how to namespace them correctly so
        // that they don't interfere with internally defined lifecycles.

        phaseToLifecycleMap = new HashMap<String,Lifecycle>();

        for ( Lifecycle lifecycle : lifecycles )
        {                        
            for ( String phase : lifecycle.getPhases() )
            {                
                // The first definition wins.
                if ( !phaseToLifecycleMap.containsKey( phase ) )
                {
                    phaseToLifecycleMap.put( phase, lifecycle );
                }
            }
            
            lifecycleMap.put( lifecycle.getId(), lifecycle );
        }
    }   
        
    // These methods deal with construction intact Plugin object that look like they come from a standard
    // <plugin/> block in a Maven POM. We have to do some wiggling to pull the sources of information
    // together and this really shows the problem of constructing a sensible default configuration but
    // it's all encapsulated here so it appears normalized to the POM builder.
    
    // We are going to take the project packaging and find all plugin in the default lifecycle and create
    // fully populated Plugin objects, including executions with goals and default configuration taken
    // from the plugin.xml inside a plugin.
    //
    public Set<Plugin> getPluginsBoundByDefaultToAllLifecycles( String packaging )
    {
        LifecycleMapping lifecycleMappingForPackaging = lifecycleMappings.get( packaging );

        if ( lifecycleMappingForPackaging == null )
        {
            return null;
        }

        Map<Plugin, Plugin> plugins = new LinkedHashMap<Plugin, Plugin>();

        for ( Lifecycle lifecycle : lifecycles )
        {
            org.apache.maven.lifecycle.mapping.Lifecycle lifecycleConfiguration =
                lifecycleMappingForPackaging.getLifecycles().get( lifecycle.getId() );

            if ( lifecycleConfiguration != null )
            {
                Map<String, String> lifecyclePhasesForPackaging = lifecycleConfiguration.getPhases();

                // These are of the form:
                //
                // compile -> org.apache.maven.plugins:maven-compiler-plugin:compile[,gid:aid:goal,...]
                //
                for ( Map.Entry<String, String> goalsForLifecyclePhase : lifecyclePhasesForPackaging.entrySet() )
                {
                    String phase = goalsForLifecyclePhase.getKey();
                    String goals = goalsForLifecyclePhase.getValue();
                    if ( goals != null )
                    {
                        parseLifecyclePhaseDefinitions( plugins, phase, goals );
                    }
                }
            }
            else if ( lifecycle.getDefaultPhases() != null )
            {
                for ( String goals : lifecycle.getDefaultPhases() )
                {
                    parseLifecyclePhaseDefinitions( plugins, null, goals );
                }
            }        
        }

        return plugins.keySet();
    }        

    private void parseLifecyclePhaseDefinitions( Map<Plugin, Plugin> plugins, String phase, String goals )
    {
        for ( StringTokenizer tok = new StringTokenizer( goals, "," ); tok.hasMoreTokens(); )
        {
            // either <groupId>:<artifactId>:<goal> or <groupId>:<artifactId>:<version>:<goal>
            String goal = tok.nextToken().trim();
            String[] p = StringUtils.split( goal, ":" );

            PluginExecution execution = new PluginExecution();
            execution.setId( "default-" + p[p.length - 1] );
            execution.setPhase( phase );
            execution.getGoals().add( p[p.length - 1] );

            Plugin plugin = new Plugin();
            plugin.setGroupId( p[0] );
            plugin.setArtifactId( p[1] );
            if ( p.length >= 4 )
            {
                plugin.setVersion( p[2] );
            }

            Plugin existing = plugins.get( plugin );
            if ( existing != null )
            {
                plugin = existing;
            }
            else
            {
                plugins.put( plugin, plugin );
            }

            plugin.getExecutions().add( execution );
        }
    }
    
    private void populateDefaultConfigurationForPlugin( Plugin plugin, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories ) 
        throws LifecycleExecutionException
    {
        if ( plugin.getVersion() == null )
        {
            try
            {
                resolvePluginVersion( plugin, localRepository, remoteRepositories );
            }
            catch ( PluginNotFoundException e )
            {
                throw new LifecycleExecutionException( "Error resolving version for plugin " + plugin.getKey(), e );
            }
        }

        for( PluginExecution pluginExecution : plugin.getExecutions() )
        {
            for( String goal : pluginExecution.getGoals() )
            {
                Xpp3Dom dom = getDefaultPluginConfiguration( plugin, goal, localRepository, remoteRepositories );
                pluginExecution.setConfiguration( Xpp3Dom.mergeXpp3Dom( (Xpp3Dom) pluginExecution.getConfiguration(), dom, Boolean.TRUE ) );
            }
        }
    }
    
    public void populateDefaultConfigurationForPlugins( Collection<Plugin> plugins, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories ) 
        throws LifecycleExecutionException
    {
        for( Plugin plugin : plugins )
        {            
            populateDefaultConfigurationForPlugin( plugin, localRepository, remoteRepositories );
        }
    }    
    
    private Xpp3Dom getDefaultPluginConfiguration( Plugin plugin, String goal, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories ) 
        throws LifecycleExecutionException
    {
        MojoDescriptor mojoDescriptor;
        
        try
        {
            mojoDescriptor = pluginManager.getMojoDescriptor( plugin, goal, localRepository, remoteRepositories );
        }
        catch ( PluginNotFoundException e )
        {
            throw new LifecycleExecutionException( "Error getting default plugin information: ", e );
        }
        catch ( PluginResolutionException e )
        {
            throw new LifecycleExecutionException( "Error getting default plugin information: ", e );
        }
        catch ( PluginDescriptorParsingException e )
        {
            throw new LifecycleExecutionException( "Error getting default plugin information: ", e );
        }
        catch ( CycleDetectedInPluginGraphException e )
        {
            throw new LifecycleExecutionException( "Error getting default plugin information: ", e );
        }
        catch ( MojoNotFoundException e )
        {
            throw new LifecycleExecutionException( "Error getting default plugin information: ", e );
        }
        catch ( InvalidPluginDescriptorException e )
        {
            throw new LifecycleExecutionException( "Error getting default plugin information: ", e );
        } 
        
        return getMojoConfiguration( mojoDescriptor );
    }
    
    public Xpp3Dom getMojoConfiguration( MojoDescriptor mojoDescriptor )
    {
        return convert( mojoDescriptor );
    }
        
    Xpp3Dom convert( MojoDescriptor mojoDescriptor  )
    {
        Xpp3Dom dom = new Xpp3Dom( "configuration" );

        PlexusConfiguration c = mojoDescriptor.getMojoConfiguration();

        PlexusConfiguration[] ces = c.getChildren();

        if ( ces != null )
        {
            for ( PlexusConfiguration ce : ces )
            {
                String value = ce.getValue( null );
                String defaultValue = ce.getAttribute( "default-value", null );
                if ( value != null || defaultValue != null )
                {
                    Xpp3Dom e = new Xpp3Dom( ce.getName() );
                    e.setValue( value );
                    if ( defaultValue != null )
                    {
                        e.setAttribute( "default-value", defaultValue );
                    }
                    dom.addChild( e );
                }
            }
        }

        return dom;
    }
                   
    private Map<String,Plugin> pluginPrefixes = new HashMap<String,Plugin>();
    
    //TODO: take repo mans into account as one may be aggregating prefixes of many
    //TODO: collect at the root of the repository, read the one at the root, and fetch remote if something is missing
    //      or the user forces the issue
    public Plugin findPluginForPrefix( String prefix, MavenSession session )
        throws NoPluginFoundForPrefixException
    {        
        // [prefix]:[goal]
        
        Plugin plugin = pluginPrefixes.get( prefix );
        
        if ( plugin != null )
        {
            return plugin;
        }

        MavenProject project = session.getCurrentProject();

        if ( project != null )
        {
            for ( Plugin buildPlugin : project.getBuildPlugins() )
            {
                try
                {
                    PluginDescriptor pluginDescriptor =
                        pluginManager.loadPlugin( buildPlugin, session.getLocalRepository(),
                                                  project.getPluginArtifactRepositories() );

                    if ( prefix.equals( pluginDescriptor.getGoalPrefix() ) )
                    {
                        Plugin p = new Plugin();
                        p.setGroupId( buildPlugin.getGroupId() );
                        p.setArtifactId( buildPlugin.getArtifactId() );
                        pluginPrefixes.put( prefix, p );
                        return p;
                    }
                }
                catch ( Exception e )
                {
                    logger.debug( "Failed to retrieve plugin descriptor for " + buildPlugin, e );
                }
            }
        }

        // Process all plugin groups in the local repository first to see if we get a hit. A developer may have been 
        // developing a plugin locally and installing.
        //
        for ( String pluginGroup : session.getPluginGroups() )
        {            
            String localPath = pluginGroup.replace( '.', '/' ) + "/" + "maven-metadata-" + session.getLocalRepository().getId() + ".xml";

            File destination = new File( session.getLocalRepository().getBasedir(), localPath );

            if ( destination.exists() )
            {                
                processPluginGroupMetadata( pluginGroup, destination, pluginPrefixes );    
                
                plugin = pluginPrefixes.get( prefix );
                
                if ( plugin != null )
                {
                    return plugin;
                }                
            }
        }
        
        // Process all the remote repositories.
        //
        for ( String pluginGroup : session.getPluginGroups() )
        {                
            for ( ArtifactRepository repository : session.getCurrentProject().getPluginArtifactRepositories() )
            {
                try
                {
                    String localPath = pluginGroup.replace( '.', '/' ) + "/" + "maven-metadata-" + repository.getId() + ".xml";
                    
                    File destination = new File( session.getLocalRepository().getBasedir(), localPath );
                    
                    String remotePath = pluginGroup.replace( '.', '/' ) + "/" + "maven-metadata.xml";

                    repositorySystem.retrieve( repository, destination, remotePath, session.getRequest().getTransferListener() );
                    
                    processPluginGroupMetadata( pluginGroup, destination, pluginPrefixes );
                    
                    plugin = pluginPrefixes.get( prefix );
                    
                    if ( plugin != null )
                    {
                        return plugin;
                    }                                        
                }
                catch ( TransferFailedException e )
                {
                    continue;
                }
                catch ( ResourceDoesNotExistException e )
                {
                    continue;
                }
            }

        }            
                            
        throw new NoPluginFoundForPrefixException( prefix, session.getLocalRepository(), session.getCurrentProject().getPluginArtifactRepositories() );
    }  
    
    // Keep track of the repository that provided the prefix mapping
    //
    private class PluginPrefix
    {
        private Plugin plugin;
        
        private ArtifactRepository repository;

        public PluginPrefix( Plugin plugin, ArtifactRepository repository )
        {
            this.plugin = plugin;
            this.repository = repository;
        }
    }
    
    
    private void processPluginGroupMetadata( String pluginGroup, File pluginGroupMetadataFile, Map<String,Plugin> pluginPrefixes )
    {
        try
        {
            Metadata pluginGroupMetadata = readMetadata( pluginGroupMetadataFile );

            List<org.apache.maven.artifact.repository.metadata.Plugin> plugins = pluginGroupMetadata.getPlugins();

            if ( plugins != null )
            {
                for ( org.apache.maven.artifact.repository.metadata.Plugin metadataPlugin : plugins )
                {
                    Plugin p = new Plugin();
                    p.setGroupId( pluginGroup );
                    p.setArtifactId( metadataPlugin.getArtifactId() );
                    pluginPrefixes.put( metadataPlugin.getPrefix(), p );
                }
            }
        }
        catch ( RepositoryMetadataReadException e )
        {
            logger.warn( "Error reading plugin group metadata: ", e );
        }
    }
    
    protected Metadata readMetadata( File mappingFile )
        throws RepositoryMetadataReadException
    {
        Metadata result;

        Reader reader = null;
        try
        {
            reader = ReaderFactory.newXmlReader( mappingFile );

            MetadataXpp3Reader mappingReader = new MetadataXpp3Reader();

            result = mappingReader.read( reader, false );
        }
        catch ( FileNotFoundException e )
        {
            throw new RepositoryMetadataReadException( "Cannot read metadata from '" + mappingFile + "'", e );
        }
        catch ( IOException e )
        {
            throw new RepositoryMetadataReadException( "Cannot read metadata from '" + mappingFile + "': " + e.getMessage(), e );
        }
        catch ( XmlPullParserException e )
        {
            throw new RepositoryMetadataReadException( "Cannot read metadata from '" + mappingFile + "': " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( reader );
        }
        return result;
    }
    
    // These are checks that should be available in real time to IDEs

    /*
    checkRequiredMavenVersion( plugin, localRepository, project.getRemoteArtifactRepositories() );
        // Validate against non-editable (@readonly) parameters, to make sure users aren't trying to override in the POM.
        //validatePomConfiguration( mojoDescriptor, pomConfiguration );            
        //checkDeprecatedParameters( mojoDescriptor, pomConfiguration );
        //checkRequiredParameters( mojoDescriptor, pomConfiguration, expressionEvaluator );        
    
    public void checkRequiredMavenVersion( Plugin plugin, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws PluginVersionResolutionException, InvalidPluginException
    {
        // if we don't have the required Maven version, then ignore an update
        if ( ( pluginProject.getPrerequisites() != null ) && ( pluginProject.getPrerequisites().getMaven() != null ) )
        {
            DefaultArtifactVersion requiredVersion = new DefaultArtifactVersion( pluginProject.getPrerequisites().getMaven() );

            if ( runtimeInformation.getApplicationInformation().getVersion().compareTo( requiredVersion ) < 0 )
            {
                throw new PluginVersionResolutionException( plugin.getGroupId(), plugin.getArtifactId(), "Plugin requires Maven version " + requiredVersion );
            }
        }
    }
    
   private void checkDeprecatedParameters( MojoDescriptor mojoDescriptor, PlexusConfiguration extractedMojoConfiguration )
        throws PlexusConfigurationException
    {
        if ( ( extractedMojoConfiguration == null ) || ( extractedMojoConfiguration.getChildCount() < 1 ) )
        {
            return;
        }

        List<Parameter> parameters = mojoDescriptor.getParameters();

        if ( ( parameters != null ) && !parameters.isEmpty() )
        {
            for ( Parameter param : parameters )
            {
                if ( param.getDeprecated() != null )
                {
                    boolean warnOfDeprecation = false;
                    PlexusConfiguration child = extractedMojoConfiguration.getChild( param.getName() );

                    if ( ( child != null ) && ( child.getValue() != null ) )
                    {
                        warnOfDeprecation = true;
                    }
                    else if ( param.getAlias() != null )
                    {
                        child = extractedMojoConfiguration.getChild( param.getAlias() );
                        if ( ( child != null ) && ( child.getValue() != null ) )
                        {
                            warnOfDeprecation = true;
                        }
                    }

                    if ( warnOfDeprecation )
                    {
                        StringBuilder buffer = new StringBuilder( 128 );
                        buffer.append( "In mojo: " ).append( mojoDescriptor.getGoal() ).append( ", parameter: " ).append( param.getName() );

                        if ( param.getAlias() != null )
                        {
                            buffer.append( " (alias: " ).append( param.getAlias() ).append( ")" );
                        }

                        buffer.append( " is deprecated:" ).append( "\n\n" ).append( param.getDeprecated() ).append( "\n" );

                        logger.warn( buffer.toString() );
                    }
                }
            }
        }
    }    
    
   private void checkRequiredParameters( MojoDescriptor goal, PlexusConfiguration configuration, ExpressionEvaluator expressionEvaluator )
        throws PluginConfigurationException
    {
        // TODO: this should be built in to the configurator, as we presently double process the expressions

        List<Parameter> parameters = goal.getParameters();

        if ( parameters == null )
        {
            return;
        }

        List<Parameter> invalidParameters = new ArrayList<Parameter>();

        for ( int i = 0; i < parameters.size(); i++ )
        {
            Parameter parameter = parameters.get( i );

            if ( parameter.isRequired() )
            {
                // the key for the configuration map we're building.
                String key = parameter.getName();

                Object fieldValue = null;
                String expression = null;
                PlexusConfiguration value = configuration.getChild( key, false );
                try
                {
                    if ( value != null )
                    {
                        expression = value.getValue( null );

                        fieldValue = expressionEvaluator.evaluate( expression );

                        if ( fieldValue == null )
                        {
                            fieldValue = value.getAttribute( "default-value", null );
                        }
                    }

                    if ( ( fieldValue == null ) && StringUtils.isNotEmpty( parameter.getAlias() ) )
                    {
                        value = configuration.getChild( parameter.getAlias(), false );
                        if ( value != null )
                        {
                            expression = value.getValue( null );
                            fieldValue = expressionEvaluator.evaluate( expression );
                            if ( fieldValue == null )
                            {
                                fieldValue = value.getAttribute( "default-value", null );
                            }
                        }
                    }
                }
                catch ( ExpressionEvaluationException e )
                {
                    throw new PluginConfigurationException( goal.getPluginDescriptor(), e.getMessage(), e );
                }

                // only mark as invalid if there are no child nodes
                if ( ( fieldValue == null ) && ( ( value == null ) || ( value.getChildCount() == 0 ) ) )
                {
                    parameter.setExpression( expression );
                    invalidParameters.add( parameter );
                }
            }
        }

        if ( !invalidParameters.isEmpty() )
        {
            throw new PluginParameterException( goal, invalidParameters );
        }
    }

    private void validatePomConfiguration( MojoDescriptor goal, PlexusConfiguration pomConfiguration )
        throws PluginConfigurationException
    {
        List<Parameter> parameters = goal.getParameters();

        if ( parameters == null )
        {
            return;
        }

        for ( int i = 0; i < parameters.size(); i++ )
        {
            Parameter parameter = parameters.get( i );

            // the key for the configuration map we're building.
            String key = parameter.getName();

            PlexusConfiguration value = pomConfiguration.getChild( key, false );

            if ( ( value == null ) && StringUtils.isNotEmpty( parameter.getAlias() ) )
            {
                key = parameter.getAlias();
                value = pomConfiguration.getChild( key, false );
            }

            if ( value != null )
            {
                // Make sure the parameter is either editable/configurable, or else is NOT specified in the POM
                if ( !parameter.isEditable() )
                {
                    StringBuilder errorMessage = new StringBuilder( 128 ).append( "ERROR: Cannot override read-only parameter: " );
                    errorMessage.append( key );
                    errorMessage.append( " in goal: " ).append( goal.getFullGoalName() );

                    throw new PluginConfigurationException( goal.getPluginDescriptor(), errorMessage.toString() );
                }

                String deprecated = parameter.getDeprecated();
                if ( StringUtils.isNotEmpty( deprecated ) )
                {
                    logger.warn( "DEPRECATED [" + parameter.getName() + "]: " + deprecated );
                }
            }
        }
    }    
    
    */    
}
