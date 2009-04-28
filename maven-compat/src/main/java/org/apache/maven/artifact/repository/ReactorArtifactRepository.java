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

package org.apache.maven.artifact.repository;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 *
 *
 * @author Oleg Gusakov
 * @version $Id$
 *
 */

@Component( role=ArtifactRepository.class,hint="reactor")
public class ReactorArtifactRepository
//extends Repository
implements ArtifactRepository
{
    private static final ArtifactRepositoryPolicy SNAPSHOTS_POLICY = new ArtifactRepositoryPolicy(true, ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER, ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE); 

    private static final ArtifactRepositoryPolicy RELEASES_POLICY = new ArtifactRepositoryPolicy(true, ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER, ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE);
    
    private static final String ID = "reactorRepository";
    
    private static final String PROTOCOL = "file";
    
    private String url;
    
    private String baseDir;

    @Requirement(role=ArtifactRepositoryLayout.class, hint="default")
    private static ArtifactRepositoryLayout layout;
    
    private Map<String, String> storage = new HashMap<String, String>(32);
    
    public static String calculateKey( Artifact artifact )
    {
        // GAV only
        return artifact.getGroupId()+":"+artifact.getArtifactId()+":"+artifact.getVersion();
    }
    
    private static String calculateBinaryName( Artifact artifact )
    {
        String type = artifact.getType();
        
        String classifier = artifact.getClassifier();
        
        if( "test-jar".equals( type ) )
        {
            type = "jar";
            classifier = "tests";
        }
        else if( "maven-plugin".equals( type ) )
        {
            type = "jar";
            classifier = null;
        }
        
        boolean hasClassifier = classifier != null && classifier.length() > 0;
        
        return artifact.getArtifactId()+"-"+artifact.getVersion()
               + (hasClassifier ? "-"+classifier : "" )
               + "." + type
       ;
    }

    /**
     * initialize this repository with the top level project. No need to add it as an Artifact because
     * it will be added from a sorted artifact list
     * 
     * @param artifact
     * @param baseDir
     * @param outputPath
     */
    public void setReactorRoot( Artifact artifact, File baseDir, String outputPath )
    {
        try
        {
            this.baseDir = baseDir.getCanonicalPath();

            this.url = baseDir.toURL().toString();
        }
        catch ( IOException e )
        {
            throw new IllegalArgumentException( "top level project's directory \""+baseDir+"\" error: "+e.getMessage() );
        }
    }

    public void addArtifact( Artifact artifact, String outputPath )
    {
        String key = calculateKey( artifact );
        
        String relTarget = calculateRelativePath( baseDir, outputPath );
        
        storage.put( key, relTarget );
    }

    /**
     * calculate the diff of basedir
     * 
     * @param baseDir parent folder
     * @param outputPath full path of the child
     * @return
     */
    private String calculateRelativePath( String baseDir, String outputPath )
    {
        int len = baseDir.length();

        return outputPath.substring( len+1 );
    }

    public String pathOf( Artifact artifact )
    {
        String key = calculateKey( artifact );
        
        String relTarget = storage.get( key );
        
        if( relTarget == null )
            return null;
        
        String targetBinaryName = calculateBinaryName( artifact );
        
        String path = relTarget+"/"+targetBinaryName;
        
        File binary = new File( baseDir, path );
        
        if( binary.exists() )
            return path;
        
        return null;
    }
    
    public String getKey()
    {
        return getId();
    }

    public ArtifactRepositoryLayout getLayout()
    {
        return layout;
    }

    public ArtifactRepositoryPolicy getReleases()
    {
        return RELEASES_POLICY;
    }

    public ArtifactRepositoryPolicy getSnapshots()
    {
        return SNAPSHOTS_POLICY;
    }

    public boolean isBlacklisted()
    {
        return false;
    }

    public boolean isUniqueVersion()
    {
        return true;
    }

    public String pathOfLocalRepositoryMetadata( ArtifactMetadata metadata, ArtifactRepository repository )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String pathOfRemoteRepositoryMetadata( ArtifactMetadata artifactMetadata )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void setBlacklisted( boolean blackListed )
    {
        // noop
    }

    public void setLayout( ArtifactRepositoryLayout layout )
    {
        // noop
    }

    public void setReleaseUpdatePolicy( ArtifactRepositoryPolicy policy )
    {
        // noop
    }

    public void setSnapshotUpdatePolicy( ArtifactRepositoryPolicy policy )
    {
        // noop
    }

    public String getBasedir()
    {
        return baseDir;
    }

    public String getId()
    {
        return ID;
    }

    public String getProtocol()
    {
        return PROTOCOL;
    }

    public String getUrl()
    {
        return url;
    }

    public void setId( String id )
    {
        // noop
    }

    public void setUrl( String url )
    {
        // noop
    }
}
