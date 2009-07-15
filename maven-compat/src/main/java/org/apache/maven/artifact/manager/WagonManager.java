package org.apache.maven.artifact.manager;

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

import org.apache.maven.repository.legacy.WagonConfigurationException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.repository.Repository;

/**
 * Manages <a href="http://maven.apache.org/wagon">Wagon</a> related operations in Maven.
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka </a>
 * @version $Id$
 */
@Deprecated
public interface WagonManager
{
    @Deprecated
    Wagon getWagon( String protocol )
        throws UnsupportedProtocolException;

    @Deprecated
    Wagon getWagon( Repository repository )
        throws UnsupportedProtocolException, WagonConfigurationException;

    /*
    
    Will restore as necessary
    
    //
    // Retriever
    //        
    void getArtifact( Artifact artifact, ArtifactRepository repository, TransferListener transferListener )
        throws TransferFailedException, ResourceDoesNotExistException;

    void getArtifact( Artifact artifact, List<ArtifactRepository> remoteRepositories, TransferListener transferListener )
        throws TransferFailedException, ResourceDoesNotExistException;

    void getRemoteFile( ArtifactRepository repository, File destination, String remotePath, TransferListener downloadMonitor, String checksumPolicy, boolean force )
        throws TransferFailedException, ResourceDoesNotExistException;

    void getArtifactMetadata( ArtifactMetadata metadata, ArtifactRepository remoteRepository, File destination, String checksumPolicy )
        throws TransferFailedException, ResourceDoesNotExistException;

    void getArtifactMetadataFromDeploymentRepository( ArtifactMetadata metadata, ArtifactRepository remoteRepository, File file, String checksumPolicyWarn )
        throws TransferFailedException, ResourceDoesNotExistException;

    //
    // Deployer
    //
    void putArtifact( File source, Artifact artifact, ArtifactRepository deploymentRepository, TransferListener downloadMonitor )
        throws TransferFailedException;

    void putRemoteFile( ArtifactRepository repository, File source, String remotePath, TransferListener downloadMonitor )
        throws TransferFailedException;

    void putArtifactMetadata( File source, ArtifactMetadata artifactMetadata, ArtifactRepository repository )
        throws TransferFailedException;    
    */
}
