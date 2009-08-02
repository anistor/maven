package org.apache.maven.artifact.resolver;

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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.wagon.events.TransferListener;

/**
 * @author Jason van Zyl
 */
// Just hide the one method we want behind the RepositorySystem interface.
public interface ArtifactResolver
{
    ArtifactResolutionResult collect( ArtifactResolutionRequest request );

    ArtifactResolutionResult resolve( ArtifactResolutionRequest request );

    // The rest is deprecated

    // USED BY MAVEN ASSEMBLY PLUGIN 2.2-beta-2 
    @Deprecated
    String ROLE = ArtifactResolver.class.getName();

    // USED BY SUREFIRE
    @Deprecated
    ArtifactResolutionResult resolveTransitively( Set<Artifact> artifacts, Artifact originatingArtifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories,
                                                  ArtifactMetadataSource source, ArtifactFilter filter )
        throws ArtifactResolutionException, ArtifactNotFoundException;

    // USED BY MAVEN ASSEMBLY PLUGIN
    @Deprecated
    public ArtifactResolutionResult resolveTransitively( Set<Artifact> artifacts, Artifact originatingArtifact, Map managedVersions, ArtifactRepository localRepository,
                                                         List<ArtifactRepository> remoteRepositories, ArtifactMetadataSource source, ArtifactFilter filter )
        throws ArtifactResolutionException, ArtifactNotFoundException;

    // USED BY REMOTE RESOURCES PLUGIN
    @Deprecated
    void resolve( Artifact artifact, List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactResolutionException, ArtifactNotFoundException;

    // USED BY REMOTE RESOURCES PLUGIN
    @Deprecated
    void resolve( Artifact artifact, List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository, TransferListener downloadMonitor )
        throws ArtifactResolutionException, ArtifactNotFoundException;

    // USED BY ARCHETYPE DOWNLOADER
    @Deprecated
    void resolveAlways( Artifact artifact, List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactResolutionException, ArtifactNotFoundException;
}
