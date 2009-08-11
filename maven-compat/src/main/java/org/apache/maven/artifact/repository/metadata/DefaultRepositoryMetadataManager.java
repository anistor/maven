package org.apache.maven.artifact.repository.metadata;

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.DefaultRepositoryRequest;
import org.apache.maven.artifact.repository.RepositoryCache;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.repository.legacy.UpdateCheckManager;
import org.apache.maven.repository.legacy.WagonManager;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * @author Jason van Zyl
 */
@Component(role=RepositoryMetadataManager.class)
public class DefaultRepositoryMetadataManager
    extends AbstractLogEnabled
    implements RepositoryMetadataManager
{
    @Requirement
    private WagonManager wagonManager;

    @Requirement
    private UpdateCheckManager updateCheckManager;

    public void resolve( RepositoryMetadata metadata, List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository )
        throws RepositoryMetadataResolutionException
    {
        RepositoryRequest request = new DefaultRepositoryRequest();
        request.setLocalRepository( localRepository );
        request.setRemoteRepositories( remoteRepositories );
        resolve( metadata, request );
    }

    public void resolve( RepositoryMetadata metadata, RepositoryRequest request )
        throws RepositoryMetadataResolutionException
    {
        RepositoryCache cache = request.getCache();

        CacheKey cacheKey = null;

        if ( cache != null )
        {
            cacheKey = new CacheKey( metadata, request );

            CacheRecord cacheRecord = (CacheRecord) cache.get( request, cacheKey );

            if ( cacheRecord != null )
            {
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug( "Resolved metadata from cache: " + metadata );
                }

                metadata.setMetadata( MetadataUtils.cloneMetadata( cacheRecord.metadata ) );

                if ( cacheRecord.repository != null )
                {
                    for ( ArtifactRepository repository : request.getRemoteRepositories() )
                    {
                        if ( cacheRecord.repository.equals( repository.getId() ) )
                        {
                            metadata.setRepository( repository );
                            break;
                        }
                    }
                }

                return;
            }
        }

        ArtifactRepository localRepository = request.getLocalRepository();
        List<ArtifactRepository> remoteRepositories = request.getRemoteRepositories();

        if ( !request.isOffline() )
        {
            for ( ArtifactRepository repository : remoteRepositories )
            {
                ArtifactRepositoryPolicy policy =
                    metadata.isSnapshot() ? repository.getSnapshots() : repository.getReleases();

                File file =
                    new File( localRepository.getBasedir(), localRepository.pathOfLocalRepositoryMetadata( metadata,
                                                                                                           repository ) );

                if ( updateCheckManager.isUpdateRequired( metadata, repository, file ) )
                {
                    getLogger().info( metadata.getKey() + ": checking for updates from " + repository.getId() );
                    try
                    {
                        wagonManager.getArtifactMetadata( metadata, repository, file, policy.getChecksumPolicy() );
                    }
                    catch ( ResourceDoesNotExistException e )
                    {
                        getLogger().debug( metadata + " could not be found on repository: " + repository.getId() );

                        // delete the local copy so the old details aren't used.
                        if ( file.exists() )
                        {
                            file.delete();
                        }
                    }
                    catch ( TransferFailedException e )
                    {
                        getLogger().warn( metadata + " could not be retrieved from repository: " + repository.getId()
                                              + " due to an error: " + e.getMessage() );
                        getLogger().debug( "Exception", e );
                    }
                    finally
                    {
                        updateCheckManager.touch( metadata, repository, file );
                    }
                }
                else
                {
                    getLogger().debug( "Skipping metadata update of " + metadata.getKey() + " from "
                                           + repository.getId() );
                }

                // TODO: should this be inside the above check?
                // touch file so that this is not checked again until interval has passed
                if ( file.exists() )
                {
                    file.setLastModified( System.currentTimeMillis() );
                }
            }
        }

        try
        {
            mergeMetadata( metadata, remoteRepositories, localRepository );
        }
        catch ( RepositoryMetadataStoreException e )
        {
            throw new RepositoryMetadataResolutionException( "Unable to store local copy of metadata: " + e.getMessage(), e );
        }
        catch ( RepositoryMetadataReadException e )
        {
            throw new RepositoryMetadataResolutionException( "Unable to read local copy of metadata: " + e.getMessage(), e );
        }

        if ( cache != null )
        {
            cache.put( request, cacheKey, new CacheRecord( metadata ) );
        }
    }

    private static final class CacheKey
    {

        final Object metadataKey;

        final ArtifactRepository localRepository;

        final List<ArtifactRepository> remoteRepositories;

        final int hashCode;

        CacheKey( RepositoryMetadata metadata, RepositoryRequest request )
        {
            metadataKey = metadata.getKey();
            localRepository = request.getLocalRepository();
            remoteRepositories = request.getRemoteRepositories();

            int hash = 17;
            hash = hash * 31 + metadata.getKey().hashCode();
            hash = hash * 31 + repoHashCode( localRepository );
            for ( ArtifactRepository remoteRepository : remoteRepositories )
            {
                hash = hash * 31 + repoHashCode( remoteRepository );
            }
            hashCode = hash;
        }

        int repoHashCode( ArtifactRepository repository )
        {
            return ( repository != null && repository.getUrl() != null ) ? repository.getUrl().hashCode() : 0;
        }

        boolean repoEquals( ArtifactRepository repo1, ArtifactRepository repo2 )
        {
            if ( repo1 == repo2 )
            {
                return true;
            }

            if ( repo1 == null || repo2 == null )
            {
                return false;
            }

            return equal( repo1.getUrl(), repo2.getUrl() ) && repo1.getClass() == repo2.getClass();
        }

        private static <T> boolean equal( T s1, T s2 )
        {
            return s1 != null ? s1.equals( s2 ) : s2 == null;
        }

        @Override
        public boolean equals( Object obj )
        {
            if ( this == obj )
            {
                return true;
            }

            if ( !( obj instanceof CacheKey ) )
            {
                return false;
            }

            CacheKey that = (CacheKey) obj;

            if ( !this.metadataKey.equals( that.metadataKey ) )
            {
                return false;
            }

            if ( !repoEquals( this.localRepository, that.localRepository ) )
            {
                return false;
            }

            for ( Iterator<ArtifactRepository> it1 = this.remoteRepositories.iterator(), it2 =
                that.remoteRepositories.iterator();; )
            {
                if ( !it1.hasNext() || !it2.hasNext() )
                {
                    if ( it1.hasNext() != it2.hasNext() )
                    {
                        return false;
                    }
                    break;
                }
                ArtifactRepository repo1 = it1.next();
                ArtifactRepository repo2 = it2.next();
                if ( !repoEquals( repo1, repo2 ) )
                {
                    return false;
                }
            }

            return true;
        }

        @Override
        public int hashCode()
        {
            return hashCode;
        }

    }

    private static final class CacheRecord
    {

        final Metadata metadata;

        final String repository;

        CacheRecord( RepositoryMetadata metadata )
        {
            this.metadata = MetadataUtils.cloneMetadata( metadata.getMetadata() );
            this.repository = ( metadata.getRepository() != null ) ? metadata.getRepository().getId() : null;
        }

    }

    private void mergeMetadata( RepositoryMetadata metadata, List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository )
        throws RepositoryMetadataStoreException, RepositoryMetadataReadException
    {
        // TODO: currently this is first wins, but really we should take the latest by comparing either the
        // snapshot timestamp, or some other timestamp later encoded into the metadata.
        // TODO: this needs to be repeated here so the merging doesn't interfere with the written metadata
        //  - we'd be much better having a pristine input, and an ongoing metadata for merging instead

        Map<ArtifactRepository, Metadata> previousMetadata = new HashMap<ArtifactRepository, Metadata>();
        ArtifactRepository selected = null;
        for ( ArtifactRepository repository : remoteRepositories )
        {
            ArtifactRepositoryPolicy policy = metadata.isSnapshot() ? repository.getSnapshots() : repository.getReleases();

            if ( policy.isEnabled() && loadMetadata( metadata, repository, localRepository, previousMetadata ) )
            {
                metadata.setRepository( repository );
                selected = repository;
            }
        }
        if ( loadMetadata( metadata, localRepository, localRepository, previousMetadata ) )
        {
            metadata.setRepository( null );
            selected = localRepository;
        }

        updateSnapshotMetadata( metadata, previousMetadata, selected, localRepository );
    }

    private void updateSnapshotMetadata( RepositoryMetadata metadata, Map<ArtifactRepository, Metadata> previousMetadata, ArtifactRepository selected, ArtifactRepository localRepository )
        throws RepositoryMetadataStoreException
    {
        // TODO: this could be a lot nicer... should really be in the snapshot transformation?
        if ( metadata.isSnapshot() )
        {
            Metadata prevMetadata = metadata.getMetadata();

            for ( ArtifactRepository repository : previousMetadata.keySet() )
            {
                Metadata m = previousMetadata.get( repository );
                if ( repository.equals( selected ) )
                {
                    if ( m.getVersioning() == null )
                    {
                        m.setVersioning( new Versioning() );
                    }

                    if ( m.getVersioning().getSnapshot() == null )
                    {
                        m.getVersioning().setSnapshot( new Snapshot() );
                    }
                }
                else
                {
                    if ( ( m.getVersioning() != null ) && ( m.getVersioning().getSnapshot() != null ) && m.getVersioning().getSnapshot().isLocalCopy() )
                    {
                        m.getVersioning().getSnapshot().setLocalCopy( false );
                        metadata.setMetadata( m );
                        metadata.storeInLocalRepository( localRepository, repository );
                    }
                }
            }

            metadata.setMetadata( prevMetadata );
        }
    }

    private boolean loadMetadata( RepositoryMetadata repoMetadata, ArtifactRepository remoteRepository, ArtifactRepository localRepository, Map<ArtifactRepository, Metadata> previousMetadata )
        throws RepositoryMetadataReadException
    {
        boolean setRepository = false;

        File metadataFile = new File( localRepository.getBasedir(), localRepository.pathOfLocalRepositoryMetadata( repoMetadata, remoteRepository ) );

        if ( metadataFile.exists() )
        {
            Metadata metadata = readMetadata( metadataFile );

            if ( repoMetadata.isSnapshot() && ( previousMetadata != null ) )
            {
                previousMetadata.put( remoteRepository, metadata );
            }

            if ( repoMetadata.getMetadata() != null )
            {
                setRepository = repoMetadata.getMetadata().merge( metadata );
            }
            else
            {
                repoMetadata.setMetadata( metadata );
                setRepository = true;
            }
        }
        return setRepository;
    }

    /** @todo share with DefaultPluginMappingManager. */
    protected static Metadata readMetadata( File mappingFile )
        throws RepositoryMetadataReadException
    {
        Metadata result;

        Reader reader = null;
        try
        {
            reader = ReaderFactory.newXmlReader( mappingFile );

            MetadataXpp3Reader mappingReader = new MetadataXpp3Reader();

            result = mappingReader.read( reader, false );
        }
        catch ( FileNotFoundException e )
        {
            throw new RepositoryMetadataReadException( "Cannot read metadata from '" + mappingFile + "'", e );
        }
        catch ( IOException e )
        {
            throw new RepositoryMetadataReadException( "Cannot read metadata from '" + mappingFile + "': " + e.getMessage(), e );
        }
        catch ( XmlPullParserException e )
        {
            throw new RepositoryMetadataReadException( "Cannot read metadata from '" + mappingFile + "': " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( reader );
        }
        return result;
    }

    public void resolveAlways( RepositoryMetadata metadata, ArtifactRepository localRepository, ArtifactRepository remoteRepository )
        throws RepositoryMetadataResolutionException
    {
        File file;
        try
        {
            file = getArtifactMetadataFromDeploymentRepository( metadata, localRepository, remoteRepository );
        }
        catch ( TransferFailedException e )
        {
            throw new RepositoryMetadataResolutionException( metadata + " could not be retrieved from repository: " + remoteRepository.getId() + " due to an error: " + e.getMessage(), e );
        }

        try
        {
            if ( file.exists() )
            {
                Metadata prevMetadata = readMetadata( file );
                metadata.setMetadata( prevMetadata );
            }
        }
        catch ( RepositoryMetadataReadException e )
        {
            throw new RepositoryMetadataResolutionException( e.getMessage(), e );
        }
    }

    private File getArtifactMetadataFromDeploymentRepository( ArtifactMetadata metadata, ArtifactRepository localRepository, ArtifactRepository remoteRepository )
        throws TransferFailedException
    {
        File file = new File( localRepository.getBasedir(), localRepository.pathOfLocalRepositoryMetadata( metadata, remoteRepository ) );

        try
        {
            wagonManager.getArtifactMetadataFromDeploymentRepository( metadata, remoteRepository, file, ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN );
        }
        catch ( ResourceDoesNotExistException e )
        {
            getLogger().info( metadata + " could not be found on repository: " + remoteRepository.getId() + ", so will be created" );

            // delete the local copy so the old details aren't used.
            if ( file.exists() )
            {
                file.delete();
            }
        }
        finally
        {
            if ( metadata instanceof RepositoryMetadata )
            {
                updateCheckManager.touch( (RepositoryMetadata) metadata, remoteRepository, file );
            }
        }
        return file;
    }

    public void deploy( ArtifactMetadata metadata, ArtifactRepository localRepository, ArtifactRepository deploymentRepository )
        throws RepositoryMetadataDeploymentException
    {
        File file;
        if ( metadata instanceof RepositoryMetadata )
        {
            getLogger().info( "Retrieving previous metadata from " + deploymentRepository.getId() );
            try
            {
                file = getArtifactMetadataFromDeploymentRepository( metadata, localRepository, deploymentRepository );
            }
            catch ( TransferFailedException e )
            {
                throw new RepositoryMetadataDeploymentException( metadata + " could not be retrieved from repository: " + deploymentRepository.getId() + " due to an error: " + e.getMessage(), e );
            }
        }
        else
        {
            // It's a POM - we don't need to retrieve it first
            file = new File( localRepository.getBasedir(), localRepository.pathOfLocalRepositoryMetadata( metadata, deploymentRepository ) );
        }

        try
        {
            metadata.storeInLocalRepository( localRepository, deploymentRepository );
        }
        catch ( RepositoryMetadataStoreException e )
        {
            throw new RepositoryMetadataDeploymentException( "Error installing metadata: " + e.getMessage(), e );
        }

        try
        {
            wagonManager.putArtifactMetadata( file, metadata, deploymentRepository );
        }
        catch ( TransferFailedException e )
        {
            throw new RepositoryMetadataDeploymentException( "Error while deploying metadata: " + e.getMessage(), e );
        }
    }

    public void install( ArtifactMetadata metadata, ArtifactRepository localRepository )
        throws RepositoryMetadataInstallationException
    {
        try
        {
            metadata.storeInLocalRepository( localRepository, localRepository );
        }
        catch ( RepositoryMetadataStoreException e )
        {
            throw new RepositoryMetadataInstallationException( "Error installing metadata: " + e.getMessage(), e );
        }
    }

}
