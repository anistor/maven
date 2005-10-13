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
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Base class for artifact resolution exceptions.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class AbstractArtifactResolutionException
    extends Exception
{
    private String groupId;

    private String artifactId;

    private String version;

    private String type;

    private List remoteRepositories;

    private final String originalMessage;

    private final String path;

    static final String LS = System.getProperty( "line.separator" );

    protected AbstractArtifactResolutionException( String message, String groupId, String artifactId, String version,
                                                   String type, List remoteRepositories, List path )
    {
        super( constructMessageBase( message, groupId, artifactId, version, type, remoteRepositories, path ) );

        this.originalMessage = message;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.type = type;
        this.version = version;
        this.remoteRepositories = remoteRepositories;
        this.path = constructArtifactPath( path );
    }

    protected AbstractArtifactResolutionException( String message, String groupId, String artifactId, String version,
                                                   String type, List remoteRepositories, List path, Throwable t )
    {
        super( constructMessageBase( message, groupId, artifactId, version, type, remoteRepositories, path ), t );

        this.originalMessage = message;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.type = type;
        this.version = version;
        this.remoteRepositories = remoteRepositories;
        this.path = constructArtifactPath( path );
    }

    protected AbstractArtifactResolutionException( String message, Artifact artifact )
    {
        this( message, artifact, null );
    }

    protected AbstractArtifactResolutionException( String message, Artifact artifact, List remoteRepositories )
    {
        this( message, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getType(),
              remoteRepositories, artifact.getDependencyTrail() );
    }

    protected AbstractArtifactResolutionException( String message, Artifact artifact, List remoteRepositories,
                                                   Throwable t )
    {
        this( message, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getType(),
              remoteRepositories, artifact.getDependencyTrail(), t );
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public String getType()
    {
        return type;
    }

    public List getRemoteRepositories()
    {
        return remoteRepositories;
    }

    public String getOriginalMessage()
    {
        return originalMessage;
    }

    protected static String constructArtifactPath( List path )
    {
        StringBuffer sb = new StringBuffer();

        if ( path != null )
        {
            sb.append( LS );
            sb.append( "Path to dependency: " );
            sb.append( LS );
            int num = 1;
            for ( Iterator i = path.iterator(); i.hasNext(); num++ )
            {
                sb.append( "\t" );
                sb.append( num );
                sb.append( ") " );
                sb.append( i.next() );
                sb.append( LS );
            }
        }

        return sb.toString();
    }

    private static String constructMessageBase( String message, String groupId, String artifactId, String version,
                                                String type, List remoteRepositories, List path )
    {
        StringBuffer sb = new StringBuffer();

        sb.append( message );
        sb.append( LS );
        sb.append( "  " + groupId + ":" + artifactId + ":" + version + ":" + type );
        sb.append( LS );
        if ( remoteRepositories != null && !remoteRepositories.isEmpty() )
        {
            sb.append( LS );
            sb.append( "from the specified remote repositories:" );
            sb.append( LS + "  " );

            for ( Iterator i = new HashSet( remoteRepositories ).iterator(); i.hasNext(); )
            {
                ArtifactRepository remoteRepository = (ArtifactRepository) i.next();

                sb.append( remoteRepository.getId() );
                sb.append( " (" );
                sb.append( remoteRepository.getUrl() );
                sb.append( ")" );
                if ( i.hasNext() )
                {
                    sb.append( ",\n  " );
                }
            }
        }

        sb.append( constructArtifactPath( path ) );
        sb.append( LS );
        return sb.toString();
    }

    public String getArtifactPath()
    {
        return path;
    }
}
