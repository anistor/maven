package org.apache.maven.artifact;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.repository.legacy.factory.ArtifactFactory;
import org.apache.maven.repository.legacy.repository.ArtifactRepository;
import org.apache.maven.repository.legacy.repository.ArtifactRepositoryFactory;
import org.apache.maven.repository.legacy.repository.ArtifactRepositoryPolicy;
import org.apache.maven.repository.legacy.repository.DefaultArtifactRepository;
import org.apache.maven.repository.legacy.repository.layout.ArtifactRepositoryLayout;
import org.codehaus.plexus.PlexusTestCase;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id$
 */
public abstract class AbstractArtifactComponentTestCase
    extends PlexusTestCase
{
    protected ArtifactFactory artifactFactory;

    protected ArtifactRepositoryFactory artifactRepositoryFactory;
    
    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        artifactFactory = lookup( ArtifactFactory.class);        
        artifactRepositoryFactory = lookup( ArtifactRepositoryFactory.class );
    }
    
    @Override
    protected void tearDown()
        throws Exception
    {
        release( artifactFactory );
        
        super.tearDown();
    }

    protected abstract String component();

    /**
     * Return an existing file, not a directory - causes creation to fail.
     * 
     * @throws Exception
     */
    protected ArtifactRepository badLocalRepository()
        throws Exception
    {
        String path = "target/test-repositories/" + component() + "/bad-local-repository";

        File f = new File( getBasedir(), path );

        f.createNewFile();

        ArtifactRepositoryLayout repoLayout =
            (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "default" );

        return artifactRepositoryFactory.createArtifactRepository( "test", "file://" + f.getPath(), repoLayout, null, null );
    }

    protected String getRepositoryLayout()
    {
        return "default";
    }

    protected ArtifactRepository localRepository()
        throws Exception
    {
        String path = "target/test-repositories/" + component() + "/local-repository";

        File f = new File( getBasedir(), path );

        ArtifactRepositoryLayout repoLayout =
            (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "default" );

        return artifactRepositoryFactory.createArtifactRepository( "local", "file://" + f.getPath(), repoLayout, null, null );
    }

    protected ArtifactRepository remoteRepository()
        throws Exception
    {
        String path = "target/test-repositories/" + component() + "/remote-repository";

        File f = new File( getBasedir(), path );

        ArtifactRepositoryLayout repoLayout =
            (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "default" );

        return artifactRepositoryFactory.createArtifactRepository( "test", "file://" + f.getPath(), repoLayout,
                                              new ArtifactRepositoryPolicy(), new ArtifactRepositoryPolicy() );
    }

    protected ArtifactRepository badRemoteRepository()
        throws Exception
    {
        ArtifactRepositoryLayout repoLayout =
            (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "default" );

        return artifactRepositoryFactory.createArtifactRepository( "test", "http://foo.bar/repository", repoLayout, null, null );
    }

    protected void assertRemoteArtifactPresent( Artifact artifact )
        throws Exception
    {
        ArtifactRepository remoteRepo = remoteRepository();

        String path = remoteRepo.pathOf( artifact );

        File file = new File( remoteRepo.getBasedir(), path );

        if ( !file.exists() )
        {
            fail( "Remote artifact " + file + " should be present." );
        }
    }

    protected void assertLocalArtifactPresent( Artifact artifact )
        throws Exception
    {
        ArtifactRepository localRepo = localRepository();

        String path = localRepo.pathOf( artifact );

        File file = new File( localRepo.getBasedir(), path );

        if ( !file.exists() )
        {
            fail( "Local artifact " + file + " should be present." );
        }
    }

    protected void assertRemoteArtifactNotPresent( Artifact artifact )
        throws Exception
    {
        ArtifactRepository remoteRepo = remoteRepository();

        String path = remoteRepo.pathOf( artifact );

        File file = new File( remoteRepo.getBasedir(), path );

        if ( file.exists() )
        {
            fail( "Remote artifact " + file + " should not be present." );
        }
    }

    protected void assertLocalArtifactNotPresent( Artifact artifact )
        throws Exception
    {
        ArtifactRepository localRepo = localRepository();

        String path = localRepo.pathOf( artifact );

        File file = new File( localRepo.getBasedir(), path );

        if ( file.exists() )
        {
            fail( "Local artifact " + file + " should not be present." );
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    protected List<ArtifactRepository> remoteRepositories()
        throws Exception
    {
        List<ArtifactRepository> remoteRepositories = new ArrayList<ArtifactRepository>();

        remoteRepositories.add( remoteRepository() );

        return remoteRepositories;
    }

    // ----------------------------------------------------------------------
    // Test artifact generation for unit tests
    // ----------------------------------------------------------------------

    protected Artifact createLocalArtifact( String artifactId, String version )
        throws Exception
    {
        Artifact artifact = createArtifact( artifactId, version );

        createArtifact( artifact, localRepository() );

        return artifact;
    }

    protected Artifact createRemoteArtifact( String artifactId, String version )
        throws Exception
    {
        Artifact artifact = createArtifact( artifactId, version );

        createArtifact( artifact, remoteRepository() );

        return artifact;
    }

    protected void createLocalArtifact( Artifact artifact )
        throws Exception
    {
        createArtifact( artifact, localRepository() );
    }

    protected void createRemoteArtifact( Artifact artifact )
        throws Exception
    {
        createArtifact( artifact, remoteRepository() );
    }

    protected void createArtifact( Artifact artifact, ArtifactRepository repository )
        throws Exception
    {
        String path = repository.pathOf( artifact );

        File artifactFile = new File( repository.getBasedir(), path );

        if ( !artifactFile.getParentFile().exists() )
        {
            artifactFile.getParentFile().mkdirs();
        }

        Writer writer = new OutputStreamWriter( new FileOutputStream( artifactFile ), "ISO-8859-1" );

        writer.write( artifact.getId() );

        writer.close();
    }

    protected Artifact createArtifact( String artifactId, String version )
        throws Exception
    {
        return createArtifact( artifactId, version, "jar" );
    }

    protected Artifact createArtifact( String artifactId, String version, String type )
        throws Exception
    {
        return createArtifact( "org.apache.maven", artifactId, version, type );
    }

    protected Artifact createArtifact( String groupId, String artifactId, String version, String type )
        throws Exception
    {
        Artifact a = artifactFactory.createBuildArtifact( groupId, artifactId, version, type );
                
        return a;
    }

    protected void deleteLocalArtifact( Artifact artifact )
        throws Exception
    {
        deleteArtifact( artifact, localRepository() );
    }

    protected void deleteArtifact( Artifact artifact, ArtifactRepository repository )
        throws Exception
    {
        String path = repository.pathOf( artifact );

        File artifactFile = new File( repository.getBasedir(), path );

        if ( artifactFile.exists() )
        {
            if ( !artifactFile.delete() )
            {
                throw new IOException( "Failure while attempting to delete artifact " + artifactFile );
            }
        }
    }
}
