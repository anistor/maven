/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
 */

package org.apache.maven.repository.mercury;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ReactorArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.resolver.filter.IncludesArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.mercury.artifact.ArtifactMetadata;
import org.apache.maven.mercury.artifact.ArtifactScopeEnum;
import org.apache.maven.mercury.artifact.MetadataTreeNode;
import org.apache.maven.mercury.builder.api.DependencyProcessor;
import org.apache.maven.mercury.repository.api.Repository;
import org.apache.maven.mercury.repository.local.m2.LocalRepositoryM2;
import org.apache.maven.mercury.repository.local.map.LocalRepositoryMap;
import org.apache.maven.mercury.repository.local.map.ReactorStorage;
import org.apache.maven.mercury.repository.local.map.StorageException;
import org.apache.maven.mercury.repository.remote.m2.RemoteRepositoryM2;
import org.apache.maven.mercury.transport.api.Server;
import org.apache.maven.mercury.util.Util;
import org.apache.maven.repository.MavenArtifactMetadata;
import org.apache.maven.repository.MetadataGraph;
import org.apache.maven.repository.MetadataGraphNode;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */
class MercuryAdaptor
{

    private static Map<String, Repository> _repos = Collections.synchronizedMap( new HashMap<String, Repository>() );

    private static LocalRepositoryMap _reactorRepository;

    /**
     * @param repository
     * @param dependencyProcessor
     * @throws StorageException
     */
    private static void initializeReactor( ReactorArtifactRepository mavenReactorRepository,
                                          DependencyProcessor dependencyProcessor )
    {
        if ( mavenReactorRepository == null )
            return;

        try
        {
            _reactorRepository =
                new LocalRepositoryMap( "reactor", dependencyProcessor,
                                        new ReactorStorage( new File( mavenReactorRepository.getBasedir() ),
                                                            mavenReactorRepository.getStorage() ) );
        }
        catch ( StorageException e )
        {
            throw new IllegalArgumentException( e );
        }

    }

    public static void deleteReactor()
    {
        _reactorRepository = null;
    }

    public static List<Repository> toMercuryRepos( ReactorArtifactRepository reactorRepository,
                                                   ArtifactRepository localRepository, List<?> remoteRepositories,
                                                   DependencyProcessor dependencyProcessor )
    {
        if ( localRepository == null && Util.isEmpty( remoteRepositories ) )
            return null;

        if ( "legacy".equals( localRepository.getLayout().getId() ) )
            return null;

        if ( !Util.isEmpty( remoteRepositories ) )
            for ( Object ro : remoteRepositories )
            {
                if ( ArtifactRepository.class.isAssignableFrom( ro.getClass() ) )
                {
                    ArtifactRepository ar = (ArtifactRepository) ro;

                    if ( "legacy".equals( ar.getLayout().getId() ) )
                        return null;
                }
            }

        if ( _reactorRepository == null && reactorRepository.isInitialized() )
            initializeReactor( reactorRepository, dependencyProcessor );

        int nRepos =
            ( _reactorRepository == null ? 0 : 1 ) + ( localRepository == null ? 0 : 1 )
                + ( Util.isEmpty( remoteRepositories ) ? 0 : remoteRepositories.size() );

        Map<String, Repository> repos = new LinkedHashMap<String, Repository>( nRepos );

        if ( _reactorRepository != null )
            repos.put( _reactorRepository.getId(), _reactorRepository );

        if ( localRepository != null )
        {
            String url = localRepository.getUrl();

            LocalRepositoryM2 lr = (LocalRepositoryM2) _repos.get( url );

            if ( lr == null )
                try
                {
                    URI rootURI = new URI( url );

                    File localRepoDir = new File( rootURI );

                    lr = new LocalRepositoryM2( localRepository.getId(), localRepoDir, dependencyProcessor );

                    // lr.setSnapshotAlwaysWins( true );

                    _repos.put( url, lr );
                }
                catch ( URISyntaxException e )
                {
                    throw new IllegalArgumentException( e );
                }
            repos.put( url, lr );
        }

        if ( !Util.isEmpty( remoteRepositories ) )
        {
            for ( Object o : remoteRepositories )
            {
                String url;
                String id;

                if ( ArtifactRepository.class.isAssignableFrom( o.getClass() ) )
                {
                    ArtifactRepository ar = (ArtifactRepository) o;
                    url = ar.getUrl();
                    id = ar.getId();
                }
                else if ( org.apache.maven.model.Repository.class.isAssignableFrom( o.getClass() ) )
                {
                    org.apache.maven.model.Repository ar = (org.apache.maven.model.Repository) o;
                    url = ar.getUrl();
                    id = ar.getId();
                }
                else
                    throw new IllegalArgumentException( "found illegal class in the remote repository list - "
                        + o.getClass().getName() );

                RemoteRepositoryM2 rr = (RemoteRepositoryM2) _repos.get( url );

                if ( rr == null )
                {
                    Server server;
                    try
                    {
                        server = new Server( id, new URL( url ) );
                    }
                    catch ( MalformedURLException e )
                    {
                        throw new IllegalArgumentException( e );
                    }
                    rr = new RemoteRepositoryM2( server, dependencyProcessor );
                    _repos.put( url, rr );
                }

                repos.put( url, rr );
            }
        }

        List<Repository> res = new ArrayList<Repository>( repos.size() );

        for ( Entry<String, Repository> e : repos.entrySet() )
            res.add( e.getValue() );

        // System.out.println("Converted "+nRepos+" -> "+res.size());
        //
        return res;
    }

