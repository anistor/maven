package org.apache.maven.artifact.metadata;

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

import java.io.File;

public class LatestArtifactMetadata
    extends AbstractVersionArtifactMetadata
{
    private String version;

    public LatestArtifactMetadata( Artifact artifact )
    {
        super( artifact );
    }

    public String getRemoteFilename()
    {
        return getFilename();
    }

    public String getLocalFilename( ArtifactRepository repository )
    {
        return getFilename();
    }

    private String getFilename()
    {
        return artifact.getArtifactId() + "-" + Artifact.LATEST_VERSION + "." + SNAPSHOT_VERSION_FILE;
    }

    public String constructVersion()
    {
        return version;
    }

    public int compareTo( Object o )
    {
        LatestArtifactMetadata metadata = (LatestArtifactMetadata) o;

        if ( version == null )
        {
            if ( metadata.version == null )
            {
                return 0;
            }
            else
            {
                return -1;
            }
        }
        else
        {
            if ( metadata.version == null )
            {
                return 1;
            }
            else
            {
                return version.compareTo( metadata.version );
            }
        }
    }

    public boolean newerThanFile( File file )
    {
        long fileTime = file.lastModified();

        return lastModified > fileTime;
    }

    public String toString()
    {
        return "latest-version information for " + artifact.getArtifactId();
    }

    protected void setContent( String content )
    {
        this.version = content.trim();
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public String getBaseVersion()
    {
        return Artifact.LATEST_VERSION;
    }

    public boolean storedInArtifactVersionDirectory()
    {
        return false;
    }

    public boolean isSnapshot()
    {
        return false;
    }
}
