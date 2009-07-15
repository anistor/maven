package org.apache.maven.project;

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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryAdapter;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.LegacyArtifactRepositoryAdapter;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.Repository;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.repository.legacy.InvalidRepositoryException;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

// This class needs to stick around because it was exposed the the remote resources plugin started using it instead of
// getting the repositories from the project.

@Deprecated
public final class ProjectUtils
{
    static RepositorySystem rs;
        
    private ProjectUtils()
    {
    }

    public static List<ArtifactRepository> buildArtifactRepositories( List<Repository> repositories, ArtifactRepositoryFactory artifactRepositoryFactory, PlexusContainer c )
        throws InvalidRepositoryException
    {
        //
        // Old repository format
        //
        List<ArtifactRepository> remoteRepositories = new ArrayList<ArtifactRepository>();
        
        for ( Repository r : repositories )
        {
            remoteRepositories.add( buildArtifactRepository( r, artifactRepositoryFactory, c ) );
        }       
                
        //TODO: This is not very nice so we need to make sure the mirrors are processed internally
        // and plugins that are accessing repositories outside the context of a MavenProject or the system
        // is categorically bad.
        return convertToLegacy( rs( c ).getMirrors( convertFromLegacy( remoteRepositories ) ) );        
    }

    private static List<org.apache.maven.repository.legacy.repository.ArtifactRepository> convertFromLegacy( List<ArtifactRepository> repositories )
    {
        List<org.apache.maven.repository.legacy.repository.ArtifactRepository> repos = new ArrayList<org.apache.maven.repository.legacy.repository.ArtifactRepository>();
        for( ArtifactRepository r : repositories )
        {
            repos.add( new ArtifactRepositoryAdapter( r ) );            
        }
        return repos;
    }
        
    private static List<ArtifactRepository> convertToLegacy( List<org.apache.maven.repository.legacy.repository.ArtifactRepository> repositories )
    {
        List<ArtifactRepository> repos = new ArrayList<ArtifactRepository>();        
        for( org.apache.maven.repository.legacy.repository.ArtifactRepository r : repositories )
        {
            repos.add( new LegacyArtifactRepositoryAdapter( r ) );
        }
        return repos;
    }
    
    public static ArtifactRepository buildDeploymentArtifactRepository( DeploymentRepository repo, ArtifactRepositoryFactory artifactRepositoryFactory, PlexusContainer c )
        throws InvalidRepositoryException
    {
        return new LegacyArtifactRepositoryAdapter( rs( c ).buildArtifactRepository( repo ) );
    }

    public static ArtifactRepository buildArtifactRepository( Repository repo, ArtifactRepositoryFactory artifactRepositoryFactory, PlexusContainer c )
        throws InvalidRepositoryException
    {
        return new LegacyArtifactRepositoryAdapter( rs( c  ).buildArtifactRepository( repo ) );        
    }

    private static RepositorySystem rs( PlexusContainer c )
    {
        try
        {
            rs = c.lookup( RepositorySystem.class );
        }
        catch ( ComponentLookupException e )
        {
        }
        
        return rs;
    }
}