    private static void setInExClusion( ArtifactMetadata md, List<String> patterns, boolean inc )
    {
        if ( Util.isEmpty( patterns ) )
            return;

        List<ArtifactMetadata> lusions = new ArrayList<ArtifactMetadata>( patterns.size() );

        for ( String pattern : patterns )
            lusions.add( new ArtifactMetadata( pattern ) );

        if ( inc )
            md.setInclusions( lusions );
        else
            md.setExclusions( lusions );
    }

    public static ArtifactMetadata toMercuryMetadata( Artifact a )
    {
        ArtifactHandler h = a.getArtifactHandler();
        
        ArtifactMetadata md = new ArtifactMetadata();
        
        md.setGroupId( a.getGroupId() );
        md.setArtifactId( a.getArtifactId() );
        md.setVersion( a.getVersion() );
        md.setType( h == null ? a.getType() : h.getExtension() );
        md.setScope( a.getScope() );
        md.setOptional( a.isOptional() );
        md.setClassifier( h == null ? a.getClassifier() : h.getClassifier() );

        // if the handler allowed this ..
        if ( "test-jar".equals( a.getType() ) )
        {
            md.setType( "jar" );
            md.setClassifier( "tests" );
        }

        ArtifactFilter af = a.getDependencyFilter();

        if ( af != null )
        {
            if ( ExcludesArtifactFilter.class.isAssignableFrom( af.getClass() ) )
            {
                setInExClusion( md, ( (ExcludesArtifactFilter) af ).getPatterns(), false );
            }
            else if ( IncludesArtifactFilter.class.isAssignableFrom( af.getClass() ) )
            {
                setInExClusion( md, ( (IncludesArtifactFilter) af ).getPatterns(), true );
            }
        }
        return md;
    }

    public static Artifact toMavenArtifact( ArtifactFactory af, org.apache.maven.mercury.artifact.Artifact a )
    {
        boolean isTestJar = "test-jar".equals( a.getType() );

        String type = isTestJar ? "jar" : a.getType();

        String classifier = isTestJar ? "tests" : a.getClassifier();

        Artifact ma =
            classifier == null ? af.createArtifact( a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getScope(),
                                                    type ) : af.createArtifactWithClassifier( a.getGroupId(),
                                                                                              a.getArtifactId(),
                                                                                              a.getVersion(), type,
                                                                                              classifier );
        ma.setScope( a.getScope() );

        ma.setFile( a.getFile() );

        ma.setResolved( a.getFile() != null );

        ma.setResolvedVersion( a.getVersion() );

        return ma;
    }

