package org.apache.maven.project.artifact;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * Provides a view of a dependency artifact whose file is resolved from the main/test output directory of an active
 * project. Which project output directory is actually used as the artifact file depends on the type and/or classifier
 * of the artifact. The artifact's file will be <code>null</code> if the corresponding project output directory does not
 * exist.
 * 
 * @author Benjamin Bentmann
 */
public class ActiveProjectOutputArtifact
    implements Artifact
{

    /**
     * The dependency artifact to resolve from the project main/test output directory, never <code>null</code>.
     */
    private final Artifact artifact;

    /**
     * The project used to resolve the artifact file from, never <code>null</code>.
     */
    private final MavenProject project;

    /**
     * Creates a new project output artifact.
     * 
     * @param project The project used to resolve the artifact, must not be <code>null</code>.
     * @param artifact The artifact whose file should be resolved from the project main/test output directory, must not
     *            be <code>null</code>.
     * @return The project output artifact or <code>null</code> if not appropriate.
     */
    public static Artifact newInstance( MavenProject project, Artifact artifact )
    {
        if ( artifact instanceof ActiveProjectOutputArtifact )
        {
            return artifact;
        }
        if ( artifact.getArtifactHandler().isAddedToClasspath() )
        {
            return new ActiveProjectOutputArtifact( project, artifact );
        }
        return null;
    }

    private ActiveProjectOutputArtifact( MavenProject project, Artifact artifact )
    {
        if ( artifact == null )
        {
            throw new IllegalArgumentException( "artifact missing" );
        }
        this.artifact = artifact;
        if ( project == null )
        {
            throw new IllegalArgumentException( "project missing" );
        }
        this.project = project;
    }

    /** {@inheritDoc} */
    public File getFile()
    {
        File file = artifact.getFile();
        if ( file != null && file.exists() )
        {
            return file;
        }

        String path;
        if ( isTestArtifact( artifact ) )
        {
            path = project.getBuild().getTestOutputDirectory();
        }
        else
        {
            path = project.getBuild().getOutputDirectory();
        }
        file = new File( path );
        return file.isDirectory() ? file : null;
    }

    /**
     * Determines whether the specified artifact refers to test classes.
     * 
     * @param artifact The artifact to check, must not be <code>null</code>.
     * @return <code>true</code> if the artifact refers to test classes, <code>false</code> otherwise.
     */
    private static boolean isTestArtifact( Artifact artifact )
    {
        if ( "test-jar".equals( artifact.getType() ) )
        {
            return true;
        }
        else if ( "jar".equals( artifact.getType() ) && "tests".equals( artifact.getClassifier() ) )
        {
            return true;
        }
        return false;
    }

    /** {@inheritDoc} */
    public void setFile( File destination )
    {
        artifact.setFile( destination );
    }

    /** {@inheritDoc} */
    public String getGroupId()
    {
        return artifact.getGroupId();
    }

    /** {@inheritDoc} */
    public void setGroupId( String groupId )
    {
        artifact.setGroupId( groupId );
    }

    /** {@inheritDoc} */
    public String getArtifactId()
    {
        return artifact.getArtifactId();
    }

    /** {@inheritDoc} */
    public void setArtifactId( String artifactId )
    {
        artifact.setArtifactId( artifactId );
    }

    /** {@inheritDoc} */
    public String getVersion()
    {
        return artifact.getVersion();
    }

    /** {@inheritDoc} */
    public void setVersion( String version )
    {
        artifact.setVersion( version );
    }

    /** {@inheritDoc} */
    public String getScope()
    {
        return artifact.getScope();
    }

    /** {@inheritDoc} */
    public void setScope( String scope )
    {
        artifact.setScope( scope );
    }

    /** {@inheritDoc} */
    public String getType()
    {
        return artifact.getType();
    }

    /** {@inheritDoc} */
    public String getClassifier()
    {
        return artifact.getClassifier();
    }

    /** {@inheritDoc} */
    public boolean hasClassifier()
    {
        return artifact.hasClassifier();
    }

    /** {@inheritDoc} */
    public String getBaseVersion()
    {
        return artifact.getBaseVersion();
    }

    /** {@inheritDoc} */
    public void setBaseVersion( String baseVersion )
    {
        artifact.setBaseVersion( baseVersion );
    }

    /** {@inheritDoc} */
    public String getId()
    {
        return artifact.getId();
    }

    /** {@inheritDoc} */
    public String getDependencyConflictId()
    {
        return artifact.getDependencyConflictId();
    }

    /** {@inheritDoc} */
    public void addMetadata( ArtifactMetadata metadata )
    {
        artifact.addMetadata( metadata );
    }

    /** {@inheritDoc} */
    public Collection getMetadataList()
    {
        return artifact.getMetadataList();
    }

    /** {@inheritDoc} */
    public ArtifactRepository getRepository()
    {
        return artifact.getRepository();
    }

    /** {@inheritDoc} */
    public void setRepository( ArtifactRepository remoteRepository )
    {
        artifact.setRepository( remoteRepository );
    }

    /** {@inheritDoc} */
    public void updateVersion( String version, ArtifactRepository localRepository )
    {
        artifact.updateVersion( version, localRepository );
    }

    /** {@inheritDoc} */
    public String getDownloadUrl()
    {
        return artifact.getDownloadUrl();
    }

    /** {@inheritDoc} */
    public void setDownloadUrl( String downloadUrl )
    {
        artifact.setDownloadUrl( downloadUrl );
    }

    /** {@inheritDoc} */
    public ArtifactFilter getDependencyFilter()
    {
        return artifact.getDependencyFilter();
    }

    /** {@inheritDoc} */
    public void setDependencyFilter( ArtifactFilter artifactFilter )
    {
        artifact.setDependencyFilter( artifactFilter );
    }

    /** {@inheritDoc} */
    public ArtifactHandler getArtifactHandler()
    {
        return artifact.getArtifactHandler();
    }

    /** {@inheritDoc} */
    public List getDependencyTrail()
    {
        return artifact.getDependencyTrail();
    }

    /** {@inheritDoc} */
    public void setDependencyTrail( List dependencyTrail )
    {
        artifact.setDependencyTrail( dependencyTrail );
    }

    /** {@inheritDoc} */
    public VersionRange getVersionRange()
    {
        return artifact.getVersionRange();
    }

    /** {@inheritDoc} */
    public void setVersionRange( VersionRange newRange )
    {
        artifact.setVersionRange( newRange );
    }

    /** {@inheritDoc} */
    public void selectVersion( String version )
    {
        artifact.selectVersion( version );
    }

    /** {@inheritDoc} */
    public boolean isSnapshot()
    {
        return artifact.isSnapshot();
    }

    /** {@inheritDoc} */
    public int compareTo( Object o )
    {
        return artifact.compareTo( o );
    }

    /** {@inheritDoc} */
    public boolean isResolved()
    {
        return true;
    }

    /** {@inheritDoc} */
    public void setResolved( boolean resolved )
    {
        // noop
    }

    /** {@inheritDoc} */
    public void setResolvedVersion( String version )
    {
        artifact.setResolvedVersion( version );
    }

    /** {@inheritDoc} */
    public void setArtifactHandler( ArtifactHandler handler )
    {
        artifact.setArtifactHandler( handler );
    }

    /** {@inheritDoc} */
    public String toString()
    {
        return artifact + " <- " + project;
    }

    /** {@inheritDoc} */
    public boolean isRelease()
    {
        return artifact.isRelease();
    }

    /** {@inheritDoc} */
    public void setRelease( boolean release )
    {
        artifact.setRelease( release );
    }

    /** {@inheritDoc} */
    public List getAvailableVersions()
    {
        return artifact.getAvailableVersions();
    }

    /** {@inheritDoc} */
    public void setAvailableVersions( List versions )
    {
        artifact.setAvailableVersions( versions );
    }

    /** {@inheritDoc} */
    public boolean isOptional()
    {
        return artifact.isOptional();
    }

    /** {@inheritDoc} */
    public void setOptional( boolean optional )
    {
        artifact.setOptional( optional );
    }

    /** {@inheritDoc} */
    public ArtifactVersion getSelectedVersion()
        throws OverConstrainedVersionException
    {
        return artifact.getSelectedVersion();
    }

    /** {@inheritDoc} */
    public boolean isSelectedVersionKnown()
        throws OverConstrainedVersionException
    {
        return artifact.isSelectedVersionKnown();
    }

    /** {@inheritDoc} */
    public int hashCode()
    {
        int result = 17;

        result = 37 * result + getGroupId().hashCode();
        result = 37 * result + getArtifactId().hashCode();
        result = 37 * result + getType().hashCode();
        if ( getVersion() != null )
        {
            result = 37 * result + getVersion().hashCode();
        }
        result = 37 * result + ( getClassifier() != null ? getClassifier().hashCode() : 0 );

        return result;
    }

    /** {@inheritDoc} */
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

        if ( !a.getGroupId().equals( getGroupId() ) )
        {
            return false;
        }
        else if ( !a.getArtifactId().equals( getArtifactId() ) )
        {
            return false;
        }
        else if ( !a.getVersion().equals( getVersion() ) )
        {
            return false;
        }
        else if ( !a.getType().equals( getType() ) )
        {
            return false;
        }
        else if ( a.getClassifier() == null ? getClassifier() != null : !a.getClassifier().equals( getClassifier() ) )
        {
            return false;
        }

        return true;
    }

    public ArtifactMetadata getMetadata( Class metadataClass )
    {
        return artifact.getMetadata( metadataClass );
    }

}
