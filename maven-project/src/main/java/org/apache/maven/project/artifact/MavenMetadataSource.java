package org.apache.maven.project.artifact;

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
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Relocation;
import org.apache.maven.project.InvalidModelException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id$
 */
public class MavenMetadataSource
    extends AbstractLogEnabled
    implements ArtifactMetadataSource
{

    public static final String ROLE_HINT = "maven";

    private MavenProjectBuilder mavenProjectBuilder;

    private ArtifactFactory artifactFactory;

    // lazily instantiated and cached.
    private MavenProject superProject;

    /**
     * Retrieve the metadata for the project from the repository.
     * Uses the ProjectBuilder, to enable post-processing and inheritance calculation before retrieving the
     * associated artifacts.
     */
    public ResolutionGroup retrieve( Artifact artifact, ArtifactRepository localRepository, List remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        MavenProject project = null;

        Artifact pomArtifact;
        boolean done = false;
        do
        {
            // TODO: can we just modify the original?
            pomArtifact = artifactFactory.createProjectArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                                                 artifact.getVersion(), artifact.getScope() );

            if ( Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
            {
                done = true;
            }
            else
            {
                try
                {
                    project = mavenProjectBuilder
                        .buildFromRepository( pomArtifact, remoteRepositories, localRepository );
                }
                catch ( InvalidModelException e )
                {
                    getLogger()
                        .warn(
                               "POM for: \'" + pomArtifact.getId()
                                   + "\' does not appear to be valid. Its will be ignored for artifact resolution." );

                    project = null;
                }
                catch ( ProjectBuildingException e )
                {
                    throw new ArtifactMetadataRetrievalException( "Unable to read the metadata file", e );
                }

                if ( project != null )
                {
                    Relocation relocation = null;

                    DistributionManagement distMgmt = project.getDistributionManagement();
                    if ( distMgmt != null )
                    {
                        relocation = distMgmt.getRelocation();
                    }

                    if ( relocation != null )
                    {
                        if ( relocation.getGroupId() != null )
                        {
                            artifact.setGroupId( relocation.getGroupId() );
                        }
                        if ( relocation.getArtifactId() != null )
                        {
                            artifact.setArtifactId( relocation.getArtifactId() );
                        }
                        if ( relocation.getVersion() != null )
                        {
                            artifact.setVersion( relocation.getVersion() );
                        }

                        String message = "\n  This artifact has been relocated to " + artifact.getGroupId() + ":"
                            + artifact.getArtifactId() + ":" + artifact.getVersion() + ".\n";

                        if ( relocation.getMessage() != null )
                        {
                            message += "  " + relocation.getMessage() + "\n";
                        }

                        getLogger().warn( message + "\n" );
                    }
                    else
                    {
                        done = true;
                    }
                }
                else
                {
                    done = true;
                }
            }
        }
        while ( !done );

        // TODO: this could come straight from the project, negating the need to set it in the project itself?
        artifact.setDownloadUrl( pomArtifact.getDownloadUrl() );

        try
        {
            ResolutionGroup result;
            
            if ( project == null )
            {
                // if the project is null, we encountered an invalid model (read: m1 POM)
                // we'll just return an empty resolution group.
                result = new ResolutionGroup( pomArtifact, Collections.EMPTY_SET, Collections.EMPTY_LIST );
            }
            else
            {
                // TODO: we could possibly use p.getDependencyArtifacts instead of this call, but they haven't been filtered
                // or used the inherited scope (should that be passed to the buildFromRepository method above?)
                Set artifacts = project.createArtifacts( artifactFactory, artifact.getScope(),
                                                     artifact.getDependencyFilter() );
                
                List repositories = aggregateRepositoryLists( remoteRepositories, project.getRemoteArtifactRepositories() );
                
                result = new ResolutionGroup( pomArtifact, artifacts, repositories );
            }
            
            return result;
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new ArtifactMetadataRetrievalException( "Unable to read the metadata file", e );
        }
        catch ( ProjectBuildingException e )
        {
            throw new ArtifactMetadataRetrievalException( "Unable to read the metadata file", e );
        }
    }

    private List aggregateRepositoryLists( List remoteRepositories, List remoteArtifactRepositories )
        throws ProjectBuildingException
    {
        if ( superProject == null )
        {
            superProject = mavenProjectBuilder.buildStandaloneSuperProject( null );
        }

        List repositories = new ArrayList();

        repositories.addAll( remoteRepositories );

        // ensure that these are defined
        for ( Iterator it = superProject.getRemoteArtifactRepositories().iterator(); it.hasNext(); )
        {
            ArtifactRepository superRepo = (ArtifactRepository) it.next();

            for ( Iterator aggregatedIterator = repositories.iterator(); aggregatedIterator.hasNext(); )
            {
                ArtifactRepository repo = (ArtifactRepository) aggregatedIterator.next();

                // if the repository exists in the list and was introduced by another POM's super-pom, 
                // remove it...the repository definitions from the super-POM should only be at the end of
                // the list.
                // if the repository has been redefined, leave it.
                if ( repo.getId().equals( superRepo.getId() ) && repo.getUrl().equals( superRepo.getUrl() ) )
                {
                    aggregatedIterator.remove();
                }
            }
        }

        // this list should contain the super-POM repositories, so we don't have to explicitly add them back.
        for ( Iterator it = remoteArtifactRepositories.iterator(); it.hasNext(); )
        {
            ArtifactRepository repository = (ArtifactRepository) it.next();

            if ( !repositories.contains( repository ) )
            {
                repositories.add( repository );
            }
        }

        return repositories;
    }

    public static Set createArtifacts( ArtifactFactory artifactFactory, List dependencies, String inheritedScope,
                                       ArtifactFilter dependencyFilter, Map projectReferences )
        throws InvalidVersionSpecificationException
    {
        Set projectArtifacts = new HashSet( dependencies.size() );

        for ( Iterator i = dependencies.iterator(); i.hasNext(); )
        {
            Dependency d = (Dependency) i.next();
            
            String scope = d.getScope();
            
            if ( StringUtils.isEmpty( scope ) )
            {
                scope = Artifact.SCOPE_COMPILE;
                
                d.setScope( scope );
            }

            VersionRange versionRange = VersionRange.createFromVersionSpec( d.getVersion() );
            Artifact artifact = artifactFactory.createDependencyArtifact( d.getGroupId(), d.getArtifactId(),
                                                                          versionRange, d.getType(), d.getClassifier(),
                                                                          scope, inheritedScope );
            
            if ( Artifact.SCOPE_SYSTEM.equals( scope ) )
            {
                artifact.setFile( new File( d.getSystemPath() ) );
            }

            if ( artifact != null && ( dependencyFilter == null || dependencyFilter.include( artifact ) ) )
            {
                if ( d.getExclusions() != null && !d.getExclusions().isEmpty() )
                {
                    List exclusions = new ArrayList();
                    for ( Iterator j = d.getExclusions().iterator(); j.hasNext(); )
                    {
                        Exclusion e = (Exclusion) j.next();
                        exclusions.add( e.getGroupId() + ":" + e.getArtifactId() );
                    }

                    ArtifactFilter newFilter = new ExcludesArtifactFilter( exclusions );

                    if ( dependencyFilter != null )
                    {
                        AndArtifactFilter filter = new AndArtifactFilter();
                        filter.add( dependencyFilter );
                        filter.add( newFilter );
                        dependencyFilter = filter;
                    }
                    else
                    {
                        dependencyFilter = newFilter;
                    }
                }

                artifact.setDependencyFilter( dependencyFilter );

                if ( projectReferences != null )
                {
                    // TODO: use MavenProject getProjectReferenceId
                    String refId = d.getGroupId() + ":" + d.getArtifactId();
                    MavenProject project = (MavenProject) projectReferences.get( refId );
                    if ( project != null && project.getArtifact() != null )
                    {
                        artifact = new ActiveProjectArtifact( project, artifact );
                    }
                }

                projectArtifacts.add( artifact );
            }
        }

        return projectArtifacts;
    }
}