    public static Artifact toMavenArtifact( ArtifactFactory af, Artifact a )
    {
        // MavenProject likes this one - replaces it with an actual test jar is available
        // bad idea - embedder tests don't like it
        // boolean isTestJar = "jar".equals( a.getType() ) && "tests".equals( a.getClassifier() );
        //        
        // String type = isTestJar ? "test-jar" : a.getType();

        Artifact ma =
            af.createArtifactWithClassifier( a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getType(),
                                             a.getClassifier() );
        ma.setScope( a.getScope() );

        ma.setFile( a.getFile() );

        ma.setResolved( a.getFile() != null );

        ma.setResolvedVersion( a.getVersion() );

        return ma;
    }

    public static Artifact toMavenArtifact( ArtifactFactory af, org.apache.maven.mercury.artifact.ArtifactMetadata a )
    {
        // MavenProject likes this one - replaces it with an actual test jar is available
        // bad idea - embedder tests don't like it
        // boolean isTestJar = "jar".equals( a.getType() ) && "tests".equals( a.getClassifier() );
        //        
        // String type = isTestJar ? "test-jar" : a.getType();

        Artifact ma =
            af.createArtifactWithClassifier( a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getType(),
                                             a.getClassifier() );
        ma.setScope( a.getScope() );

        return ma;
    }

    /**
     * @param factory
     * @param d
     * @return
     */
//    public static Artifact toMavenArtifact( ArtifactFactory af, Dependency a )
//    {
//        boolean isTestJar = "test-jar".equals( a.getType() );
//
//        String type = isTestJar ? "jar" : a.getType();
//
//        String classifier = isTestJar ? "tests" : a.getClassifier();
//
//        Artifact ma =
//            af.createArtifactWithClassifier( a.getGroupId(), a.getArtifactId(), a.getVersion(), type, classifier );
//        ma.setScope( a.getScope() );
//
//        return ma;
//    }

    public static ArtifactMetadata toMercuryArtifactMetadata( MavenArtifactMetadata md )
    {
        ArtifactMetadata mmd = new ArtifactMetadata();
        mmd.setGroupId( md.getGroupId() );
        mmd.setArtifactId( md.getArtifactId() );
        mmd.setVersion( md.getVersion() );
        mmd.setClassifier( md.getClassifier() );
        mmd.setType( md.getType() );

        if ( "test-jar".equals( md.getType() ) )
        {
            mmd.setType( "jar" );
            mmd.setClassifier( "tests" );
        }

        return mmd;
    }

    public static MavenArtifactMetadata toMavenArtifactMetadata( ArtifactMetadata md )
    {
        MavenArtifactMetadata mmd = new MavenArtifactMetadata();
        mmd.setGroupId( md.getGroupId() );
        mmd.setArtifactId( md.getArtifactId() );
        mmd.setVersion( md.getVersion() );
        mmd.setClassifier( md.getClassifier() );
        mmd.setType( md.getType() );

        if ( "test-jar".equals( md.getType() ) )
        {
            mmd.setType( "jar" );
            mmd.setClassifier( "tests" );
        }

        return mmd;
    }

    public static MavenArtifactMetadata toMavenArtifactMetadata( Artifact md )
    {
        MavenArtifactMetadata mmd = new MavenArtifactMetadata();
        mmd.setGroupId( md.getGroupId() );
        mmd.setArtifactId( md.getArtifactId() );
        mmd.setVersion( md.getVersion() );
        mmd.setClassifier( md.getClassifier() );
        mmd.setType( md.getType() );

        if ( "test-jar".equals( md.getType() ) )
        {
            mmd.setType( "jar" );
            mmd.setClassifier( "tests" );
        }

        return mmd;
    }

    public static MetadataGraph resolvedTreeToGraph( MetadataTreeNode root )
    {
        if ( root == null )
            return null;

        MetadataGraphNode entry = new MetadataGraphNode( toMavenArtifactMetadata( root.getMd() ) );

        MetadataGraph graph = new MetadataGraph( entry );

        graph.addNode( entry );

        addKids( root, entry, graph );

        return graph;
    }

