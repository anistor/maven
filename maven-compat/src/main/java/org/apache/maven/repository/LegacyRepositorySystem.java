package org.apache.maven.repository;

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
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;
import org.apache.maven.repository.legacy.ChecksumFailedException;
import org.apache.maven.repository.legacy.InvalidRepositoryException;
import org.apache.maven.repository.legacy.UpdateCheckManager;
import org.apache.maven.repository.legacy.deployer.ArtifactDeploymentException;
import org.apache.maven.repository.legacy.handler.ArtifactHandler;
import org.apache.maven.repository.legacy.handler.manager.ArtifactHandlerManager;
import org.apache.maven.repository.legacy.installer.ArtifactInstallationException;
import org.apache.maven.repository.legacy.metadata.ArtifactMetadata;
import org.apache.maven.repository.legacy.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.repository.legacy.metadata.ArtifactMetadataSource;
import org.apache.maven.repository.legacy.metadata.ResolutionGroup;
import org.apache.maven.repository.legacy.repository.ArtifactRepository;
import org.apache.maven.repository.legacy.repository.ArtifactRepositoryFactory;
import org.apache.maven.repository.legacy.repository.ArtifactRepositoryPolicy;
import org.apache.maven.repository.legacy.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.repository.legacy.repository.metadata.RepositoryMetadataDeploymentException;
import org.apache.maven.repository.legacy.repository.metadata.RepositoryMetadataInstallationException;
import org.apache.maven.repository.legacy.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.repository.legacy.resolver.CyclicDependencyException;
import org.apache.maven.repository.legacy.resolver.DebugResolutionListener;
import org.apache.maven.repository.legacy.resolver.ResolutionErrorHandler;
import org.apache.maven.repository.legacy.resolver.ResolutionListener;
import org.apache.maven.repository.legacy.resolver.ResolutionListenerForDepMgmt;
import org.apache.maven.repository.legacy.resolver.ResolutionNode;
import org.apache.maven.repository.legacy.resolver.conflict.ConflictResolver;
import org.apache.maven.repository.legacy.resolver.filter.AndArtifactFilter;
import org.apache.maven.repository.legacy.resolver.filter.ArtifactFilter;
import org.apache.maven.repository.legacy.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.repository.legacy.resolver.transform.ArtifactTransformationManager;
import org.apache.maven.repository.legacy.versioning.ArtifactVersion;
import org.apache.maven.repository.legacy.versioning.InvalidVersionSpecificationException;
import org.apache.maven.repository.legacy.versioning.ManagedVersionMap;
import org.apache.maven.repository.legacy.versioning.OverConstrainedVersionException;
import org.apache.maven.repository.legacy.versioning.VersionRange;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.observers.ChecksumObserver;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;

// ArtifactResolver
// ArtifactCollector
// ArtifactFactory

// ArtifactRepositoryFactory

/**
 * @author Jason van Zyl
 */
