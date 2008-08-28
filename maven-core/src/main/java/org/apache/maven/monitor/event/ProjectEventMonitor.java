package org.apache.maven.monitor.event;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.codehaus.plexus.logging.Logger;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.MavenProject;

/**
 * Notifies the ProjectBuilder of events.
 */
public class ProjectEventMonitor
    extends AbstractSelectiveEventMonitor
{

    private static final String[] EVENTS = {MavenEvents.MOJO_EXECUTION};

    private MavenProjectBuilder builder;

    public ProjectEventMonitor( MavenProjectBuilder builder )
    {
        super( EVENTS, EVENTS, MavenEvents.NO_EVENTS );

        this.builder = builder;
    }

    protected void doStartEvent( MavenEvent event, String target, long time )
    {
        Object obj = event.getSource();
        if ( !target.startsWith( "clean") && obj != null && obj instanceof MavenProject )
        {
            builder.prepareProject((MavenProject)obj);
        }
    }

    protected void doEndEvent( MavenEvent event, String target, long timestamp )
    {
        Object obj = event.getSource();
        if ( target.startsWith( "clean") && obj != null && obj instanceof MavenProject )
        {
            builder.cleanProject((MavenProject)obj);
        }
    }

}