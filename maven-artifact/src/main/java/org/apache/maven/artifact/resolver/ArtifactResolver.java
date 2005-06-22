package org.apache.maven.artifact.resolver;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * I want to use it for hidding the fact that sometime artifact must be
 * downloaded. I am just asking LocalRepository for given artifact and I don't
 * care if it is alredy there or how it will get there.
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka </a>
 * @version $Id$
 */
public interface ArtifactResolver
{
    static String ROLE = ArtifactResolver.class.getName();

    void resolve( Artifact artifact, List remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactResolutionException;

    ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact, List remoteRepositories,
                                                  ArtifactRepository localRepository, ArtifactMetadataSource source )
        throws ArtifactResolutionException;

    ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact, List remoteRepositories,
                                                  ArtifactRepository localRepository, ArtifactMetadataSource source,
                                                  List listeners )
        throws ArtifactResolutionException;

    ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact,
                                                  ArtifactRepository localRepository, List remoteRepositories,
                                                  ArtifactMetadataSource source, ArtifactFilter filter )
        throws ArtifactResolutionException;

    ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact, Map managedVersions,
                                                  ArtifactRepository localRepository, List remoteRepositories,
                                                  ArtifactMetadataSource source )
        throws ArtifactResolutionException;

    ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact, Map managedVersions,
                                                  ArtifactRepository localRepository, List remoteRepositories,
                                                  ArtifactMetadataSource source, ArtifactFilter filter )
        throws ArtifactResolutionException;
}