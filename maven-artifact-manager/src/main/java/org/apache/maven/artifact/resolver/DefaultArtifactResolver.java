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
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.transform.ArtifactTransformation;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class DefaultArtifactResolver
    extends AbstractLogEnabled
    implements ArtifactResolver
{
    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    private WagonManager wagonManager;

    private List artifactTransformations;

    protected ArtifactFactory artifactFactory;

    private ArtifactCollector artifactCollector;

    // ----------------------------------------------------------------------
    // Implementation
    // ----------------------------------------------------------------------

    public void resolve( Artifact artifact, List remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactResolutionException
    {
        if ( artifact == null )
        {
            return;
        }

        // ----------------------------------------------------------------------
        // Check for the existence of the artifact in the specified local
        // ArtifactRepository. If it is present then simply return as the
        // request for resolution has been satisfied.
        // ----------------------------------------------------------------------

        Logger logger = getLogger();
        logger.debug(
            "Resolving: " + artifact.getId() + " from:\n" + "{localRepository: " + localRepository + "}\n" +
                "{remoteRepositories: " + remoteRepositories + "}" );

        String localPath = localRepository.pathOf( artifact );

        artifact.setFile( new File( localRepository.getBasedir(), localPath ) );

        // TODO: better to have a transform manager, or reuse the handler manager again so we don't have these requirements duplicated all over?
        for ( Iterator i = artifactTransformations.iterator(); i.hasNext(); )
        {
            ArtifactTransformation transform = (ArtifactTransformation) i.next();
            try
            {
                transform.transformForResolve( artifact, remoteRepositories, localRepository );
            }
            catch ( ArtifactMetadataRetrievalException e )
            {
                throw new ArtifactResolutionException( e.getMessage(), artifact, remoteRepositories, e );
            }
        }

        File destination = artifact.getFile();
        if ( !destination.exists() )
        {
            try
            {
                if ( artifact.getRepository() != null )
                {
                    // the transformations discovered the artifact - so use it exclusively
                    wagonManager.getArtifact( artifact, artifact.getRepository(), destination );
                }
                else
                {
                    wagonManager.getArtifact( artifact, remoteRepositories, destination );
                }

                // must be after the artifact is downloaded
                for ( Iterator i = artifact.getMetadataList().iterator(); i.hasNext(); )
                {
                    ArtifactMetadata metadata = (ArtifactMetadata) i.next();
                    metadata.storeInLocalRepository( localRepository );
                }
            }
            catch ( ResourceDoesNotExistException e )
            {
                throw new ArtifactResolutionException( e.getMessage(), artifact, remoteRepositories, e );
            }
            catch ( TransferFailedException e )
            {
                throw new ArtifactResolutionException( e.getMessage(), artifact, remoteRepositories, e );
            }
            catch ( ArtifactMetadataRetrievalException e )
            {
                throw new ArtifactResolutionException( e.getMessage(), artifact, remoteRepositories, e );
            }
        }
    }

    public ArtifactResolutionResult resolveTransitively( Artifact artifact, List remoteRepositories,
                                                         ArtifactRepository localRepository,
                                                         ArtifactMetadataSource source )
        throws ArtifactResolutionException
    {
        return resolveTransitively( artifact, remoteRepositories, localRepository, source, null );
    }

    public ArtifactResolutionResult resolveTransitively( Artifact artifact, List remoteRepositories,
                                                         ArtifactRepository localRepository,
                                                         ArtifactMetadataSource source, ArtifactFilter filter )
        throws ArtifactResolutionException
    {
        return resolveTransitively( Collections.singleton( artifact ), null, remoteRepositories, localRepository,
                                    source, filter );
    }

    public ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact,
                                                         List remoteRepositories, ArtifactRepository localRepository,
                                                         ArtifactMetadataSource source, ArtifactFilter filter )
        throws ArtifactResolutionException
    {
        ArtifactResolutionResult artifactResolutionResult;

        artifactResolutionResult = artifactCollector.collect( artifacts, originatingArtifact, localRepository,
                                                              remoteRepositories, source, filter, artifactFactory );

        for ( Iterator i = artifactResolutionResult.getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            resolve( artifact, remoteRepositories, localRepository );
        }

        return artifactResolutionResult;
    }

    public ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact,
                                                         List remoteRepositories, ArtifactRepository localRepository,
                                                         ArtifactMetadataSource source )
        throws ArtifactResolutionException
    {
        return resolveTransitively( artifacts, originatingArtifact, remoteRepositories, localRepository, source, null );
    }

}