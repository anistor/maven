package org.apache.maven.artifact.metadata;

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
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

public class ResolutionGroup
{

    private final Set<Artifact> artifacts;

    private final List<ArtifactRepository> resolutionRepositories;

    private final Artifact pomArtifact;

    public ResolutionGroup( Artifact pomArtifact,
                            Set<Artifact> artifacts,
                            List<ArtifactRepository> resolutionRepositories )
    {
        this.pomArtifact = pomArtifact;
        this.artifacts = artifacts;
        this.resolutionRepositories = resolutionRepositories;
    }

    public Artifact getPomArtifact()
    {
        return pomArtifact;
    }

    public Set<Artifact> getArtifacts()
    {
        return artifacts;
    }

    public List<ArtifactRepository> getResolutionRepositories()
    {
        return resolutionRepositories;
    }

}