@Component(role = RepositorySystem.class, hint = "default")
public class LegacyRepositorySystem
    implements RepositorySystem
{
    @Requirement
    private ArtifactRepositoryFactory artifactRepositoryFactory;

    @Requirement
    private ArtifactRepositoryLayout defaultArtifactRepositoryLayout;

    @Requirement
    private MirrorBuilder mirrorBuilder;

    @Requirement
    private PlexusContainer plexus;

    @Requirement
    private Logger logger;

    @Requirement
    private ArtifactTransformationManager transformationManager;

    @Requirement
    private ResolutionErrorHandler resolutionErrorHandler;

    @Requirement
    private ArtifactMetadataSource source;

    @Requirement(hint = "nearest")
    private ConflictResolver defaultConflictResolver;

    @Requirement
    private ArtifactHandlerManager artifactHandlerManager;

    @Requirement
    private PlexusContainer container;

    public Artifact createArtifact( String groupId, String artifactId, String version, String packaging )
    {
        return createBuildArtifact( groupId, artifactId, version, packaging );
    }

    public Artifact createDependencyArtifact( Dependency d )
    {
        VersionRange versionRange;
        try
        {
            versionRange = VersionRange.createFromVersionSpec( d.getVersion() );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            return null;
        }

        Artifact artifact = createDependencyArtifact( d.getGroupId(), d.getArtifactId(), versionRange, d.getType(), d.getClassifier(), d.getScope(), d.isOptional() );

        if ( Artifact.SCOPE_SYSTEM.equals( d.getScope() ) && d.getSystemPath() != null )
        {
            artifact.setFile( new File( d.getSystemPath() ) );
        }

        if ( !d.getExclusions().isEmpty() )
        {
            List<String> exclusions = new ArrayList<String>();

            for ( Exclusion exclusion : d.getExclusions() )
            {
                exclusions.add( exclusion.getGroupId() + ':' + exclusion.getArtifactId() );
            }

            artifact.setDependencyFilter( new ExcludesArtifactFilter( exclusions ) );
        }

        return artifact;
    }

    public Artifact createExtensionArtifact( String groupId, String artifactId, String version )
    {
        VersionRange versionRange;
        try
        {
            versionRange = VersionRange.createFromVersionSpec( version );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            return null;
        }

        return createExtensionArtifact( groupId, artifactId, versionRange );
    }

    public Artifact createPluginArtifact( Plugin plugin )
    {
        VersionRange versionRange;
        try
        {
            versionRange = VersionRange.createFromVersionSpec( plugin.getVersion() );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            return null;
        }

        return createPluginArtifact( plugin.getGroupId(), plugin.getArtifactId(), versionRange );
    }

    public ArtifactRepository buildArtifactRepository( Repository repo )
        throws InvalidRepositoryException
    {
        if ( repo != null )
        {
            String id = repo.getId();

            String url = repo.getUrl();

            ArtifactRepositoryPolicy snapshots = buildArtifactRepositoryPolicy( repo.getSnapshots() );

            ArtifactRepositoryPolicy releases = buildArtifactRepositoryPolicy( repo.getReleases() );

            return artifactRepositoryFactory.createArtifactRepository( id, url, repo.getLayout(), snapshots, releases );
        }
        else
        {
            return null;
        }
    }

    public ArtifactRepositoryPolicy buildArtifactRepositoryPolicy( RepositoryPolicy policy )
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

    public ArtifactRepository createDefaultLocalRepository()
        throws InvalidRepositoryException
    {
        return createLocalRepository( RepositorySystem.defaultUserLocalRepository );
    }

    public ArtifactRepository createLocalRepository( File localRepository )
        throws InvalidRepositoryException
    {
        return createRepository( "file://" + localRepository.toURI().getRawPath(), RepositorySystem.DEFAULT_LOCAL_REPO_ID, true, ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS, true,
                                 ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS, ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE );
    }

    public ArtifactRepository createDefaultRemoteRepository()
        throws InvalidRepositoryException
    {
        return createRepository( RepositorySystem.DEFAULT_REMOTE_REPO_URL, RepositorySystem.DEFAULT_REMOTE_REPO_ID, true, ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER, false,
                                 ArtifactRepositoryPolicy.UPDATE_POLICY_DAILY, ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN );
    }

    public ArtifactRepository createLocalRepository( String url, String repositoryId )
        throws IOException
    {
        return createRepository( canonicalFileUrl( url ), repositoryId, true, ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS, true, ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS,
                                 ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE );
    }

    private String canonicalFileUrl( String url )
        throws IOException
    {
        if ( !url.startsWith( "file:" ) )
        {
            url = "file://" + url;
        }
        else if ( url.startsWith( "file:" ) && !url.startsWith( "file://" ) )
        {
            url = "file://" + url.substring( "file:".length() );
        }

        // So now we have an url of the form file://<path>

        // We want to eliminate any relative path nonsense and lock down the path so we
        // need to fully resolve it before any sub-modules use the path. This can happen
        // when you are using a custom settings.xml that contains a relative path entry
        // for the local repository setting.

        File localRepository = new File( url.substring( "file://".length() ) );

        if ( !localRepository.isAbsolute() )
        {
            url = "file://" + localRepository.getCanonicalPath();
        }

        return url;
    }

    private ArtifactRepository createRepository( String url, String repositoryId, boolean releases, String releaseUpdates, boolean snapshots, String snapshotUpdates, String checksumPolicy )
    {
        ArtifactRepositoryPolicy snapshotsPolicy = new ArtifactRepositoryPolicy( snapshots, snapshotUpdates, checksumPolicy );

        ArtifactRepositoryPolicy releasesPolicy = new ArtifactRepositoryPolicy( releases, releaseUpdates, checksumPolicy );

        return artifactRepositoryFactory.createArtifactRepository( repositoryId, url, defaultArtifactRepositoryLayout, snapshotsPolicy, releasesPolicy );
    }

    public ArtifactRepository createArtifactRepository( String id, String url, ArtifactRepositoryLayout repositoryLayout, ArtifactRepositoryPolicy snapshots, ArtifactRepositoryPolicy releases )
    {
        return artifactRepositoryFactory.createArtifactRepository( id, url, repositoryLayout, snapshots, releases );
    }

    public ArtifactResolutionResult resolve( ArtifactResolutionRequest request )
    {
        /*
         * Probably is not worth it, but here I make sure I restore request
         * to its original state. 
         */
        try
        {
            LocalArtifactRepository ideWorkspace = plexus.lookup( LocalArtifactRepository.class, LocalArtifactRepository.IDE_WORKSPACE );

            if ( request.getLocalRepository() instanceof DelegatingLocalArtifactRepository )
            {
                DelegatingLocalArtifactRepository delegatingLocalRepository = (DelegatingLocalArtifactRepository) request.getLocalRepository();

                LocalArtifactRepository orig = delegatingLocalRepository.getIdeWorspace();

                delegatingLocalRepository.setIdeWorkspace( ideWorkspace );

                try
                {
                    return resolveInternal( request );
                }
                finally
                {
                    delegatingLocalRepository.setIdeWorkspace( orig );
                }
            }
            else
            {
                ArtifactRepository localRepository = request.getLocalRepository();
                DelegatingLocalArtifactRepository delegatingLocalRepository = new DelegatingLocalArtifactRepository( localRepository );
                delegatingLocalRepository.setIdeWorkspace( ideWorkspace );
                request.setLocalRepository( delegatingLocalRepository );
                try
                {
                    return resolveInternal( request );
                }
                finally
                {
                    request.setLocalRepository( localRepository );
                }
            }
        }
        catch ( ComponentLookupException e )
        {
            // no ide workspace artifact resolution
        }

        return resolveInternal( request );
    }

    /*
    public void addProxy( String protocol, String host, int port, String username, String password, String nonProxyHosts )
    {
        ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setHost( host );
        proxyInfo.setType( protocol );
        proxyInfo.setPort( port );
        proxyInfo.setNonProxyHosts( nonProxyHosts );
        proxyInfo.setUserName( username );
        proxyInfo.setPassword( password );

        proxies.put( protocol, proxyInfo );

        wagonManager.addProxy( protocol, host, port, username, password, nonProxyHosts );
    }
    */

    /*
    public void addAuthenticationInfo( String repositoryId, String username, String password, String privateKey, String passphrase )
    {
        AuthenticationInfo authInfo = new AuthenticationInfo();
        authInfo.setUserName( username );
        authInfo.setPassword( password );
        authInfo.setPrivateKey( privateKey );
        authInfo.setPassphrase( passphrase );

        authenticationInfoMap.put( repositoryId, authInfo );

        wagonManager.addAuthenticationInfo( repositoryId, username, password, privateKey, passphrase );
    }
    */

    // Mirror 
    public void addMirror( String id, String mirrorOf, String url )
    {
        mirrorBuilder.addMirror( id, mirrorOf, url );
    }

    public List<ArtifactRepository> getMirrors( List<ArtifactRepository> repositories )
    {
        return mirrorBuilder.getMirrors( repositories );
    }

    public List<ArtifactRepository> getEffectiveRepositories( List<ArtifactRepository> repositories )
    {
        if ( repositories == null )
        {
            return null;
        }

        Map<String, List<ArtifactRepository>> reposByKey = new LinkedHashMap<String, List<ArtifactRepository>>();

        for ( ArtifactRepository repository : repositories )
        {
            String key = repository.getId();

            List<ArtifactRepository> aliasedRepos = reposByKey.get( key );

            if ( aliasedRepos == null )
            {
                aliasedRepos = new ArrayList<ArtifactRepository>();
                reposByKey.put( key, aliasedRepos );
            }

            aliasedRepos.add( repository );
        }

        List<ArtifactRepository> effectiveRepositories = new ArrayList<ArtifactRepository>();

        for ( List<ArtifactRepository> aliasedRepos : reposByKey.values() )
        {
            List<ArtifactRepositoryPolicy> releasePolicies = new ArrayList<ArtifactRepositoryPolicy>( aliasedRepos.size() );

            for ( ArtifactRepository aliasedRepo : aliasedRepos )
            {
                releasePolicies.add( aliasedRepo.getReleases() );
            }

            ArtifactRepositoryPolicy releasePolicy = getEffectivePolicy( releasePolicies );

            List<ArtifactRepositoryPolicy> snapshotPolicies = new ArrayList<ArtifactRepositoryPolicy>( aliasedRepos.size() );

            for ( ArtifactRepository aliasedRepo : aliasedRepos )
            {
                snapshotPolicies.add( aliasedRepo.getSnapshots() );
            }

            ArtifactRepositoryPolicy snapshotPolicy = getEffectivePolicy( snapshotPolicies );

            ArtifactRepository aliasedRepo = aliasedRepos.get( 0 );

            ArtifactRepository effectiveRepository = artifactRepositoryFactory.createArtifactRepository( aliasedRepo.getId(), aliasedRepo.getUrl(), aliasedRepo.getLayout(), snapshotPolicy,
                                                                                                         releasePolicy );

            effectiveRepositories.add( effectiveRepository );
        }

        return effectiveRepositories;
    }

    private ArtifactRepositoryPolicy getEffectivePolicy( Collection<ArtifactRepositoryPolicy> policies )
    {
        ArtifactRepositoryPolicy effectivePolicy = null;

        for ( ArtifactRepositoryPolicy policy : policies )
        {
            if ( effectivePolicy == null )
            {
                effectivePolicy = new ArtifactRepositoryPolicy( policy.isEnabled(), policy.getUpdatePolicy(), policy.getChecksumPolicy() );
            }
            else
            {
                if ( policy.isEnabled() )
                {
                    effectivePolicy.setEnabled( true );

                    if ( ordinalOfChecksumPolicy( policy.getChecksumPolicy() ) < ordinalOfChecksumPolicy( effectivePolicy.getChecksumPolicy() ) )
                    {
                        effectivePolicy.setChecksumPolicy( policy.getChecksumPolicy() );
                    }

                    if ( ordinalOfUpdatePolicy( policy.getUpdatePolicy() ) < ordinalOfUpdatePolicy( effectivePolicy.getUpdatePolicy() ) )
                    {
                        effectivePolicy.setUpdatePolicy( policy.getUpdatePolicy() );
                    }
                }
            }
        }

        return effectivePolicy;
    }

    private int ordinalOfChecksumPolicy( String policy )
    {
        if ( ArtifactRepositoryPolicy.CHECKSUM_POLICY_FAIL.equals( policy ) )
        {
            return 2;
        }
        else if ( ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE.equals( policy ) )
        {
            return 0;
        }
        else
        {
            return 1;
        }
    }

    private int ordinalOfUpdatePolicy( String policy )
    {
        if ( ArtifactRepositoryPolicy.UPDATE_POLICY_DAILY.equals( policy ) )
        {
            return 1440;
        }
        else if ( ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS.equals( policy ) )
        {
            return 0;
        }
        else if ( policy != null && policy.startsWith( ArtifactRepositoryPolicy.UPDATE_POLICY_INTERVAL ) )
        {
            return 60;
        }
        else
        {
            return Integer.MAX_VALUE;
        }
    }

    public void retrieve( ArtifactRepository repository, File destination, String remotePath, TransferListener downloadMonitor )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        getRemoteFile( repository, destination, remotePath, downloadMonitor, ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN, true );
    }

    public void publish( ArtifactRepository repository, File source, String remotePath, TransferListener downloadMonitor )
        throws TransferFailedException
    {
        putRemoteFile( repository, source, remotePath, downloadMonitor );
    }

    // Resolver

    public void resolve( Artifact artifact, List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository, TransferListener downloadMonitor, boolean force )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        if ( artifact == null )
        {
            return;
        }

        File destination;

        if ( Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
        {
            File systemFile = artifact.getFile();

            if ( systemFile == null )
            {
                throw new ArtifactNotFoundException( "System artifact: " + artifact + " has no file attached", artifact );
            }

            if ( !systemFile.exists() )
            {
                throw new ArtifactNotFoundException( "System artifact: " + artifact + " not found in path: " + systemFile, artifact );
            }

            if ( !systemFile.isFile() )
            {
                throw new ArtifactNotFoundException( "System artifact: " + artifact + " is not a file: " + systemFile, artifact );
            }

            artifact.setResolved( true );

            return;
        }

        if ( !artifact.isResolved() )
        {
            // ----------------------------------------------------------------------
            // Check for the existence of the artifact in the specified local
            // ArtifactRepository. If it is present then simply return as the
            // request for resolution has been satisfied.
            // ----------------------------------------------------------------------

            artifact = localRepository.find( artifact );

            if ( artifact.isFromAuthoritativeRepository() )
            {
                return;
            }

            if ( artifact.isSnapshot() && artifact.isResolved() )
            {
                return;
            }

            transformationManager.transformForResolve( artifact, remoteRepositories, localRepository );

            boolean localCopy = isLocalCopy( artifact );

            destination = artifact.getFile();

            boolean resolved = false;

            if ( force || !destination.exists() || ( artifact.isSnapshot() && !localCopy ) )
            {
                try
                {
                    if ( artifact.getRepository() != null )
                    {
                        // the transformations discovered the artifact - so use it exclusively
                        getArtifact( artifact, artifact.getRepository(), downloadMonitor );
                    }
                    else
                    {
                        getArtifact( artifact, remoteRepositories, downloadMonitor );
                    }

                    if ( !artifact.isResolved() && !destination.exists() )
                    {
                        throw new ArtifactResolutionException( "Failed to resolve artifact, possibly due to a repository list that is not appropriately equipped for this artifact's metadata.",
                                                               artifact, remoteRepositories );
                    }
                }
                catch ( ResourceDoesNotExistException e )
                {
                    throw new ArtifactNotFoundException( e.getMessage(), artifact, remoteRepositories, e );
                }
                catch ( TransferFailedException e )
                {
                    throw new ArtifactResolutionException( e.getMessage(), artifact, remoteRepositories, e );
                }

                resolved = true;
            }

            if ( destination.exists() )
            {
                artifact.setResolved( true );
            }

            // 1.0-SNAPSHOT
            //
            // 1)         pom = 1.0-SoNAPSHOT
            // 2)         pom = 1.0-yyyymmdd.hhmmss
            // 3) baseVersion = 1.0-SNAPSHOT
            if ( artifact.isSnapshot() && !artifact.getBaseVersion().equals( artifact.getVersion() ) )
            {
                String version = artifact.getVersion();

                // 1.0-SNAPSHOT
                artifact.selectVersion( artifact.getBaseVersion() );

                // Make a file with a 1.0-SNAPSHOT format
                File copy = new File( localRepository.getBasedir(), localRepository.pathOf( artifact ) );

                // if the timestamped version was resolved or the copy doesn't exist then copy a version
                // of the file like 1.0-SNAPSHOT. Even if there is a timestamped version the non-timestamped
                // version will be created.
                if ( resolved || !copy.exists() )
                {
                    // recopy file if it was reresolved, or doesn't exist.
                    try
                    {
                        FileUtils.copyFile( destination, copy );

                        copy.setLastModified( destination.lastModified() );
                    }
                    catch ( IOException e )
                    {
                        throw new ArtifactResolutionException( "Unable to copy resolved artifact for local use: " + e.getMessage(), artifact, remoteRepositories, e );
                    }
                }

                // We are only going to use the 1.0-SNAPSHOT version
                artifact.setFile( copy );

                // Set the version to the 1.0-SNAPSHOT version
                artifact.selectVersion( version );
            }
        }
    }

    private ArtifactResolutionResult resolveInternal( ArtifactResolutionRequest request )
    {
        Artifact rootArtifact = request.getArtifact();
        Set<Artifact> artifacts = request.getArtifactDependencies();
        Map managedVersions = request.getManagedVersionMap();
        ArtifactRepository localRepository = request.getLocalRepository();
        List<ArtifactRepository> remoteRepositories = request.getRemoteRepostories();
        List<ResolutionListener> listeners = request.getListeners();
        ArtifactFilter filter = request.getFilter();

        //TODO: hack because metadata isn't generated in m2e correctly and i want to run the maven i have in the workspace
        if ( source == null )
        {
            try
            {
                source = container.lookup( ArtifactMetadataSource.class );
            }
            catch ( ComponentLookupException e )
            {
                e.printStackTrace();
                // won't happen
            }
        }

        if ( listeners == null )
        {
            listeners = new ArrayList<ResolutionListener>();

            if ( logger.isDebugEnabled() )
            {
                listeners.add( new DebugResolutionListener( logger ) );
            }
        }

        ArtifactResolutionResult result = new ArtifactResolutionResult();

        // The root artifact may, or may not be resolved so we need to check before we attempt to resolve.
        // This is often an artifact like a POM that is taken from disk and we already have hold of the
        // file reference. But this may be a Maven Plugin that we need to resolve from a remote repository
        // as well as its dependencies.

        if ( request.isResolveRoot() && rootArtifact.getFile() == null )
        {
            try
            {
                resolve( rootArtifact, remoteRepositories, localRepository, null, false );
            }
            catch ( ArtifactResolutionException e )
            {
                result.addErrorArtifactException( e );
                return result;
            }
            catch ( ArtifactNotFoundException e )
            {
                result.addMissingArtifact( request.getArtifact() );
                return result;
            }
        }

        if ( request.isResolveTransitively() )
        {
            try
            {
                Set<Artifact> directArtifacts = source.retrieve( rootArtifact, localRepository, remoteRepositories ).getArtifacts();

                if ( artifacts == null || artifacts.isEmpty() )
                {
                    artifacts = directArtifacts;
                }
                else
                {
                    List<Artifact> allArtifacts = new ArrayList<Artifact>();
                    allArtifacts.addAll( artifacts );
                    allArtifacts.addAll( directArtifacts );

                    Map<String, Artifact> mergedArtifacts = new LinkedHashMap<String, Artifact>();
                    for ( Artifact artifact : allArtifacts )
                    {
                        String conflictId = artifact.getDependencyConflictId();
                        if ( !mergedArtifacts.containsKey( conflictId ) )
                        {
                            mergedArtifacts.put( conflictId, artifact );
                        }
                    }

                    artifacts = new LinkedHashSet<Artifact>( mergedArtifacts.values() );
                }
            }
            catch ( ArtifactMetadataRetrievalException e )
            {
                // need to add metadata resolution exception
                return result;
            }
        }

        if ( artifacts == null || artifacts.isEmpty() )
        {
            if ( request.isResolveRoot() )
            {
                result.addArtifact( rootArtifact );
            }
            return result;
        }

        // After the collection we will have the artifact object in the result but they will not be resolved yet.
        result = collect( artifacts, rootArtifact, managedVersions, localRepository, remoteRepositories, source, filter, listeners, null );

        // We have metadata retrieval problems, or there are cycles that have been detected
        // so we give this back to the calling code and let them deal with this information
        // appropriately.

        if ( result.hasMetadataResolutionExceptions() || result.hasVersionRangeViolations() || result.hasCircularDependencyExceptions() )
        {
            return result;
        }

        if ( result.getArtifacts() != null )
        {
            for ( Artifact artifact : result.getArtifacts() )
            {
                try
                {
                    resolve( artifact, remoteRepositories, localRepository, request.getTransferListener(), false );
                }
                catch ( ArtifactNotFoundException anfe )
                {
                    // These are cases where the artifact just isn't present in any of the remote repositories
                    // because it wasn't deployed, or it was deployed in the wrong place.

                    result.addMissingArtifact( artifact );
                }
                catch ( ArtifactResolutionException e )
                {
                    // This is really a wagon TransferFailedException so something went wrong after we successfully
                    // retrieved the metadata.

                    result.addErrorArtifactException( e );
                }
            }
        }

        // We want to send the root artifact back in the result but we need to do this after the other dependencies
        // have been resolved.
        if ( request.isResolveRoot() )
        {
            // Add the root artifact (as the first artifact to retain logical order of class path!)
            Set<Artifact> allArtifacts = new LinkedHashSet<Artifact>();
            allArtifacts.add( rootArtifact );
            allArtifacts.addAll( result.getArtifacts() );
            result.setArtifacts( allArtifacts );
        }

        return result;
    }

    private boolean isLocalCopy( Artifact artifact )
    {
        boolean localCopy = false;

        for ( ArtifactMetadata m : artifact.getMetadataList() )
        {
            if ( m instanceof SnapshotArtifactRepositoryMetadata )
            {
                SnapshotArtifactRepositoryMetadata snapshotMetadata = (SnapshotArtifactRepositoryMetadata) m;

                Metadata metadata = snapshotMetadata.getMetadata();

                if ( metadata != null )
                {
                    Versioning versioning = metadata.getVersioning();

                    if ( versioning != null )
                    {
                        Snapshot snapshot = versioning.getSnapshot();

                        if ( snapshot != null )
                        {
                            // TODO is it possible to have more than one SnapshotArtifactRepositoryMetadata
                            localCopy = snapshot.isLocalCopy();
                        }
                    }
                }
            }
        }

        return localCopy;
    }

    // Collector

    public ArtifactResolutionResult collect( Set<Artifact> artifacts, Artifact originatingArtifact, Map managedVersions, ArtifactRepository localRepository,
                                             List<ArtifactRepository> remoteRepositories, ArtifactMetadataSource source, ArtifactFilter filter, List<ResolutionListener> listeners,
                                             List<ConflictResolver> conflictResolvers )
    {
        ArtifactResolutionResult result = new ArtifactResolutionResult();

        result.ListOriginatingArtifact( originatingArtifact );

        if ( conflictResolvers == null )
        {
            conflictResolvers = Collections.singletonList( defaultConflictResolver );
        }

        Map<Object, List<ResolutionNode>> resolvedArtifacts = new LinkedHashMap<Object, List<ResolutionNode>>();

        ResolutionNode root = new ResolutionNode( originatingArtifact, remoteRepositories );

        try
        {
            root.addDependencies( artifacts, remoteRepositories, filter );
        }
        catch ( CyclicDependencyException e )
        {
            result.addCircularDependencyException( e );

            return result;
        }
        catch ( OverConstrainedVersionException e )
        {
            result.addVersionRangeViolation( e );

            return result;
        }

        ManagedVersionMap versionMap = getManagedVersionsMap( originatingArtifact, managedVersions );

        try
        {
            recurse( result, root, resolvedArtifacts, versionMap, localRepository, remoteRepositories, source, filter, listeners, conflictResolvers );
        }
        catch ( CyclicDependencyException e )
        {
            logger.debug( "While recursing: " + e.getMessage(), e );
            result.addCircularDependencyException( e );
        }
        catch ( OverConstrainedVersionException e )
        {
            logger.debug( "While recursing: " + e.getMessage(), e );
            result.addVersionRangeViolation( e );
        }
        catch ( ArtifactResolutionException e )
        {
            logger.debug( "While recursing: " + e.getMessage(), e );
            result.addErrorArtifactException( e );
        }

        Set<ResolutionNode> set = new LinkedHashSet<ResolutionNode>();

        for ( List<ResolutionNode> nodes : resolvedArtifacts.values() )
        {
            for ( ResolutionNode node : nodes )
            {
                if ( !node.equals( root ) && node.isActive() )
                {
                    Artifact artifact = node.getArtifact();

                    try
                    {
                        if ( node.filterTrail( filter ) )
                        {
                            // If it was optional and not a direct dependency,
                            // we don't add it or its children, just allow the update of the version and artifactScope
                            if ( node.isChildOfRootNode() || !artifact.isOptional() )
                            {
                                artifact.setDependencyTrail( node.getDependencyTrail() );

                                set.add( node );

                                // This is required right now.
                                result.addArtifact( artifact );
                                result.addRequestedArtifact( artifact );
                            }
                        }
                    }
                    catch ( OverConstrainedVersionException e )
                    {
                        result.addVersionRangeViolation( e );
                    }
                }
            }
        }

        return result;
    }

    /**
     * Get the map of managed versions, removing the originating artifact if it is also in managed
     * versions
     * 
     * @param originatingArtifact artifact we are processing
     * @param managedVersions original managed versions
     */
    private ManagedVersionMap getManagedVersionsMap( Artifact originatingArtifact, Map managedVersions )
    {
        ManagedVersionMap versionMap;
        if ( ( managedVersions != null ) && ( managedVersions instanceof ManagedVersionMap ) )
        {
            versionMap = (ManagedVersionMap) managedVersions;
        }
        else
        {
            versionMap = new ManagedVersionMap( managedVersions );
        }

        /* remove the originating artifact if it is also in managed versions to avoid being modified during resolution */
        Artifact managedOriginatingArtifact = (Artifact) versionMap.get( originatingArtifact.getDependencyConflictId() );

        if ( managedOriginatingArtifact != null )
        {
            // TODO we probably want to warn the user that he is building an artifact with
            // different values than in dependencyManagement
            if ( managedVersions instanceof ManagedVersionMap )
            {
                /* avoid modifying the managedVersions parameter creating a new map */
                versionMap = new ManagedVersionMap( managedVersions );
            }
            versionMap.remove( originatingArtifact.getDependencyConflictId() );
        }

        return versionMap;
    }

    private void recurse( ArtifactResolutionResult result, ResolutionNode node, Map<Object, List<ResolutionNode>> resolvedArtifacts, ManagedVersionMap managedVersions,
                          ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories, ArtifactMetadataSource source, ArtifactFilter filter, List<ResolutionListener> listeners,
                          List<ConflictResolver> conflictResolvers )
        throws ArtifactResolutionException
    {
        fireEvent( ResolutionListener.TEST_ARTIFACT, listeners, node );

        Object key = node.getKey();

        // TODO: Does this check need to happen here? Had to add the same call
        // below when we iterate on child nodes -- will that suffice?
        if ( managedVersions.containsKey( key ) )
        {
            manageArtifact( node, managedVersions, listeners );
        }

        List<ResolutionNode> previousNodes = resolvedArtifacts.get( key );

        if ( previousNodes != null )
        {
            for ( ResolutionNode previous : previousNodes )
            {
                try
                {
                    if ( previous.isActive() )
                    {
                        // Version mediation
                        VersionRange previousRange = previous.getArtifact().getVersionRange();
                        VersionRange currentRange = node.getArtifact().getVersionRange();

                        if ( ( previousRange != null ) && ( currentRange != null ) )
                        {
                            // TODO: shouldn't need to double up on this work, only done for simplicity of handling
                            // recommended
                            // version but the restriction is identical
                            VersionRange newRange = previousRange.restrict( currentRange );
                            // TODO: ick. this forces the OCE that should have come from the previous call. It is still
                            // correct
                            if ( newRange.isSelectedVersionKnown( previous.getArtifact() ) )
                            {
                                fireEvent( ResolutionListener.RESTRICT_RANGE, listeners, node, previous.getArtifact(), newRange );
                            }
                            previous.getArtifact().setVersionRange( newRange );
                            node.getArtifact().setVersionRange( currentRange.restrict( previousRange ) );

                            // Select an appropriate available version from the (now restricted) range
                            // Note this version was selected before to get the appropriate POM
                            // But it was reset by the call to setVersionRange on restricting the version
                            ResolutionNode[] resetNodes = { previous, node };
                            for ( int j = 0; j < 2; j++ )
                            {
                                Artifact resetArtifact = resetNodes[j].getArtifact();

                                // MNG-2123: if the previous node was not a range, then it wouldn't have any available
                                // versions. We just clobbered the selected version above. (why? i have no idea.)
                                // So since we are here and this is ranges we must go figure out the version (for a
                                // third time...)
                                if ( resetArtifact.getVersion() == null && resetArtifact.getVersionRange() != null )
                                {

                                    // go find the version. This is a total hack. See previous comment.
                                    List<ArtifactVersion> versions = resetArtifact.getAvailableVersions();
                                    if ( versions == null )
                                    {
                                        try
                                        {
                                            versions = source.retrieveAvailableVersions( resetArtifact, localRepository, remoteRepositories );
                                            resetArtifact.setAvailableVersions( versions );
                                        }
                                        catch ( ArtifactMetadataRetrievalException e )
                                        {
                                            resetArtifact.setDependencyTrail( node.getDependencyTrail() );
                                            throw new ArtifactResolutionException( "Unable to get dependency information: " + e.getMessage(), resetArtifact, remoteRepositories, e );
                                        }
                                    }
                                    // end hack

                                    // MNG-2861: match version can return null
                                    ArtifactVersion selectedVersion = resetArtifact.getVersionRange().matchVersion( resetArtifact.getAvailableVersions() );
                                    if ( selectedVersion != null )
                                    {
                                        resetArtifact.selectVersion( selectedVersion.toString() );
                                    }
                                    else
                                    {
                                        throw new OverConstrainedVersionException( " Unable to find a version in " + resetArtifact.getAvailableVersions() + " to match the range "
                                            + resetArtifact.getVersionRange(), resetArtifact );
                                    }

                                    fireEvent( ResolutionListener.SELECT_VERSION_FROM_RANGE, listeners, resetNodes[j] );
                                }
                            }
                        }

                        // Conflict Resolution
                        ResolutionNode resolved = null;
                        for ( Iterator j = conflictResolvers.iterator(); ( resolved == null ) && j.hasNext(); )
                        {
                            ConflictResolver conflictResolver = (ConflictResolver) j.next();

                            resolved = conflictResolver.resolveConflict( previous, node );
                        }

                        if ( resolved == null )
                        {
                            // TODO: add better exception that can detail the two conflicting artifacts
                            ArtifactResolutionException are = new ArtifactResolutionException( "Cannot resolve artifact version conflict between " + previous.getArtifact().getVersion() + " and "
                                + node.getArtifact().getVersion(), previous.getArtifact() );
                            result.addVersionRangeViolation( are );
                        }

                        if ( ( resolved != previous ) && ( resolved != node ) )
                        {
                            // TODO: add better exception
                            result.addVersionRangeViolation( new ArtifactResolutionException( "Conflict resolver returned unknown resolution node: ", resolved.getArtifact() ) );
                        }

                        // TODO: should this be part of mediation?
                        // previous one is more dominant
                        ResolutionNode nearest;
                        ResolutionNode farthest;

                        if ( resolved == previous )
                        {
                            nearest = previous;
                            farthest = node;
                        }
                        else
                        {
                            nearest = node;
                            farthest = previous;
                        }

                        if ( checkScopeUpdate( farthest, nearest, listeners ) )
                        {
                            // if we need to update artifactScope of nearest to use farthest artifactScope, use the
                            // nearest version, but farthest artifactScope
                            nearest.disable();
                            farthest.getArtifact().setVersion( nearest.getArtifact().getVersion() );
                            fireEvent( ResolutionListener.OMIT_FOR_NEARER, listeners, nearest, farthest.getArtifact() );
                        }
                        else
                        {
                            farthest.disable();
                            fireEvent( ResolutionListener.OMIT_FOR_NEARER, listeners, farthest, nearest.getArtifact() );
                        }
                    }
                }
                catch ( OverConstrainedVersionException e )
                {
                    result.addVersionRangeViolation( e );
                }
            }
        }
        else
        {
            previousNodes = new ArrayList<ResolutionNode>();

            resolvedArtifacts.put( key, previousNodes );
        }
        previousNodes.add( node );

        if ( node.isActive() )
        {
            fireEvent( ResolutionListener.INCLUDE_ARTIFACT, listeners, node );
        }

        // don't pull in the transitive deps of a system-scoped dependency.
        if ( node.isActive() && !Artifact.SCOPE_SYSTEM.equals( node.getArtifact().getScope() ) )
        {
            fireEvent( ResolutionListener.PROCESS_CHILDREN, listeners, node );

            for ( Iterator i = node.getChildrenIterator(); i.hasNext(); )
            {
                ResolutionNode child = (ResolutionNode) i.next();

                try
                {

                    // We leave in optional ones, but don't pick up its dependencies
                    if ( !child.isResolved() && ( !child.getArtifact().isOptional() || child.isChildOfRootNode() ) )
                    {
                        Artifact artifact = child.getArtifact();
                        List<ArtifactRepository> childRemoteRepositories = child.getRemoteRepositories();

                        try
                        {
                            Object childKey;
                            do
                            {
                                childKey = child.getKey();

                                if ( managedVersions.containsKey( childKey ) )
                                {
                                    // If this child node is a managed dependency, ensure
                                    // we are using the dependency management version
                                    // of this child if applicable b/c we want to use the
                                    // managed version's POM, *not* any other version's POM.
                                    // We retrieve the POM below in the retrieval step.
                                    manageArtifact( child, managedVersions, listeners );

                                    // Also, we need to ensure that any exclusions it presents are
                                    // added to the artifact before we retrive the metadata
                                    // for the artifact; otherwise we may end up with unwanted
                                    // dependencies.
                                    Artifact ma = (Artifact) managedVersions.get( childKey );
                                    ArtifactFilter managedExclusionFilter = ma.getDependencyFilter();
                                    if ( null != managedExclusionFilter )
                                    {
                                        if ( null != artifact.getDependencyFilter() )
                                        {
                                            AndArtifactFilter aaf = new AndArtifactFilter();
                                            aaf.add( artifact.getDependencyFilter() );
                                            aaf.add( managedExclusionFilter );
                                            artifact.setDependencyFilter( aaf );
                                        }
                                        else
                                        {
                                            artifact.setDependencyFilter( managedExclusionFilter );
                                        }
                                    }
                                }

                                if ( artifact.getVersion() == null )
                                {
                                    // set the recommended version
                                    // TODO: maybe its better to just pass the range through to retrieval and use a
                                    // transformation?
                                    ArtifactVersion version;
                                    if ( !artifact.isSelectedVersionKnown() )
                                    {
                                        List<ArtifactVersion> versions = artifact.getAvailableVersions();
                                        if ( versions == null )
                                        {
                                            versions = source.retrieveAvailableVersions( artifact, localRepository, childRemoteRepositories );
                                            artifact.setAvailableVersions( versions );
                                        }

                                        Collections.sort( versions );

                                        VersionRange versionRange = artifact.getVersionRange();

                                        version = versionRange.matchVersion( versions );

                                        if ( version == null )
                                        {
                                            // Getting the dependency trail so it can be logged in the exception
                                            artifact.setDependencyTrail( node.getDependencyTrail() );

                                            if ( versions.isEmpty() )
                                            {
                                                throw new OverConstrainedVersionException( "No versions are present in the repository for the artifact with a range " + versionRange, artifact,
                                                                                           childRemoteRepositories );
                                            }

                                            throw new OverConstrainedVersionException( "Couldn't find a version in " + versions + " to match range " + versionRange, artifact, childRemoteRepositories );
                                        }
                                    }
                                    else
                                    {
                                        version = artifact.getSelectedVersion();
                                    }

                                    artifact.selectVersion( version.toString() );
                                    fireEvent( ResolutionListener.SELECT_VERSION_FROM_RANGE, listeners, child );
                                }
                            }
                            while ( !childKey.equals( child.getKey() ) );

                            artifact.setDependencyTrail( node.getDependencyTrail() );

                            ResolutionGroup rGroup = source.retrieve( artifact, localRepository, childRemoteRepositories );

                            // TODO might be better to have source.retrieve() throw a specific exception for this
                            // situation
                            // and catch here rather than have it return null
                            if ( rGroup == null )
                            {
                                // relocated dependency artifact is declared excluded, no need to add and recurse
                                // further
                                continue;
                            }

                            child.addDependencies( rGroup.getArtifacts(), rGroup.getResolutionRepositories(), filter );

                        }
                        catch ( CyclicDependencyException e )
                        {
                            // would like to throw this, but we have crappy stuff in the repo

                            fireEvent( ResolutionListener.OMIT_FOR_CYCLE, listeners, new ResolutionNode( e.getArtifact(), childRemoteRepositories, child ) );
                        }
                        catch ( ArtifactMetadataRetrievalException e )
                        {
                            artifact.setDependencyTrail( node.getDependencyTrail() );

                            throw new ArtifactResolutionException( "Unable to get dependency information: " + e.getMessage(), artifact, childRemoteRepositories, e );
                        }

                        recurse( result, child, resolvedArtifacts, managedVersions, localRepository, childRemoteRepositories, source, filter, listeners, conflictResolvers );
                    }
                }
                catch ( OverConstrainedVersionException e )
                {
                    result.addVersionRangeViolation( e );
                }
                catch ( ArtifactResolutionException e )
                {
                    result.addMetadataResolutionException( e );
                }
            }

            fireEvent( ResolutionListener.FINISH_PROCESSING_CHILDREN, listeners, node );
        }
    }

    private void manageArtifact( ResolutionNode node, ManagedVersionMap managedVersions, List<ResolutionListener> listeners )
    {
        Artifact artifact = (Artifact) managedVersions.get( node.getKey() );

        // Before we update the version of the artifact, we need to know
        // whether we are working on a transitive dependency or not. This
        // allows depMgmt to always override transitive dependencies, while
        // explicit child override depMgmt (viz. depMgmt should only
        // provide defaults to children, but should override transitives).
        // We can do this by calling isChildOfRootNode on the current node.

        if ( ( artifact.getVersion() != null ) && ( !node.isChildOfRootNode() || node.getArtifact().getVersion() == null ) )
        {
            fireEvent( ResolutionListener.MANAGE_ARTIFACT_VERSION, listeners, node, artifact );
            node.getArtifact().setVersion( artifact.getVersion() );
        }

        if ( ( artifact.getScope() != null ) && ( !node.isChildOfRootNode() || node.getArtifact().getScope() == null ) )
        {
            fireEvent( ResolutionListener.MANAGE_ARTIFACT_SCOPE, listeners, node, artifact );
            node.getArtifact().setScope( artifact.getScope() );
        }

        if ( Artifact.SCOPE_SYSTEM.equals( node.getArtifact().getScope() ) && ( node.getArtifact().getFile() == null ) && ( artifact.getFile() != null ) )
        {
            fireEvent( ResolutionListener.MANAGE_ARTIFACT_SYSTEM_PATH, listeners, node, artifact );
            node.getArtifact().setFile( artifact.getFile() );
        }
    }

    /**
     * Check if the artifactScope needs to be updated. <a href=
     * "http://docs.codehaus.org/x/IGU#DependencyMediationandConflictResolution-Scoperesolution"
     * >More info</a>.
     * 
     * @param farthest farthest resolution node
     * @param nearest nearest resolution node
     * @param listeners
     */
    boolean checkScopeUpdate( ResolutionNode farthest, ResolutionNode nearest, List<ResolutionListener> listeners )
    {
        boolean updateScope = false;
        Artifact farthestArtifact = farthest.getArtifact();
        Artifact nearestArtifact = nearest.getArtifact();

        /* farthest is runtime and nearest has lower priority, change to runtime */
        if ( Artifact.SCOPE_RUNTIME.equals( farthestArtifact.getScope() )
            && ( Artifact.SCOPE_TEST.equals( nearestArtifact.getScope() ) || Artifact.SCOPE_PROVIDED.equals( nearestArtifact.getScope() ) ) )
        {
            updateScope = true;
        }

        /* farthest is compile and nearest is not (has lower priority), change to compile */
        if ( Artifact.SCOPE_COMPILE.equals( farthestArtifact.getScope() ) && !Artifact.SCOPE_COMPILE.equals( nearestArtifact.getScope() ) )
        {
            updateScope = true;
        }

        /* current POM rules all, if nearest is in current pom, do not update its artifactScope */
        if ( ( nearest.getDepth() < 2 ) && updateScope )
        {
            updateScope = false;

            fireEvent( ResolutionListener.UPDATE_SCOPE_CURRENT_POM, listeners, nearest, farthestArtifact );
        }

        if ( updateScope )
        {
            fireEvent( ResolutionListener.UPDATE_SCOPE, listeners, nearest, farthestArtifact );

            // previously we cloned the artifact, but it is more effecient to just update the artifactScope
            // if problems are later discovered that the original object needs its original artifactScope value, cloning
            // may
            // again be appropriate
            nearestArtifact.setScope( farthestArtifact.getScope() );
        }

        return updateScope;
    }

    private void fireEvent( int event, List<ResolutionListener> listeners, ResolutionNode node )
    {
        fireEvent( event, listeners, node, null );
    }

    private void fireEvent( int event, List<ResolutionListener> listeners, ResolutionNode node, Artifact replacement )
    {
        fireEvent( event, listeners, node, replacement, null );
    }

    private void fireEvent( int event, List<ResolutionListener> listeners, ResolutionNode node, Artifact replacement, VersionRange newRange )
    {
        for ( ResolutionListener listener : listeners )
        {
            switch ( event )
            {
                case ResolutionListener.TEST_ARTIFACT:
                    listener.testArtifact( node.getArtifact() );
                    break;
                case ResolutionListener.PROCESS_CHILDREN:
                    listener.startProcessChildren( node.getArtifact() );
                    break;
                case ResolutionListener.FINISH_PROCESSING_CHILDREN:
                    listener.endProcessChildren( node.getArtifact() );
                    break;
                case ResolutionListener.INCLUDE_ARTIFACT:
                    listener.includeArtifact( node.getArtifact() );
                    break;
                case ResolutionListener.OMIT_FOR_NEARER:
                    listener.omitForNearer( node.getArtifact(), replacement );
                    break;
                case ResolutionListener.OMIT_FOR_CYCLE:
                    listener.omitForCycle( node.getArtifact() );
                    break;
                case ResolutionListener.UPDATE_SCOPE:
                    listener.updateScope( node.getArtifact(), replacement.getScope() );
                    break;
                case ResolutionListener.UPDATE_SCOPE_CURRENT_POM:
                    listener.updateScopeCurrentPom( node.getArtifact(), replacement.getScope() );
                    break;
                case ResolutionListener.MANAGE_ARTIFACT_VERSION:
                    if ( listener instanceof ResolutionListenerForDepMgmt )
                    {
                        ResolutionListenerForDepMgmt asImpl = (ResolutionListenerForDepMgmt) listener;
                        asImpl.manageArtifactVersion( node.getArtifact(), replacement );
                    }
                    else
                    {
                        listener.manageArtifact( node.getArtifact(), replacement );
                    }
                    break;
                case ResolutionListener.MANAGE_ARTIFACT_SCOPE:
                    if ( listener instanceof ResolutionListenerForDepMgmt )
                    {
                        ResolutionListenerForDepMgmt asImpl = (ResolutionListenerForDepMgmt) listener;
                        asImpl.manageArtifactScope( node.getArtifact(), replacement );
                    }
                    else
                    {
                        listener.manageArtifact( node.getArtifact(), replacement );
                    }
                    break;
                case ResolutionListener.MANAGE_ARTIFACT_SYSTEM_PATH:
                    if ( listener instanceof ResolutionListenerForDepMgmt )
                    {
                        ResolutionListenerForDepMgmt asImpl = (ResolutionListenerForDepMgmt) listener;
                        asImpl.manageArtifactSystemPath( node.getArtifact(), replacement );
                    }
                    else
                    {
                        listener.manageArtifact( node.getArtifact(), replacement );
                    }
                    break;
                case ResolutionListener.SELECT_VERSION_FROM_RANGE:
                    listener.selectVersionFromRange( node.getArtifact() );
                    break;
                case ResolutionListener.RESTRICT_RANGE:
                    if ( node.getArtifact().getVersionRange().hasRestrictions() || replacement.getVersionRange().hasRestrictions() )
                    {
                        listener.restrictRange( node.getArtifact(), replacement, newRange );
                    }
                    break;
                default:
                    throw new IllegalStateException( "Unknown event: " + event );
            }
        }
    }

    public ArtifactResolutionResult collect( Set<Artifact> artifacts, Artifact originatingArtifact, Map managedVersions, ArtifactRepository localRepository,
                                             List<ArtifactRepository> remoteRepositories, ArtifactMetadataSource source, ArtifactFilter filter, List<ResolutionListener> listeners )
    {
        return collect( artifacts, originatingArtifact, managedVersions, localRepository, remoteRepositories, source, filter, listeners, null );
    }

    // Factory

    public Artifact createArtifact( String groupId, String artifactId, String version, String scope, String type )
    {
        return createArtifact( groupId, artifactId, version, scope, type, null, null );
    }

    public Artifact createArtifactWithClassifier( String groupId, String artifactId, String version, String type, String classifier )
    {
        return createArtifact( groupId, artifactId, version, null, type, classifier, null );
    }

    public Artifact createDependencyArtifact( String groupId, String artifactId, VersionRange versionRange, String type, String classifier, String scope )
    {
        return createArtifact( groupId, artifactId, versionRange, type, classifier, scope, null );
    }

    public Artifact createDependencyArtifact( String groupId, String artifactId, VersionRange versionRange, String type, String classifier, String scope, boolean optional )
    {
        return createArtifact( groupId, artifactId, versionRange, type, classifier, scope, null, optional );
    }

    public Artifact createDependencyArtifact( String groupId, String artifactId, VersionRange versionRange, String type, String classifier, String scope, String inheritedScope )
    {
        return createArtifact( groupId, artifactId, versionRange, type, classifier, scope, inheritedScope );
    }

    public Artifact createDependencyArtifact( String groupId, String artifactId, VersionRange versionRange, String type, String classifier, String scope, String inheritedScope, boolean optional )
    {
        return createArtifact( groupId, artifactId, versionRange, type, classifier, scope, inheritedScope, optional );
    }

    public Artifact createBuildArtifact( String groupId, String artifactId, String version, String packaging )
    {
        return createArtifact( groupId, artifactId, version, null, packaging, null, null );
    }

    public Artifact createProjectArtifact( String groupId, String artifactId, String version )
    {
        return createProjectArtifact( groupId, artifactId, version, null );
    }

    public Artifact createParentArtifact( String groupId, String artifactId, String version )
    {
        return createProjectArtifact( groupId, artifactId, version );
    }

    public Artifact createPluginArtifact( String groupId, String artifactId, VersionRange versionRange )
    {
        return createArtifact( groupId, artifactId, versionRange, "maven-plugin", null, Artifact.SCOPE_RUNTIME, null );
    }

    public Artifact createProjectArtifact( String groupId, String artifactId, String version, String scope )
    {
        return createArtifact( groupId, artifactId, version, scope, "pom" );
    }

    public Artifact createExtensionArtifact( String groupId, String artifactId, VersionRange versionRange )
    {
        return createArtifact( groupId, artifactId, versionRange, "jar", null, Artifact.SCOPE_RUNTIME, null );
    }

    private Artifact createArtifact( String groupId, String artifactId, String version, String scope, String type, String classifier, String inheritedScope )
    {
        VersionRange versionRange = null;
        if ( version != null )
        {
            versionRange = VersionRange.createFromVersion( version );
        }
        return createArtifact( groupId, artifactId, versionRange, type, classifier, scope, inheritedScope );
    }

    private Artifact createArtifact( String groupId, String artifactId, VersionRange versionRange, String type, String classifier, String scope, String inheritedScope )
    {
        return createArtifact( groupId, artifactId, versionRange, type, classifier, scope, inheritedScope, false );
    }

    private Artifact createArtifact( String groupId, String artifactId, VersionRange versionRange, String type, String classifier, String scope, String inheritedScope, boolean optional )
    {
        String desiredScope = Artifact.SCOPE_RUNTIME;

        if ( inheritedScope == null )
        {
            desiredScope = scope;
        }
        else if ( Artifact.SCOPE_TEST.equals( scope ) || Artifact.SCOPE_PROVIDED.equals( scope ) )
        {
            return null;
        }
        else if ( Artifact.SCOPE_COMPILE.equals( scope ) && Artifact.SCOPE_COMPILE.equals( inheritedScope ) )
        {
            // added to retain compile artifactScope. Remove if you want compile inherited as runtime
            desiredScope = Artifact.SCOPE_COMPILE;
        }

        if ( Artifact.SCOPE_TEST.equals( inheritedScope ) )
        {
            desiredScope = Artifact.SCOPE_TEST;
        }

        if ( Artifact.SCOPE_PROVIDED.equals( inheritedScope ) )
        {
            desiredScope = Artifact.SCOPE_PROVIDED;
        }

        if ( Artifact.SCOPE_SYSTEM.equals( scope ) )
        {
            // system scopes come through unchanged...
            desiredScope = Artifact.SCOPE_SYSTEM;
        }

        ArtifactHandler handler = artifactHandlerManager.getArtifactHandler( type );

        // Create a Maven Artifact and an adapter compat -> repository

        return new DefaultArtifact( groupId, artifactId, versionRange, desiredScope, type, classifier, handler, optional );
    }

    // WagonManager

    private static final String[] CHECKSUM_IDS = { "md5", "sha1" };

    /** have to match the CHECKSUM_IDS */
    private static final String[] CHECKSUM_ALGORITHMS = { "MD5", "SHA-1" };

    @Requirement(role = Wagon.class)
    private Map<String, Wagon> wagons;

    @Requirement
    private UpdateCheckManager updateCheckManager;

    //
    // Retriever
    //   
    public void getArtifact( Artifact artifact, ArtifactRepository repository, TransferListener downloadMonitor )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        String remotePath = repository.pathOf( artifact );

        ArtifactRepositoryPolicy policy = artifact.isSnapshot() ? repository.getSnapshots() : repository.getReleases();

        boolean updateCheckIsRequired = updateCheckManager.isUpdateRequired( artifact, repository );

        if ( !policy.isEnabled() )
        {
            logger.debug( "Skipping disabled repository " + repository.getId() );
        }

        // If the artifact is a snapshot, we need to determine whether it's time to check this repository for an update:
        // 1. If it's forced, then check
        // 2. If the updateInterval has been exceeded since the last check for this artifact on this repository, then check.        
        else if ( artifact.isSnapshot() && updateCheckIsRequired )
        {
            logger.debug( "Trying repository " + repository.getId() );

            try
            {
                getRemoteFile( repository, artifact.getFile(), remotePath, downloadMonitor, policy.getChecksumPolicy(), false );
            }
            finally
            {
                updateCheckManager.touch( artifact, repository );
            }

            logger.debug( "  Artifact resolved" );

            artifact.setResolved( true );
        }

        // XXX: This is not really intended for the long term - unspecified POMs should be converted to failures
        //      meaning caching would be unnecessary. The code for this is here instead of the MavenMetadataSource
        //      to keep the logic related to update checks enclosed, and so to keep the rules reasonably consistent
        //      with release metadata
        else if ( "pom".equals( artifact.getType() ) && !artifact.getFile().exists() )
        {
            // if POM is not present locally, try and get it if it's forced, out of date, or has not been attempted yet  
            if ( updateCheckManager.isPomUpdateRequired( artifact, repository ) )
            {
                logger.debug( "Trying repository " + repository.getId() );

                try
                {
                    getRemoteFile( repository, artifact.getFile(), remotePath, downloadMonitor, policy.getChecksumPolicy(), false );
                }
                catch ( ResourceDoesNotExistException e )
                {
                    // cache the POM failure
                    updateCheckManager.touch( artifact, repository );

                    throw e;
                }

                logger.debug( "  Artifact resolved" );

                artifact.setResolved( true );
            }
            else
            {
                // cached failure - pass on the failure
                throw new ResourceDoesNotExistException( "Failure was cached in the local repository" );
            }
        }

        // If it's not a snapshot artifact, then we don't care what the force flag says. If it's on the local
        // system, it's resolved. Releases are presumed to be immutable, so release artifacts are not ever updated.
        // NOTE: This is NOT the case for metadata files on relese-only repositories. This metadata may contain information
        // about successive releases, so it should be checked using the same updateInterval/force characteristics as snapshot
        // artifacts, above.

        // don't write touch-file for release artifacts.
        else if ( !artifact.isSnapshot() )
        {
            logger.debug( "Trying repository " + repository.getId() );

            getRemoteFile( repository, artifact.getFile(), remotePath, downloadMonitor, policy.getChecksumPolicy(), false );

            logger.debug( "  Artifact resolved" );

            artifact.setResolved( true );
        }
    }

    public void getArtifact( Artifact artifact, List<ArtifactRepository> remoteRepositories, TransferListener downloadMonitor )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        for ( ArtifactRepository repository : remoteRepositories )
        {
            try
            {
                getArtifact( artifact, repository, downloadMonitor );

                if ( artifact.isResolved() )
                {
                    break;
                }
            }
            catch ( ResourceDoesNotExistException e )
            {
                // This one we will eat when looking through remote repositories
                // because we want to cycle through them all before squawking.

                logger.debug( "Unable to get resource '" + artifact.getId() + "' from repository " + repository.getId() + " (" + repository.getUrl() + ")", e );
            }
            catch ( TransferFailedException e )
            {
                logger.debug( "Unable to get resource '" + artifact.getId() + "' from repository " + repository.getId() + " (" + repository.getUrl() + ")", e );
            }
        }

        // if it already exists locally we were just trying to force it - ignore the update
        if ( !artifact.getFile().exists() )
        {
            throw new ResourceDoesNotExistException( "Unable to download the artifact from any repository" );
        }
    }

    public void getArtifactMetadata( ArtifactMetadata metadata, ArtifactRepository repository, File destination, String checksumPolicy )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        String remotePath = repository.pathOfRemoteRepositoryMetadata( metadata );

        getRemoteFile( repository, destination, remotePath, null, checksumPolicy, true );
    }

    public void getArtifactMetadataFromDeploymentRepository( ArtifactMetadata metadata, ArtifactRepository repository, File destination, String checksumPolicy )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        String remotePath = repository.pathOfRemoteRepositoryMetadata( metadata );

        getRemoteFile( repository, destination, remotePath, null, checksumPolicy, true );
    }

    public void getRemoteFile( ArtifactRepository repository, File destination, String remotePath, TransferListener downloadMonitor, String checksumPolicy, boolean force )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        String protocol = repository.getProtocol();

        Wagon wagon;

        try
        {
            wagon = getWagon( protocol );
        }
        catch ( UnsupportedProtocolException e )
        {
            throw new TransferFailedException( "Unsupported Protocol: '" + protocol + "': " + e.getMessage(), e );
        }

        if ( downloadMonitor != null )
        {
            wagon.addTransferListener( downloadMonitor );
        }

        File temp = new File( destination + ".tmp" );

        temp.deleteOnExit();

        boolean downloaded = false;

        try
        {
            wagon.connect( new Repository( repository.getId(), repository.getUrl() ) );

            boolean firstRun = true;
            boolean retry = true;

            // this will run at most twice. The first time, the firstRun flag is turned off, and if the retry flag
            // is set on the first run, it will be turned off and not re-set on the second try. This is because the
            // only way the retry flag can be set is if ( firstRun == true ).
            while ( firstRun || retry )
            {
                ChecksumObserver md5ChecksumObserver = null;
                ChecksumObserver sha1ChecksumObserver = null;
                try
                {
                    // TODO: configure on repository
                    int i = 0;

                    md5ChecksumObserver = addChecksumObserver( wagon, CHECKSUM_ALGORITHMS[i++] );
                    sha1ChecksumObserver = addChecksumObserver( wagon, CHECKSUM_ALGORITHMS[i++] );

                    // reset the retry flag.
                    retry = false;

                    // This should take care of creating destination directory now on
                    if ( destination.exists() && !force )
                    {
                        try
                        {
                            downloaded = wagon.getIfNewer( remotePath, temp, destination.lastModified() );

                            if ( !downloaded )
                            {
                                // prevent additional checks of this artifact until it expires again
                                destination.setLastModified( System.currentTimeMillis() );
                            }
                        }
                        catch ( UnsupportedOperationException e )
                        {
                            // older wagons throw this. Just get() instead
                            wagon.get( remotePath, temp );

                            downloaded = true;
                        }
                    }
                    else
                    {
                        wagon.get( remotePath, temp );
                        downloaded = true;
                    }
                }
                finally
                {
                    wagon.removeTransferListener( md5ChecksumObserver );
                    wagon.removeTransferListener( sha1ChecksumObserver );
                }

                if ( downloaded )
                {
                    // keep the checksum files from showing up on the download monitor...
                    if ( downloadMonitor != null )
                    {
                        wagon.removeTransferListener( downloadMonitor );
                    }

                    // try to verify the SHA-1 checksum for this file.
                    try
                    {
                        verifyChecksum( sha1ChecksumObserver, destination, temp, remotePath, ".sha1", wagon );
                    }
                    catch ( ChecksumFailedException e )
                    {
                        // if we catch a ChecksumFailedException, it means the transfer/read succeeded, but the checksum
                        // doesn't match. This could be a problem with the server (ibiblio HTTP-200 error page), so we'll
                        // try this up to two times. On the second try, we'll handle it as a bona-fide error, based on the
                        // repository's checksum checking policy.
                        if ( firstRun )
                        {
                            logger.warn( "*** CHECKSUM FAILED - " + e.getMessage() + " - RETRYING" );
                            retry = true;
                        }
                        else
                        {
                            handleChecksumFailure( checksumPolicy, e.getMessage(), e.getCause() );
                        }
                    }
                    catch ( ResourceDoesNotExistException sha1TryException )
                    {
                        logger.debug( "SHA1 not found, trying MD5", sha1TryException );

                        // if this IS NOT a ChecksumFailedException, it was a problem with transfer/read of the checksum
                        // file...we'll try again with the MD5 checksum.
                        try
                        {
                            verifyChecksum( md5ChecksumObserver, destination, temp, remotePath, ".md5", wagon );
                        }
                        catch ( ChecksumFailedException e )
                        {
                            // if we also fail to verify based on the MD5 checksum, and the checksum transfer/read
                            // succeeded, then we need to determine whether to retry or handle it as a failure.
                            if ( firstRun )
                            {
                                retry = true;
                            }
                            else
                            {
                                handleChecksumFailure( checksumPolicy, e.getMessage(), e.getCause() );
                            }
                        }
                        catch ( ResourceDoesNotExistException md5TryException )
                        {
                            // this was a failed transfer, and we don't want to retry.
                            handleChecksumFailure( checksumPolicy, "Error retrieving checksum file for " + remotePath, md5TryException );
                        }
                    }

                    // reinstate the download monitor...
                    if ( downloadMonitor != null )
                    {
                        wagon.addTransferListener( downloadMonitor );
                    }
                }

                // unset the firstRun flag, so we don't get caught in an infinite loop...
                firstRun = false;
            }
        }
        catch ( ConnectionException e )
        {
            throw new TransferFailedException( "Connection failed: " + e.getMessage(), e );
        }
        catch ( AuthenticationException e )
        {
            throw new TransferFailedException( "Authentication failed: " + e.getMessage(), e );
        }
        catch ( AuthorizationException e )
        {
            throw new TransferFailedException( "Authorization failed: " + e.getMessage(), e );
        }
        finally
        {
            // Remove remaining TransferListener instances (checksum handlers removed in above finally clause)
            if ( downloadMonitor != null )
            {
                wagon.removeTransferListener( downloadMonitor );
            }

            disconnectWagon( wagon );

            releaseWagon( protocol, wagon );
        }

        if ( downloaded )
        {
            if ( !temp.exists() )
            {
                throw new ResourceDoesNotExistException( "Downloaded file does not exist: " + temp );
            }

            // The temporary file is named destination + ".tmp" and is done this way to ensure
            // that the temporary file is in the same file system as the destination because the
            // File.renameTo operation doesn't really work across file systems.
            // So we will attempt to do a File.renameTo for efficiency and atomicity, if this fails
            // then we will use a brute force copy and delete the temporary file.

            if ( !temp.renameTo( destination ) )
            {
                try
                {
                    FileUtils.copyFile( temp, destination );

                    temp.delete();
                }
                catch ( IOException e )
                {
                    throw new TransferFailedException( "Error copying temporary file to the final destination: " + e.getMessage(), e );
                }
            }
        }
    }

    //
    // Publisher
    //    
    public void putArtifact( File source, Artifact artifact, ArtifactRepository deploymentRepository, TransferListener downloadMonitor )
        throws TransferFailedException
    {
        putRemoteFile( deploymentRepository, source, deploymentRepository.pathOf( artifact ), downloadMonitor );
    }

    public void putArtifactMetadata( File source, ArtifactMetadata artifactMetadata, ArtifactRepository repository )
        throws TransferFailedException
    {
        logger.info( "Uploading " + artifactMetadata );
        putRemoteFile( repository, source, repository.pathOfRemoteRepositoryMetadata( artifactMetadata ), null );
    }

    public void putRemoteFile( ArtifactRepository repository, File source, String remotePath, TransferListener downloadMonitor )
        throws TransferFailedException
    {
        String protocol = repository.getProtocol();

        Wagon wagon;
        try
        {
            wagon = getWagon( protocol );
        }
        catch ( UnsupportedProtocolException e )
        {
            throw new TransferFailedException( "Unsupported Protocol: '" + protocol + "': " + e.getMessage(), e );
        }

        if ( downloadMonitor != null )
        {
            wagon.addTransferListener( downloadMonitor );
        }

        Map<String, ChecksumObserver> checksums = new HashMap<String, ChecksumObserver>( 2 );

        Map<String, String> sums = new HashMap<String, String>( 2 );

        // TODO: configure these on the repository
        for ( int i = 0; i < CHECKSUM_IDS.length; i++ )
        {
            checksums.put( CHECKSUM_IDS[i], addChecksumObserver( wagon, CHECKSUM_ALGORITHMS[i] ) );
        }

        try
        {
            try
            {
                wagon.connect( new Repository( repository.getId(), repository.getUrl() ) );

                wagon.put( source, remotePath );
            }
            finally
            {
                if ( downloadMonitor != null )
                {
                    wagon.removeTransferListener( downloadMonitor );
                }
            }

            // Pre-store the checksums as any future puts will overwrite them
            for ( String extension : checksums.keySet() )
            {
                ChecksumObserver observer = checksums.get( extension );
                sums.put( extension, observer.getActualChecksum() );
            }

            // We do this in here so we can checksum the artifact metadata too, otherwise it could be metadata itself
            for ( String extension : checksums.keySet() )
            {
                // TODO: shouldn't need a file intermediatary - improve wagon to take a stream
                File temp = File.createTempFile( "maven-artifact", null );
                temp.deleteOnExit();
                FileUtils.fileWrite( temp.getAbsolutePath(), "UTF-8", sums.get( extension ) );

                wagon.put( temp, remotePath + "." + extension );
            }
        }
        catch ( ConnectionException e )
        {
            throw new TransferFailedException( "Connection failed: " + e.getMessage(), e );
        }
        catch ( AuthenticationException e )
        {
            throw new TransferFailedException( "Authentication failed: " + e.getMessage(), e );
        }
        catch ( AuthorizationException e )
        {
            throw new TransferFailedException( "Authorization failed: " + e.getMessage(), e );
        }
        catch ( ResourceDoesNotExistException e )
        {
            throw new TransferFailedException( "Resource to deploy not found: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Error creating temporary file for deployment: " + e.getMessage(), e );
        }
        finally
        {
            // Remove every checksum listener
            for ( String aCHECKSUM_IDS : CHECKSUM_IDS )
            {
                TransferListener checksumListener = checksums.get( aCHECKSUM_IDS );
                if ( checksumListener != null )
                {
                    wagon.removeTransferListener( checksumListener );
                }
            }

            disconnectWagon( wagon );

            releaseWagon( protocol, wagon );
        }
    }

    private ChecksumObserver addChecksumObserver( Wagon wagon, String algorithm )
        throws TransferFailedException
    {
        try
        {
            ChecksumObserver checksumObserver = new ChecksumObserver( algorithm );
            wagon.addTransferListener( checksumObserver );
            return checksumObserver;
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new TransferFailedException( "Unable to add checksum for unsupported algorithm " + algorithm, e );
        }
    }

    private void handleChecksumFailure( String checksumPolicy, String message, Throwable cause )
        throws ChecksumFailedException
    {
        if ( ArtifactRepositoryPolicy.CHECKSUM_POLICY_FAIL.equals( checksumPolicy ) )
        {
            throw new ChecksumFailedException( message, cause );
        }
        else if ( !ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE.equals( checksumPolicy ) )
        {
            // warn if it is set to anything other than ignore
            logger.warn( "*** CHECKSUM FAILED - " + message + " - IGNORING" );
        }
        // otherwise it is ignore
    }

    private void verifyChecksum( ChecksumObserver checksumObserver, File destination, File tempDestination, String remotePath, String checksumFileExtension, Wagon wagon )
        throws ResourceDoesNotExistException, TransferFailedException, AuthorizationException
    {
        try
        {
            // grab it first, because it's about to change...
            String actualChecksum = checksumObserver.getActualChecksum();

            File tempChecksumFile = new File( tempDestination + checksumFileExtension + ".tmp" );
            tempChecksumFile.deleteOnExit();
            wagon.get( remotePath + checksumFileExtension, tempChecksumFile );

            String expectedChecksum = FileUtils.fileRead( tempChecksumFile, "UTF-8" );

            // remove whitespaces at the end
            expectedChecksum = expectedChecksum.trim();

            // check for 'ALGO (name) = CHECKSUM' like used by openssl
            if ( expectedChecksum.regionMatches( true, 0, "MD", 0, 2 ) || expectedChecksum.regionMatches( true, 0, "SHA", 0, 3 ) )
            {
                int lastSpacePos = expectedChecksum.lastIndexOf( ' ' );
                expectedChecksum = expectedChecksum.substring( lastSpacePos + 1 );
            }
            else
            {
                // remove everything after the first space (if available)
                int spacePos = expectedChecksum.indexOf( ' ' );

                if ( spacePos != -1 )
                {
                    expectedChecksum = expectedChecksum.substring( 0, spacePos );
                }
            }
            if ( expectedChecksum.equalsIgnoreCase( actualChecksum ) )
            {
                File checksumFile = new File( destination + checksumFileExtension );
                if ( checksumFile.exists() )
                {
                    checksumFile.delete();
                }
                FileUtils.copyFile( tempChecksumFile, checksumFile );
                tempChecksumFile.delete();
            }
            else
            {
                throw new ChecksumFailedException( "Checksum failed on download: local = '" + actualChecksum + "'; remote = '" + expectedChecksum + "'" );
            }
        }
        catch ( IOException e )
        {
            throw new ChecksumFailedException( "Invalid checksum file", e );
        }
    }

    private void disconnectWagon( Wagon wagon )
    {
        try
        {
            wagon.disconnect();
        }
        catch ( ConnectionException e )
        {
            logger.error( "Problem disconnecting from wagon - ignoring: " + e.getMessage() );
        }
    }

    private void releaseWagon( String protocol, Wagon wagon )
    {
        try
        {
            container.release( wagon );
        }
        catch ( ComponentLifecycleException e )
        {
            logger.error( "Problem releasing wagon - ignoring: " + e.getMessage() );
            logger.debug( "", e );
        }
    }

    @Deprecated
    public Wagon getWagon( Repository repository )
        throws UnsupportedProtocolException
    {
        return getWagon( repository.getProtocol() );
    }

    @Deprecated
    public Wagon getWagon( String protocol )
        throws UnsupportedProtocolException
    {
        if ( protocol == null )
        {
            throw new UnsupportedProtocolException( "Unspecified protocol" );
        }

        String hint = protocol.toLowerCase( java.util.Locale.ENGLISH );
        Wagon wagon = (Wagon) wagons.get( hint );

        if ( wagon == null )
        {
            throw new UnsupportedProtocolException( "Cannot find wagon which supports the requested protocol: " + protocol );
        }

        return wagon;
    }

    // Deployer

    public void deploy( File source, Artifact artifact, ArtifactRepository deploymentRepository, ArtifactRepository localRepository )
        throws ArtifactDeploymentException
    {
        try
        {
            transformationManager.transformForDeployment( artifact, deploymentRepository, localRepository );

            // Copy the original file to the new one if it was transformed
            File artifactFile = new File( localRepository.getBasedir(), localRepository.pathOf( artifact ) );
            if ( !artifactFile.equals( source ) )
            {
                FileUtils.copyFile( source, artifactFile );
            }

            putArtifact( source, artifact, deploymentRepository, null );

            // must be after the artifact is installed
            for ( ArtifactMetadata metadata : artifact.getMetadataList() )
            {
                repositoryMetadataManager.deploy( metadata, localRepository, deploymentRepository );
            }
        }
        catch ( TransferFailedException e )
        {
            throw new ArtifactDeploymentException( "Error deploying artifact: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new ArtifactDeploymentException( "Error deploying artifact: " + e.getMessage(), e );
        }
        catch ( RepositoryMetadataDeploymentException e )
        {
            throw new ArtifactDeploymentException( "Error installing artifact's metadata: " + e.getMessage(), e );
        }
    }

    // Installer

    public void install( File source, Artifact artifact, ArtifactRepository localRepository )
        throws ArtifactInstallationException
    {
        try
        {
            transformationManager.transformForInstall( artifact, localRepository );

            String localPath = localRepository.pathOf( artifact );

            // TODO: use a file: wagon and the wagon manager?
            File destination = new File( localRepository.getBasedir(), localPath );
            if ( !destination.getParentFile().exists() )
            {
                destination.getParentFile().mkdirs();
            }

            logger.info( "Installing " + source.getPath() + " to " + destination );

            FileUtils.copyFile( source, destination );

            // must be after the artifact is installed
            for ( ArtifactMetadata metadata : artifact.getMetadataList() )
            {
                repositoryMetadataManager.install( metadata, localRepository );
            }
            // TODO: would like to flush this, but the plugin metadata is added in advance, not as an install/deploy
            // transformation
            // This would avoid the need to merge and clear out the state during deployment
            // artifact.getMetadataList().clear();
        }
        catch ( IOException e )
        {
            throw new ArtifactInstallationException( "Error installing artifact: " + e.getMessage(), e );
        }
        catch ( RepositoryMetadataInstallationException e )
        {
            throw new ArtifactInstallationException( "Error installing artifact's metadata: " + e.getMessage(), e );
        }
    }
}
