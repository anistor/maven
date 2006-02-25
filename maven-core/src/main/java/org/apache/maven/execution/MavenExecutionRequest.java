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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.monitor.event.EventMonitor;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.events.TransferListener;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public interface MavenExecutionRequest
{
    ArtifactRepository getLocalRepository();

    List getGoals();

    Settings getSettings();

    String getBaseDirectory();

    boolean isRecursive();

    boolean isInteractive();

    boolean isReactorActive();

    String getPomFile();

    String getFailureBehavior();

    Properties getProperties();

    Date getStartTime();

    boolean isShowErrors();

    List getEventMonitors();

    List getActiveProfiles();

    List getInactiveProfiles();

    TransferListener getTransferListener();

    int getLoggingLevel();

    boolean isDefaultEventMonitorActive();

    // ----------------------------------------------------------------------
    // Logging
    // ----------------------------------------------------------------------

    static final int LOGGING_LEVEL_DEBUG = 0;

    static final int LOGGING_LEVEL_INFO = 1;

    static final int LOGGING_LEVEL_WARN = 2;

    static final int LOGGING_LEVEL_ERROR = 3;

    static final int LOGGING_LEVEL_FATAL = 4;

    static final int LOGGING_LEVEL_DISABLE = 5;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    MavenExecutionRequest setBasedir( File basedir );

    MavenExecutionRequest setSettings( Settings settings );

    MavenExecutionRequest setStartTime( Date start );

    MavenExecutionRequest setGoals( List goals );

    MavenExecutionRequest setLocalRepository( ArtifactRepository localRepository );

    MavenExecutionRequest setProperties( Properties properties );

    MavenExecutionRequest setFailureBehavior( String failureBehavior );

    MavenExecutionRequest setSession( MavenSession session );

    MavenExecutionRequest addActiveProfile( String profile );

    MavenExecutionRequest addInactiveProfile( String profile );

    MavenExecutionRequest addActiveProfiles( List profiles );

    MavenExecutionRequest addInactiveProfiles( List profiles );

    MavenExecutionRequest addEventMonitor( EventMonitor monitor );

    MavenExecutionRequest setReactorActive( boolean reactorActive );

    MavenExecutionRequest setPomFile( String pomFilename );

    MavenExecutionRequest setRecursive( boolean recursive );

    MavenExecutionRequest setShowErrors( boolean showErrors );

    MavenExecutionRequest setInteractive( boolean interactive );

    MavenExecutionRequest setTransferListener( TransferListener transferListener );

    MavenExecutionRequest setLoggingLevel( int loggingLevel );

    MavenExecutionRequest activateDefaultEventMonitor();
}
