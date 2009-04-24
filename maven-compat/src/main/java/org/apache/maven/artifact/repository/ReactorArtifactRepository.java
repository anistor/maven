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
import org.apache.maven.wagon.repository.Repository;
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
extends Repository
implements ArtifactRepository
{
    private static final ArtifactRepositoryPolicy SNAPSHOTS_POLICY = new ArtifactRepositoryPolicy(true, ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER, ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE); 

    private static final ArtifactRepositoryPolicy RELEASES_POLICY = new ArtifactRepositoryPolicy(true, ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER, ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE);
    
    private static final String ID = "reactorRepository";
    
    private static String URL = "file:///tmp";
    
    @Requirement(role=ArtifactRepositoryLayout.class, hint="default")
    private static ArtifactRepositoryLayout layout;
    
    private Map<String, File> storage = new HashMap<String, File>(16);
    
    /**
     * @throws IOException 
     * 
     */
    public ReactorArtifactRepository()
    {
        super( ID, URL );
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

    /* (non-Javadoc)
     * @see org.apache.maven.artifact.repository.ArtifactRepository#pathOf(org.apache.maven.artifact.Artifact)
     */
    public String pathOf( Artifact artifact )
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.maven.artifact.repository.ArtifactRepository#pathOfLocalRepositoryMetadata(org.apache.maven.artifact.metadata.ArtifactMetadata, org.apache.maven.artifact.repository.ArtifactRepository)
     */
    public String pathOfLocalRepositoryMetadata( ArtifactMetadata metadata, ArtifactRepository repository )
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.maven.artifact.repository.ArtifactRepository#pathOfRemoteRepositoryMetadata(org.apache.maven.artifact.metadata.ArtifactMetadata)
     */
    public String pathOfRemoteRepositoryMetadata( ArtifactMetadata artifactMetadata )
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.maven.artifact.repository.ArtifactRepository#setBlacklisted(boolean)
     */
    public void setBlacklisted( boolean blackListed )
    {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.apache.maven.artifact.repository.ArtifactRepository#setLayout(org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout)
     */
    public void setLayout( ArtifactRepositoryLayout layout )
    {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.apache.maven.artifact.repository.ArtifactRepository#setReleaseUpdatePolicy(org.apache.maven.artifact.repository.ArtifactRepositoryPolicy)
     */
    public void setReleaseUpdatePolicy( ArtifactRepositoryPolicy policy )
    {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.apache.maven.artifact.repository.ArtifactRepository#setSnapshotUpdatePolicy(org.apache.maven.artifact.repository.ArtifactRepositoryPolicy)
     */
    public void setSnapshotUpdatePolicy( ArtifactRepositoryPolicy policy )
    {
        // TODO Auto-generated method stub
        
    }
}
