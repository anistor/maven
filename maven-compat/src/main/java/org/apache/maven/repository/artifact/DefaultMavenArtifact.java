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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.repository.legacy.handler.ArtifactHandler;
import org.apache.maven.repository.legacy.metadata.ArtifactMetadata;
import org.apache.maven.repository.legacy.repository.ArtifactRepository;
import org.apache.maven.repository.legacy.resolver.filter.ArtifactFilter;
import org.apache.maven.repository.legacy.versioning.ArtifactVersion;
import org.apache.maven.repository.legacy.versioning.DefaultArtifactVersion;
import org.apache.maven.repository.legacy.versioning.VersionRange;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author Jason van Zyl
 */
public class DefaultMavenArtifact
    implements MavenArtifact
{
    private String groupId;

    private String artifactId;

    private String baseVersion;

    private final String type;

    private final String classifier;

    private String scope;

    private File file;

    private ArtifactRepository repository;

    private String downloadUrl;

    private ArtifactFilter dependencyFilter;

    private ArtifactHandler artifactHandler;

    private List<String> dependencyTrail;

    private String version;

    private VersionRange versionRange;

    private boolean resolved;

    private boolean release;

    private List<ArtifactVersion> availableVersions;

    private Map<Object,ArtifactMetadata> metadataMap;
    
    private boolean optional;

    public DefaultMavenArtifact( String groupId, String artifactId, String version, String scope, String type, String classifier, ArtifactHandler artifactHandler )
    {
        this( groupId, artifactId, VersionRange.createFromVersion( version ), scope, type, classifier, artifactHandler, false );
    }
    
    public DefaultMavenArtifact( String groupId, String artifactId, VersionRange versionRange, String scope, String type, String classifier, ArtifactHandler artifactHandler )
    {
        this( groupId, artifactId, versionRange, scope, type, classifier, artifactHandler, false );
    }

    public DefaultMavenArtifact( String groupId, String artifactId, VersionRange versionRange, String scope, String type, String classifier, ArtifactHandler artifactHandler, boolean optional )
    {
        this.groupId = groupId;

        this.artifactId = artifactId;

        this.versionRange = versionRange;

        selectVersionFromNewRangeIfAvailable();

        this.artifactHandler = artifactHandler;

        this.scope = scope;

        this.type = type;

        if ( classifier == null )
        {
            classifier = artifactHandler.getClassifier();
        }

        this.classifier = classifier;

        this.optional = optional;
    }

    private boolean empty( String value )
    {
        return ( value == null ) || ( value.trim().length() < 1 );
    }

    public String getClassifier()
    {
        return classifier;
    }

    public boolean hasClassifier()
    {
        return StringUtils.isNotEmpty( classifier );
    }

    public String getScope()
    {
        return scope;
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

    public void setVersion( String version )
    {
        this.version = version;
        setBaseVersionInternal( version );
        versionRange = null;
    }

    public String getType()
    {
        return type;
    }

    public void setFile( File file )
    {
        this.file = file;
    }

    public File getFile()
    {
        return file;
    }

    public ArtifactRepository getRepository()
    {
        return repository;
    }

    public void setRepository( ArtifactRepository repository )
    {
        this.repository = repository;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public String getId()
    {
        return getDependencyConflictId() + ":" + getBaseVersion();
    }

    public String getDependencyConflictId()
    {
        StringBuffer sb = new StringBuffer();
        sb.append( getGroupId() );
        sb.append( ":" );
        appendArtifactTypeClassifierString( sb );
        return sb.toString();
    }

    private void appendArtifactTypeClassifierString( StringBuffer sb )
    {
        sb.append( getArtifactId() );
        sb.append( ":" );
        sb.append( getType() );
        if ( hasClassifier() )
        {
            sb.append( ":" );
            sb.append( getClassifier() );
        }
    }

    public void addMetadata( ArtifactMetadata metadata )
    {
        if ( metadataMap == null )
        {
            metadataMap = new HashMap<Object,ArtifactMetadata>();
        }

        ArtifactMetadata m = metadataMap.get( metadata.getKey() );
        if ( m != null )
        {
            m.merge( metadata );
        }
        else
        {
            metadataMap.put( metadata.getKey(), metadata );
        }
    }

    public Collection<ArtifactMetadata> getMetadataList()
    {
        if (metadataMap == null) {
            return Collections.emptyList();
        }

        return metadataMap.values();
    }

    // ----------------------------------------------------------------------
    // Object overrides
    // ----------------------------------------------------------------------

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        if ( getGroupId() != null )
        {
            sb.append( getGroupId() );
            sb.append( ":" );
        }
        appendArtifactTypeClassifierString( sb );
        sb.append( ":" );
        if ( getBaseVersionInternal() != null )
        {
            sb.append( getBaseVersionInternal() );
        }
        else
        {
            sb.append( versionRange.toString() );
        }
        if ( scope != null )
        {
            sb.append( ":" );
            sb.append( scope );
        }
        return sb.toString();
    }

    public int hashCode()
    {
        int result = 17;
        result = 37 * result + groupId.hashCode();
        result = 37 * result + artifactId.hashCode();
        result = 37 * result + type.hashCode();
        if ( version != null )
        {
            result = 37 * result + version.hashCode();
        }
        result = 37 * result + ( classifier != null ? classifier.hashCode() : 0 );
        return result;
    }

    public boolean equals( Object o )
    {
        if ( o == this )
        {
            return true;
        }

        if ( !( o instanceof Artifact ) )
        {
            return false;
        }

        Artifact a = (Artifact) o;

        if ( !a.getGroupId().equals( groupId ) )
        {
            return false;
        }
        else if ( !a.getArtifactId().equals( artifactId ) )
        {
            return false;
        }
        else if ( !a.getVersion().equals( version ) )
        {
            return false;
        }
        else if ( !a.getType().equals( type ) )
        {
            return false;
        }
        else if ( a.getClassifier() == null ? classifier != null : !a.getClassifier().equals( classifier ) )
        {
            return false;
        }

        // We don't consider the version range in the comparison, just the resolved version

        return true;
    }

    public String getBaseVersion()
    {
        if ( baseVersion == null )
        {
            if ( version == null )
            {
                throw new NullPointerException( "version was null for " + groupId + ":" + artifactId );
            }
            setBaseVersionInternal( version );
        }
        return baseVersion;
    }

    protected String getBaseVersionInternal()
    {
        if ( ( baseVersion == null ) && ( version != null ) )
        {
            setBaseVersionInternal( version );
        }

        return baseVersion;
    }

    public void setBaseVersion( String baseVersion )
    {
        setBaseVersionInternal( baseVersion );
    }

    protected void setBaseVersionInternal( String baseVersion )
    {
        Matcher m = VERSION_FILE_PATTERN.matcher( baseVersion );
        
        if ( m.matches() )
        {
            this.baseVersion = m.group( 1 ) + "-" + SNAPSHOT_VERSION;
        }
        else
        {
            this.baseVersion = baseVersion;
        }
    }

    public int compareTo( Object o )
    {
        Artifact a = (Artifact) o;

        int result = groupId.compareTo( a.getGroupId() );
        if ( result == 0 )
        {
            result = artifactId.compareTo( a.getArtifactId() );
            if ( result == 0 )
            {
                result = type.compareTo( a.getType() );
                if ( result == 0 )
                {
                    if ( classifier == null )
                    {
                        if ( a.getClassifier() != null )
                        {
                            result = 1;
                        }
                    }
                    else
                    {
                        if ( a.getClassifier() != null )
                        {
                            result = classifier.compareTo( a.getClassifier() );
                        }
                        else
                        {
                            result = -1;
                        }
                    }
                    if ( result == 0 )
                    {
                        // We don't consider the version range in the comparison, just the resolved version
                        result = new DefaultArtifactVersion( version ).compareTo(
                            new DefaultArtifactVersion( a.getVersion() ) );
                    }
                }
            }
        }
        return result;
    }

    public String getDownloadUrl()
    {
        return downloadUrl;
    }

    public void setDownloadUrl( String downloadUrl )
    {
        this.downloadUrl = downloadUrl;
    }

    public ArtifactFilter getDependencyFilter()
    {
        return dependencyFilter;
    }

    public void setDependencyFilter( ArtifactFilter artifactFilter )
    {
        dependencyFilter = artifactFilter;
    }

    public ArtifactHandler getArtifactHandler()
    {
        return artifactHandler;
    }

    public List<String> getDependencyTrail()
    {
        return dependencyTrail;
    }

    public void setDependencyTrail( List<String> dependencyTrail )
    {
        this.dependencyTrail = dependencyTrail;
    }

    public void setScope( String scope )
    {
        this.scope = scope;
    }

    public VersionRange getVersionRange()
    {
        return versionRange;
    }

    public void setVersionRange( VersionRange versionRange )
    {
        this.versionRange = versionRange;

        selectVersionFromNewRangeIfAvailable();
    }

    private void selectVersionFromNewRangeIfAvailable()
    {
        if ( ( versionRange != null ) && ( versionRange.getRecommendedVersion() != null ) )
        {
            selectVersion( versionRange.getRecommendedVersion().toString() );
        }
        else
        {
            version = null;
            baseVersion = null;
        }
    }

    public void selectVersion( String version )
    {
        this.version = version;
        setBaseVersionInternal( version );
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public boolean isSnapshot()
    {
        return getBaseVersion() != null && (getBaseVersion().endsWith(SNAPSHOT_VERSION) || getBaseVersion().equals(LATEST_VERSION));
    }

    public void setResolved( boolean resolved )
    {
        this.resolved = resolved;
    }

    public boolean isResolved()
    {
        return resolved;
    }

    public void setResolvedVersion( String version )
    {
        this.version = version;
        // retain baseVersion
    }

    public void setArtifactHandler( ArtifactHandler artifactHandler )
    {
        this.artifactHandler = artifactHandler;
    }

    public void setRelease( boolean release )
    {
        this.release = release;
    }

    public boolean isRelease()
    {
        return release;
    }

    public List<ArtifactVersion> getAvailableVersions()
    {
        return availableVersions;
    }

    public void setAvailableVersions( List<ArtifactVersion> availableVersions )
    {
        this.availableVersions = availableVersions;
    }

    public boolean isOptional()
    {
        return optional;
    }

    public void setOptional( boolean optional )
    {
        this.optional = optional;
    }
        
    private boolean fromAuthoritativeRepository;
    
    public void setFromAuthoritativeRepository( boolean fromAuthoritativeRepository )
    {
        this.fromAuthoritativeRepository = fromAuthoritativeRepository;
    }
    
    public boolean isFromAuthoritativeRepository()
    {
        return fromAuthoritativeRepository;
    }
}