    private static final void addKids( MetadataTreeNode tParent, MetadataGraphNode gParent, MetadataGraph graph )
    {
        if ( !tParent.hasChildren() )
            return;

        for ( MetadataTreeNode kid : tParent.getChildren() )
        {
            MavenArtifactMetadata mmd = toMavenArtifactMetadata( kid.getMd() );

            MetadataGraphNode node = graph.findNode( mmd );

            node.addIncident( gParent );

            gParent.addIncident( node );

            addKids( kid, node, graph );
        }
    }

    /**
     * @param reqArtifact
     * @param filter
     * @return
     */
    public static ArtifactScopeEnum extractScope( Artifact reqArtifact, ArtifactFilter filter )
    {
        String scopeStr =
            reqArtifact.getScope() == null ?  org.apache.maven.mercury.artifact.Artifact.SCOPE_COMPILE
                            : reqArtifact.getScope();
        
        System.out.println("Scope 1: "+scopeStr);
        
        if ( filter != null )
        {
            if ( ScopeArtifactFilter.class.isAssignableFrom( filter.getClass() ) )
            {
                scopeStr = ( (ScopeArtifactFilter) filter ).getScope();

System.out.println("Scope 2: "+scopeStr);
                
                if( scopeStr == null )
                    scopeStr = org.apache.maven.mercury.artifact.Artifact.SCOPE_COMPILE;

System.out.println("Scope 3: "+scopeStr);
            }
        }

        if ( scopeStr != null )
        {
            if ( org.apache.maven.mercury.artifact.Artifact.SCOPE_COMPILE.equals( scopeStr ) )
                return ArtifactScopeEnum.compile;
            else if ( org.apache.maven.mercury.artifact.Artifact.SCOPE_TEST.equals( scopeStr ) )
                return ArtifactScopeEnum.test;
            else if ( org.apache.maven.mercury.artifact.Artifact.SCOPE_PROVIDED.equals( scopeStr ) )
                return ArtifactScopeEnum.provided;
            else if ( org.apache.maven.mercury.artifact.Artifact.SCOPE_RUNTIME.equals( scopeStr ) )
                return ArtifactScopeEnum.runtime;
            else if ( Artifact.SCOPE_RUNTIME_PLUS_SYSTEM.equals( scopeStr ) )
                return ArtifactScopeEnum.runtime;
            else if ( org.apache.maven.mercury.artifact.Artifact.SCOPE_SYSTEM.equals( scopeStr ) )
                return ArtifactScopeEnum.system;
        }

System.out.println("Scope 4: null");

        return null;
    }

    static Map<String, ArtifactMetadata> _hackMap = new HashMap<String, ArtifactMetadata>();

    static int _hackMapSize = 0;
    static
    {
        ArtifactMetadata md;

        md = new ArtifactMetadata( "cglib-nodep:cglib-nodep:2.1_3" );
        md.setOptional( true );
        _hackMap.put( md.toManagementString(), md );

        md = new ArtifactMetadata( "javax:j2ee:1.4" );
        md.setOptional( true );
        _hackMap.put( md.toManagementString(), md );

        _hackMapSize = _hackMap.size();
    }

    public static Map<String, ArtifactMetadata> toMercuryVersionMap( ArtifactResolutionRequest request )
    {
        Map<String, Artifact> vmap = (Map<String, Artifact>) request.getManagedVersionMap();

        if ( Util.isEmpty( vmap ) )
            return null;

        Map<String, ArtifactMetadata> res = new HashMap<String, ArtifactMetadata>( vmap.size() + _hackMapSize );

        // res.putAll( _hackMap );

        for ( Entry<String, Artifact> e : vmap.entrySet() )
        {
            ArtifactMetadata md = toMercuryMetadata( e.getValue() );

            res.put( e.getKey(), md );
        }

        return res;
    }

}
