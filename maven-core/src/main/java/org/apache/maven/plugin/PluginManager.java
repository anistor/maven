package org.apache.maven.plugin;

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

import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

/**
 * @author Jason van Zyl
 */
public interface PluginManager
{
    // igorf: Way too many declared exceptions!
    PluginDescriptor loadPlugin( Plugin plugin, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException, CycleDetectedInPluginGraphException, InvalidPluginDescriptorException;

    // igorf: Way too many declared exceptions!
    MojoDescriptor getMojoDescriptor( String groupId, String artifactId, String version, String goal, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException, CycleDetectedInPluginGraphException, MojoNotFoundException, InvalidPluginDescriptorException;

    // igorf: Way too many declared exceptions!
    MojoDescriptor getMojoDescriptor( Plugin plugin, String goal, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException, CycleDetectedInPluginGraphException, MojoNotFoundException, InvalidPluginDescriptorException;

    void executeMojo( MavenSession session, MojoExecution execution )
        throws MojoFailureException, MojoExecutionException, PluginConfigurationException, PluginManagerException;

    ClassRealm getPluginRealm( MavenSession session, PluginDescriptor pluginDescriptor ) 
        throws PluginManagerException;
}