package org.apache.maven.plugin;

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
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id$
 */
public interface PluginManager
{
    String ROLE = PluginManager.class.getName();

    void executeMojo( MavenSession session, GoalInstance buildStep )
        throws MojoExecutionException, PluginManagerException, ArtifactResolutionException;

    PluginDescriptor verifyPlugin( String prefix );

    PluginDescriptor verifyPlugin( String groupId, String artifactId, String version, MavenProject project,
                                   ArtifactRepository localRepository )
        throws ArtifactResolutionException, PluginManagerException;
}