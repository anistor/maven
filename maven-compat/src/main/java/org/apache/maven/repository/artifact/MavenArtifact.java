package org.apache.maven.repository.artifact;

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
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.maven.repository.legacy.handler.ArtifactHandler;
import org.apache.maven.repository.legacy.metadata.ArtifactMetadata;
import org.apache.maven.repository.legacy.repository.ArtifactRepository;
import org.apache.maven.repository.legacy.resolver.filter.ArtifactFilter;
import org.apache.maven.repository.legacy.versioning.ArtifactVersion;
import org.apache.maven.repository.legacy.versioning.VersionRange;

/**
 * @author Jason van Zyl
 */
public interface MavenArtifact
    extends Comparable
{
    String LATEST_VERSION = "LATEST";

    String SNAPSHOT_VERSION = "SNAPSHOT";

    String RELEASE_VERSION = "RELEASE";
    
    Pattern VERSION_FILE_PATTERN = Pattern.compile( "^(.*)-([0-9]{8}.[0-9]{6})-([0-9]+)$" );

    String SCOPE_COMPILE = "compile";

    String SCOPE_TEST = "test";

    String SCOPE_RUNTIME = "runtime";

    String SCOPE_RUNTIME_PLUS_SYSTEM = "runtime_plus_system";

    String SCOPE_PROVIDED = "provided";

    String SCOPE_SYSTEM = "system";

    String SCOPE_IMPORT = "import";   // Used to import dependencyManagement dependencies

    String getGroupId();

    String getArtifactId();

    String getVersion();

    void setVersion( String version );

    String getScope();

    String getType();

    String getClassifier();

    boolean hasClassifier();

    File getFile();

    void setFile( File destination );

    String getBaseVersion();

    void setBaseVersion( String baseVersion );

    String getId();

    String getDependencyConflictId();

    void addMetadata( ArtifactMetadata metadata );

    Collection<ArtifactMetadata> getMetadataList();

    void setRepository( ArtifactRepository remoteRepository );

    ArtifactRepository getRepository();

    String getDownloadUrl();

    void setDownloadUrl( String downloadUrl );

    ArtifactFilter getDependencyFilter();

    void setDependencyFilter( ArtifactFilter artifactFilter );

    ArtifactHandler getArtifactHandler();

    List<String> getDependencyTrail();

    void setDependencyTrail( List<String> dependencyTrail );

    void setScope( String scope );

    VersionRange getVersionRange();

    void setVersionRange( VersionRange newRange );

    void selectVersion( String version );

    void setGroupId( String groupId );

    void setArtifactId( String artifactId );

    boolean isSnapshot();

    void setResolved( boolean resolved );

    boolean isResolved();

    void setResolvedVersion( String version );

    void setArtifactHandler( ArtifactHandler handler );

    boolean isRelease();

    void setRelease( boolean release );

    List<ArtifactVersion> getAvailableVersions();

    void setAvailableVersions( List<ArtifactVersion> versions );

    boolean isOptional();

    void setOptional( boolean optional );
    
    void setFromAuthoritativeRepository( boolean fromAuthoritativeRepository );
    
    boolean isFromAuthoritativeRepository();
}