package org.apache.maven.execution;

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

import org.apache.maven.monitor.event.EventMonitor;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 * @todo merge Settings,RuntimeInfo,MavenSession into this. make adapters for everything
 */
public interface MavenExecutionRequest
{
    // ----------------------------------------------------------------------
    // Logging
    // ----------------------------------------------------------------------

    static final int LOGGING_LEVEL_DEBUG = Logger.LEVEL_DEBUG;

    static final int LOGGING_LEVEL_INFO = Logger.LEVEL_INFO;

    static final int LOGGING_LEVEL_WARN = Logger.LEVEL_WARN;

    static final int LOGGING_LEVEL_ERROR = Logger.LEVEL_ERROR;

    static final int LOGGING_LEVEL_FATAL = Logger.LEVEL_FATAL;

    static final int LOGGING_LEVEL_DISABLED = Logger.LEVEL_DISABLED;

    // ----------------------------------------------------------------------
    // Reactor Failure Mode
    // ----------------------------------------------------------------------

    static final String REACTOR_FAIL_FAST = ReactorManager.FAIL_FAST;

    static final String REACTOR_FAIL_AT_END = ReactorManager.FAIL_AT_END;

    static final String REACTOR_FAIL_NEVER = ReactorManager.FAIL_NEVER;

    // ----------------------------------------------------------------------
    // Artifactr repository policies
    // ----------------------------------------------------------------------

    static final String CHECKSUM_POLICY_FAIL = ArtifactRepositoryPolicy.CHECKSUM_POLICY_FAIL;

    static final String CHECKSUM_POLICY_WARN = ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    // Base directory

    MavenExecutionRequest setBasedir( File basedir );

    String getBaseDirectory();

    // Settings
    MavenExecutionRequest setSettings( Settings settings );

    Settings getSettings();

    // Timing (remove this)
    MavenExecutionRequest setStartTime( Date start );

    Date getStartTime();

    // Goals
    MavenExecutionRequest setGoals( List goals );

    List getGoals();

    // Properties
    MavenExecutionRequest setProperties( Properties properties );

    Properties getProperties();

    // Reactor
    MavenExecutionRequest setReactorFailureBehavior( String failureBehavior );

    String getReactorFailureBehavior();

    MavenExecutionRequest setUseReactor( boolean useReactor );

    boolean useReactor();

    // Event monitors
    MavenExecutionRequest addEventMonitor( EventMonitor monitor );

    List getEventMonitors();

    // Pom
    MavenExecutionRequest setPomFile( String pomFilename );

    String getPomFile();

    // Errors
    MavenExecutionRequest setShowErrors( boolean showErrors );

    boolean isShowErrors();

    // Transfer listeners
    MavenExecutionRequest setTransferListener( TransferListener transferListener );

    TransferListener getTransferListener();

    // Logging
    MavenExecutionRequest setLoggingLevel( int loggingLevel );

    int getLoggingLevel();

    // Update snapshots
    MavenExecutionRequest setUpdateSnapshots( boolean updateSnapshots );

    boolean isUpdateSnapshots();

    // Checksum policy
    MavenExecutionRequest setGlobalChecksumPolicy( String globalChecksumPolicy );

    String getGlobalChecksumPolicy();

    // ----------------------------------------------------------------------------
    // Settings equivalents
    // ----------------------------------------------------------------------------

    // Local repository

    MavenExecutionRequest setLocalRepositoryPath( String localRepository );

    MavenExecutionRequest setLocalRepositoryPath( File localRepository );

    File getLocalRepositoryPath();

    MavenExecutionRequest setLocalRepository( ArtifactRepository repository );

    ArtifactRepository getLocalRepository();

    // Interactive
    MavenExecutionRequest setInteractiveMode( boolean interactive );

    boolean isInteractiveMode();

    // Offline
    MavenExecutionRequest setOffline( boolean offline );

    boolean isOffline();

    // Profiles
    List getProfiles();

    MavenExecutionRequest setProfiles( List profiles );

    MavenExecutionRequest addActiveProfile( String profile );

    MavenExecutionRequest addActiveProfiles( List profiles );

    List getActiveProfiles();

    MavenExecutionRequest addInactiveProfile( String profile );

    MavenExecutionRequest addInactiveProfiles( List profiles );

    List getInactiveProfiles();

    // Proxies
    List getProxies();

    MavenExecutionRequest setProxies( List proxies );

    // Servers
    List getServers();

    MavenExecutionRequest setServers( List servers );

    // Mirrors
    List getMirrors();

    MavenExecutionRequest setMirrors( List mirrors );

    // Plugin groups
    List getPluginGroups();

    MavenExecutionRequest setPluginGroups( List pluginGroups );

    // Plugin registry
    boolean isUsePluginRegistry();

    MavenExecutionRequest setUsePluginRegistry( boolean usePluginRegistry );
}
