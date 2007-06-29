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

import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.resolver.conflict.ConflictResolver;
import org.apache.maven.artifact.resolver.conflict.ConflictResolverFactory;
import org.apache.maven.artifact.resolver.conflict.ConflictResolverNotFoundException;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryBase;
import org.apache.maven.model.RepositoryPolicy;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ProjectUtils
{
    /**
     * The POM property name for the comma-separated list of conflict resolver types.
     */
    private static final String CONFLICT_RESOLVERS_PROPERTY = "mavenConflictResolvers";
    
    private ProjectUtils()
    {
    }

    public static List buildArtifactRepositories( List repositories,
                                                  ArtifactRepositoryFactory artifactRepositoryFactory,
                                                  PlexusContainer container )
        throws InvalidRepositoryException
    {

        List repos = new ArrayList();

        for ( Iterator i = repositories.iterator(); i.hasNext(); )
        {
            Repository mavenRepo = (Repository) i.next();

            ArtifactRepository artifactRepo =
                buildArtifactRepository( mavenRepo, artifactRepositoryFactory, container );

            if ( !repos.contains( artifactRepo ) )
            {
                repos.add( artifactRepo );
            }
        }
        return repos;
    }

    public static ArtifactRepository buildDeploymentArtifactRepository( DeploymentRepository repo,
                                                                        ArtifactRepositoryFactory artifactRepositoryFactory,
                                                                        PlexusContainer container )
        throws InvalidRepositoryException
    {
        if ( repo != null )
        {
            String id = repo.getId();
            String url = repo.getUrl();

            // TODO: make this a map inside the factory instead, so no lookup needed
            ArtifactRepositoryLayout layout = getRepositoryLayout( repo, container );

            return artifactRepositoryFactory.createDeploymentArtifactRepository( id, url, layout,
                                                                                 repo.isUniqueVersion() );
        }
        else
        {
            return null;
        }
    }

    public static ArtifactRepository buildArtifactRepository( Repository repo,
                                                              ArtifactRepositoryFactory artifactRepositoryFactory,
                                                              PlexusContainer container )
        throws InvalidRepositoryException
    {
        if ( repo != null )
        {
            String id = repo.getId();
            String url = repo.getUrl();

            // TODO: make this a map inside the factory instead, so no lookup needed
            ArtifactRepositoryLayout layout = getRepositoryLayout( repo, container );

            ArtifactRepositoryPolicy snapshots = buildArtifactRepositoryPolicy( repo.getSnapshots() );
            ArtifactRepositoryPolicy releases = buildArtifactRepositoryPolicy( repo.getReleases() );

            return artifactRepositoryFactory.createArtifactRepository( id, url, layout, snapshots, releases );
        }
        else
        {
            return null;
        }
    }
    
    /**
     * Gets a list of conflict resolvers for the specified project. The project's conflict resolvers are defined by the
     * POM property <code>mavenConflictResolvers</code> as a comma-separated list of conflict resolver types. Conflict
     * resolver instances for these types are then obtained from the factory and returned.
     * 
     * @param project
     *            the project
     * @param conflictResolverFactory
     *            the factory to use to obtain the conflict resolvers from
     * @return the list of conflict resolvers
     * @throws ConflictResolverNotFoundException
     *             if a specified conflict resolver cannot be found
     */
    public static List buildConflictResolvers( MavenProject project, ConflictResolverFactory conflictResolverFactory )
        throws ConflictResolverNotFoundException
    {
        List conflictResolvers;

        Object value = project.getProperties().get( CONFLICT_RESOLVERS_PROPERTY );

        if ( value instanceof String )
        {
            conflictResolvers = new ArrayList();
            
            String[] types = ( (String) value ).split( "," );

            for ( int i = 0; i < types.length; i++ )
            {
                String type = types[i].trim();

                ConflictResolver conflictResolver = conflictResolverFactory.getConflictResolver( type );
                conflictResolvers.add( conflictResolver );
            }
        }
        else
        {
            conflictResolvers = null;
        }

        return conflictResolvers;
    }

    private static ArtifactRepositoryPolicy buildArtifactRepositoryPolicy( RepositoryPolicy policy )
    {
        boolean enabled = true;
        String updatePolicy = null;
        String checksumPolicy = null;

        if ( policy != null )
        {
            enabled = policy.isEnabled();
            if ( policy.getUpdatePolicy() != null )
            {
                updatePolicy = policy.getUpdatePolicy();
            }
            if ( policy.getChecksumPolicy() != null )
            {
                checksumPolicy = policy.getChecksumPolicy();
            }
        }

        return new ArtifactRepositoryPolicy( enabled, updatePolicy, checksumPolicy );
    }

    private static ArtifactRepositoryLayout getRepositoryLayout( RepositoryBase mavenRepo, PlexusContainer container )
        throws InvalidRepositoryException
    {
        String layout = mavenRepo.getLayout();

        ArtifactRepositoryLayout repositoryLayout;
        try
        {
            repositoryLayout = (ArtifactRepositoryLayout) container.lookup( ArtifactRepositoryLayout.ROLE, layout );
        }
        catch ( ComponentLookupException e )
        {
            throw new InvalidRepositoryException( "Cannot find layout implementation corresponding to: \'" + layout +
                "\' for remote repository with id: \'" + mavenRepo.getId() + "\'.", e );
        }
        return repositoryLayout;
    }

}
