package org.apache.maven.execution;

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

import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.List;

/** @author Jason van Zyl */
public class DefaultMavenExecutionResult
    implements MavenExecutionResult
{
    private List exceptions;

    private MavenProject mavenProject;

    private ReactorManager reactorManager;

    public DefaultMavenExecutionResult( List exceptions )
    {
        this.exceptions = exceptions;
    }

    public DefaultMavenExecutionResult( ReactorManager reactorManager )
    {
        this.reactorManager = reactorManager;
    }

    public DefaultMavenExecutionResult( List exceptions,
                                        ReactorManager reactorManager )
    {
        this.reactorManager = reactorManager;
        this.exceptions = exceptions;
    }

    public DefaultMavenExecutionResult( MavenProject project,
                                        List exceptions )
    {
        this.mavenProject = project;
        this.exceptions = exceptions;
    }

    public MavenProject getMavenProject()
    {
        if ( reactorManager != null )
        {
            return reactorManager.getTopLevelProject();
        }

        return mavenProject;
    }

    public ReactorManager getReactorManager()
    {
        return reactorManager;
    }

    public List getExceptions()
    {
        return exceptions;
    }

    public void addException( Throwable t )
    {
        if ( exceptions == null )
        {
            exceptions = new ArrayList();
        }

        exceptions.add( t );
    }

    public boolean hasExceptions()
    {
        return (exceptions != null && exceptions.size() > 0 );
    }
}
