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

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ReactorArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.mercury.artifact.ArtifactExclusionList;
import org.apache.maven.mercury.artifact.ArtifactMetadata;
import org.apache.maven.mercury.artifact.ArtifactQueryList;
import org.apache.maven.mercury.artifact.ArtifactScopeEnum;
import org.apache.maven.mercury.artifact.MetadataTreeNode;
import org.apache.maven.mercury.builder.api.DependencyProcessor;
import org.apache.maven.mercury.plexus.PlexusMercury;
import org.apache.maven.mercury.repository.api.Repository;
import org.apache.maven.mercury.repository.api.RepositoryException;
import org.apache.maven.mercury.util.Util;
import org.apache.maven.repository.LegacyRepositorySystem;
import org.apache.maven.repository.MetadataGraph;
import org.apache.maven.repository.MetadataResolutionRequest;
import org.apache.maven.repository.MetadataResolutionResult;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.lang.DefaultLanguage;
import org.codehaus.plexus.lang.Language;
import org.codehaus.plexus.logging.Logger;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */
// PLEASE NOTE: don't change the following string (spaces matter) as it is used by repo system flip flag
@Component( role = RepositorySystem.class, hint = "default" )
public class MercuryRepositorySystem
    extends LegacyRepositorySystem
    implements RepositorySystem
{
    private static final Language LANG = new DefaultLanguage( MercuryRepositorySystem.class );
    
    private Map<String, Set<Artifact> > _resolutions = Collections.synchronizedMap( new HashMap<String, Set<Artifact>>(128) );
    
    @Requirement( hint = "maven" )
    DependencyProcessor _dependencyProcessor;

    @Requirement
    PlexusMercury _mercury;

    @Requirement
    ArtifactFactory _artifactFactory;

    @Requirement(role=ArtifactRepository.class,hint="reactor")
    private ReactorArtifactRepository _reactorRepository;

    @Requirement
    private Logger _logger;
    
    private boolean _reactorInitialized = false;
    
    @Override
    public ArtifactResolutionResult resolve( ArtifactResolutionRequest request )
    {
        if ( request == null )
            throw new IllegalArgumentException( LANG.getMessage( "null.request" ) );

        if ( request.getArtifact() == null )
            throw new IllegalArgumentException( LANG.getMessage( "null.request.artifact" ) );
        
        ArtifactResolutionResult result = new ArtifactResolutionResult();
        
        String requestKey = null;
        
        if( !_reactorInitialized )
        {
            MercuryAdaptor.initializeReactor( _reactorRepository, _dependencyProcessor );
            
            _reactorInitialized = true;
        }

        List<Repository> repos =
            MercuryAdaptor.toMercuryRepos(   request.getLocalRepository()
                                           , request.getRemoteRepostories()
                                           , _dependencyProcessor
                                         );
        if( repos == null )
            return super.resolve( request );
        
        ArtifactFilter filter = request.getFilter();

        try
        {
            
long start = System.currentTimeMillis();

            org.apache.maven.artifact.Artifact rootArtifact = request.getArtifact();
            
            org.apache.maven.artifact.Artifact mavenPluginArtifact = rootArtifact;
            
            Set<Artifact> artifacts = request.getArtifactDependencies();
            
            boolean isPlugin = "maven-plugin".equals( rootArtifact.getType() ); 
            
            ArtifactScopeEnum scope = MercuryAdaptor.extractScope( rootArtifact, isPlugin, request.getFilter() );
            
if( _logger.isDebugEnabled() )
{
_logger.debug( "\n\n======> mercury: request for "+request.getArtifact()
   +", scope="+scope
   +", deps="+ (artifacts == null ? 0 : artifacts.size() )
   +", resolveRoot="+request.isResolveRoot()
//+", repos="+request.getRemoteRepostories().size()
//+", map=" + request.getManagedVersionMap() 
);
    if( artifacts != null )
    {
        showList( artifacts, "   --------> " );
    //if( request.getManagedVersionMap() != null && request.getManagedVersionMap().size() > 0 )
    //    logger.debug( "   ########>  VersionMap\n"+request.getManagedVersionMap()+"\n" );
    } 
}

            Map<String, ArtifactMetadata> versionMap = MercuryAdaptor.toMercuryVersionMap( request );
            

            if( isPlugin  )
            {
                // check resolution cache first - plugins are cached here
                requestKey = calcKey( request );
                
                if( requestKey != null )
                {
                    Set<Artifact> al = _resolutions.get( requestKey );
                    
                    if( al != null )
                    {
                        for( Artifact a : al )
                        {
                            if( isGood( filter, a ) )
                            {
                                result.addArtifact( a );
                                result.addRequestedArtifact( a );
                            }
                        }
                        
                        return result;
                    }
                }
                
                // not cached - let's proceed
                rootArtifact = createArtifact( rootArtifact.getGroupId()
                                                    , rootArtifact.getArtifactId()
                                                    , rootArtifact.getVersion()
                                                    , rootArtifact.getScope()
                                                    , "jar"
                                                  );
            }

            ArtifactMetadata rootMd = MercuryAdaptor.toMercuryMetadata( rootArtifact );

            // cannot just replace the type. Besides - Maven expects the same object out
            org.apache.maven.artifact.Artifact root = isPlugin ? mavenPluginArtifact : rootArtifact;

            // firstly - deal with the root
            // copied from artifact resolver 
            if ( request.isResolveRoot() && rootArtifact.getFile() == null && Util.isEmpty( artifacts ) )
            {
                try
                {
                    List<ArtifactMetadata> mercuryMetadataList = new ArrayList<ArtifactMetadata>(1);
                    
                    mercuryMetadataList.add( rootMd );
                    
                    List<org.apache.maven.mercury.artifact.Artifact> mercuryArtifactList =
                        _mercury.read( repos, mercuryMetadataList );
                    
                    if( Util.isEmpty( mercuryArtifactList ) )
                    {
                        result.addErrorArtifactException( new ArtifactResolutionException( "scope="+scope, rootArtifact) );
                        return result;
                    }

                    org.apache.maven.mercury.artifact.Artifact a = mercuryArtifactList.get( 0 );
                    
                    root.setFile( a.getFile() );
                    root.setResolved( true );
                    root.setResolvedVersion( a.getVersion() );
                    
                    result.addArtifact( root );
                    result.addRequestedArtifact( root );
                }
                catch ( Exception e )
                {
                    result.addMissingArtifact( request.getArtifact() );
                    return result;
                }
            }

            // no dependencies - bye
            if ( Util.isEmpty( artifacts ) )
            {
if( _logger.isDebugEnabled() )
    _logger.debug("mercury: resolved("+(System.currentTimeMillis() - start)+") "+root+", scope="+scope
+", artifacs="+(result.getArtifacts() == null ? 0 :result.getArtifacts().size())
+", file "+root.getFile()
+"\n<===========\n" );
                return result;
            } 

            // resolved metadata in Mercury format
            List<ArtifactMetadata> mercuryMetadataList = null;
            
            // no sense to resolve reactor artifacts - just pass them back as is
            // their dependencies should already be on the list anyway
            List<Artifact> resolvedList = new ArrayList<Artifact>( artifacts.size() + 1 );
            
            // exclude resolved from resolution
            List<ArtifactMetadata> globalExclusions = new ArrayList<ArtifactMetadata>( artifacts.size() + 1 );
            
            // query for mercury
            List<ArtifactMetadata> query = new ArrayList<ArtifactMetadata>( artifacts.size() + 1 );

            
            if( request.isResolveRoot() && rootArtifact.getFile() == null  )
            {
                if( root.isResolved() )
                {
                    resolvedList.add( root );
                    globalExclusions.add( rootMd );
                } 
                else
                    query.add( rootMd );
            }
            
            // TODO - are activeProjectDependencies here rightly ??
            // add dependencies of actives to the original request list
            List<Artifact> activeDependencies = new ArrayList<Artifact>();
            
            for( Artifact a : artifacts )
            {
//                if( a.isResolved() && DependencyHolder.class.isAssignableFrom( a.getClass() ) )
//                {
//                    DependencyHolder apa = (DependencyHolder) a;
//                    List<Dependency> deps = apa.getDependencyList();
//                    if( deps != null )
//                        for( Dependency d : deps )
//                            activeDependencies.add( MercuryAdaptor.toMavenArtifact( _artifactFactory, d ) );
//                }
            }
            
            //
            // daddy Jason does not do that, 
            // but they are very important - cannot build correct classpath
            // without them. The correct way to add them is:
            //                    - grab the active's POM
            //                    - create a local map repository for each POM
            //                    - add a POM to the metadata list (be re resolved from map repo)
            //                    - remove POMs from final result
            //                    - add original active's instead
            //
            // TODO replace this HACK 
            //due to the lack of time - hacking up a solution: add all them to the dependency list,
            //  but this approach looses the depth and causes problems:
            //         if active depends on junit 4.4 and root depends on 3.8.1 - 4.4 wins, while it should not
            //  trying to mitigate it by adding only non-existent GAs
          if( activeDependencies.size() > 0 )
          {
              for( Artifact a : activeDependencies )
                  if( gaDoesNotExist(a,artifacts) )
                  {
                      if( isGood( filter, a ) )
                      {
                          artifacts.add( a );
    
                          if( _logger.isDebugEnabled() )
                              _logger.debug( "        ------ active's dep --> "+a );
                      }
                  }
              
          }
            
          
          // decide - what goes to query vs. already resolved
            for( Artifact a : artifacts )
            {
                if( a.isResolved() )
                {
                    resolvedList.add( a );
                    // don't resolve again
                    globalExclusions.add( MercuryAdaptor.toMercuryMetadata( a ) );
                }
                else
                    query.add( MercuryAdaptor.toMercuryMetadata( a ) );
            }

            if( query.size() > 0 ) // metadata resolution first
                mercuryMetadataList = _mercury.resolve( repos, scope, new ArtifactQueryList(query)
                                                      , null, new ArtifactExclusionList(globalExclusions), versionMap
                                                      );
if( _logger.isDebugEnabled() )
{
    showMdList( mercuryMetadataList, "     <.. md by mercury ...... " );
}
            
            // no metadata resolved and nothing pre-resolved
            if( Util.isEmpty( mercuryMetadataList ) && Util.isEmpty( resolvedList ) ) 
            {
                result.addMissingArtifact( rootArtifact );
                
                long diff = System.currentTimeMillis() - start;
                
if( _logger.isDebugEnabled() )
    _logger.debug("mercury: missing artifact("+diff+") "+rootArtifact+"("+scope+")"+"\n<===========\n" );

                return result;
            }
            
            // read binaries
            List<org.apache.maven.mercury.artifact.Artifact> mercuryArtifactList =
                _mercury.read( repos, mercuryMetadataList );
            
            if( _logger.isDebugEnabled() )
            {
                showAList( mercuryArtifactList, "     <__ bin. by mercury ____ " );
            }
            
            // ActiveProjectArtifacts - add the to the result before the rest
            for( Artifact a : resolvedList )
            {
                result.addArtifact( a );
                result.addRequestedArtifact( a );
                if( _logger.isDebugEnabled() )
                {
                    _logger.debug( "     <-- pre-resolved -- "+a );
                }
            }
            
            if ( !Util.isEmpty( mercuryArtifactList ) )
            {
                for ( org.apache.maven.mercury.artifact.Artifact a : mercuryArtifactList )
                {
                    if( a.getGroupId().equals( rootMd.getGroupId() ) && a.getArtifactId().equals( rootMd.getArtifactId() ) )
                    { // root artifact processing - if resolved with everybody
                        if( !request.isResolveRoot() )
                            continue;

                        root = isPlugin ? mavenPluginArtifact : rootArtifact;

                        root.setFile( a.getFile() );
                        root.setResolved( true );
                        root.setResolvedVersion( a.getVersion() );

                        result.addArtifact( root );
                        result.addRequestedArtifact( root );
                    }
                    else // regular resolved artifact
                    {
                        Artifact ma = MercuryAdaptor.toMavenArtifact( _artifactFactory, a );
                        
                        if( isGood( filter, ma ) )
                        {
                            result.addArtifact( ma );
                            result.addRequestedArtifact( ma );
                        }
                    }
                }

                long diff = System.currentTimeMillis() - start;

                Set<Artifact> resSet = result.getArtifacts();

if( _logger.isDebugEnabled() )
{
    _logger.debug("mercury: resolved("+diff+") "+root+", scope="+scope
+", artifacs="+( resSet == null ? 0 : resSet.size() )
+", file "+root.getFile() 
               );
    showList( resSet, "       <--------- " );
    _logger.debug("\n<==========================================\n");
}

            if( requestKey != null )
                _resolutions.put( requestKey, resSet );

            }
            else
            {
                result.addMissingArtifact( rootArtifact );
                
                long diff = System.currentTimeMillis() - start;
if( _logger.isDebugEnabled() )
    _logger.debug("mercury: missing artifact("+diff+") "+rootArtifact+"("+scope+")"+"\n<===========\n" );
            }
            
        }
        catch ( RepositoryException e )
        {
            result.addErrorArtifactException( new ArtifactResolutionException( e.getMessage(), request.getArtifact(),
                                                                               request.getRemoteRepostories() ) );
        }

        return result;
    }
    

    /**
     * @param a
     * @param artifacts
     * @return
     */
    private boolean gaDoesNotExist( Artifact me, Set<Artifact> artifacts )
    {
        String myType = me.getType() == null ? "jar" : me.getType();
        
        for( Artifact a : artifacts )
        {
            String aType = a.getType() == null ? "jar" : a.getType();
            
            if( a.getGroupId().equals( me.getGroupId() )
                && a.getArtifactId().equals( me.getArtifactId() )
                && myType.equals( aType )
              )
            {
                if( me.hasClassifier() )
                {
                    if( a.hasClassifier() && a.getClassifier().equals( me.getClassifier() ) )
                        return false;
                }
                else
                    return false;
            }
        }
        
        return true;
    }


    /**
     * @param mercuryArtifactList
     * @param string
     */
    private void showAList( List<org.apache.maven.mercury.artifact.Artifact> artifacts, String prefix )
    {
        if( Util.isEmpty( artifacts ) )
            return;
        
        TreeSet<String> ts = new TreeSet<String>();
        
        for( org.apache.maven.mercury.artifact.Artifact a : artifacts )
            ts.add( ""+a );
        
        for( String a : ts )
            _logger.debug( prefix + a );
    }


    /**
     * @param mercuryMetadataList
     * @param string
     */
    private void showMdList( List<ArtifactMetadata> artifacts, String prefix )
    {
        if( Util.isEmpty( artifacts ) )
            return;
        
        TreeSet<String> ts = new TreeSet<String>();
        
        for( ArtifactMetadata a : artifacts )
            ts.add( ""+a );
        
        for( String a : ts )
            _logger.debug( prefix + a );
    }


    private void showList( Set<Artifact> artifacts, String prefix )
    {
        if( Util.isEmpty( artifacts ) )
            return;
        
        TreeSet<String> ts = new TreeSet<String>();
        
        for( Artifact a : artifacts )
            ts.add( a + (a.isOptional() ? ", [optional]":"")+(a.isResolved()?", [resolved]":"")+ ( a.getFile() == null ? "" :", file="+a.getFile()) );
        
        for( String a : ts )
            _logger.debug( prefix + a );
    }

    /**
     * @param request
     * @return
     */
    private static String calcKey( ArtifactResolutionRequest request )
    {
        TreeSet<String> ts = new TreeSet<String>();
        
        ts.add( request.getArtifact().toString() );
        
        Set<Artifact> artifacts = request.getArtifactDependencies();

        if( artifacts != null )
            for( Artifact a : artifacts )
                ts.add( ""+a );
        
        try
        {
            MessageDigest md = MessageDigest.getInstance( "SHA-1" );
            
            for( String s : ts )
                md.update( s.getBytes() );
            
            byte [] digest = md.digest();
            
            StringBuilder sb = new StringBuilder( 64 );
            
            for( byte b : digest )
                sb.append( "."+b );
            
            return sb.toString();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        
        return null;
    }



    public MetadataResolutionResult resolveMetadata( MetadataResolutionRequest request )
    {
        if ( request == null )
            throw new IllegalArgumentException( LANG.getMessage( "null.request" ) );

        if ( request.getArtifactMetadata() == null )
            throw new IllegalArgumentException( LANG.getMessage( "null.request.artifact" ) );

        List<Repository> repos =
            MercuryAdaptor.toMercuryRepos( request.getLocalRepository()
                                           , request.getRemoteRepostories()
                                           , _dependencyProcessor
                                         );

        MetadataResolutionResult res = new MetadataResolutionResult();
        
        ArtifactMetadata md = MercuryAdaptor.toMercuryArtifactMetadata( request.getArtifactMetadata() );
        
        try
        {
            MetadataTreeNode root = _mercury.resolveAsTree( repos, ArtifactScopeEnum.valueOf( request.getScope() ), new ArtifactQueryList(md), null, null );
            if( root != null )
            {
                MetadataGraph resTree = MercuryAdaptor.resolvedTreeToGraph( root );
                
                res.setResolvedTree( resTree );
            }
        }
        catch ( RepositoryException e )
        {
            res.addError( e );
        }
        
        return res;
    }
    
    private static boolean isGood( ArtifactFilter filter, Artifact a )
    {
        if( filter != null )
            return filter.include( a );
        
        return true;
    }

}
