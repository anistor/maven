package org.apache.maven.artifact;

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

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.factory.DefaultArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;

import java.io.FileReader;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id$
 */
public class MavenMetadataSource
    implements ArtifactMetadataSource
{
    private MavenProjectBuilder mavenProjectBuilder;

    private ArtifactResolver artifactResolver;

    // TODO: configure?
    protected ArtifactFactory artifactFactory = new DefaultArtifactFactory();

    /**
     * @todo remove.
     */
    private MavenXpp3Reader reader = new MavenXpp3Reader();

    public MavenMetadataSource( ArtifactResolver artifactResolver )
    {
        // there is code in plexus that uses this (though it shouldn't) so we
        // need to be able to not have a project builder
        // TODO: remove, then remove those null checks
        this.artifactResolver = artifactResolver;
        this.mavenProjectBuilder = null;
    }

    public MavenMetadataSource( ArtifactResolver artifactResolver, MavenProjectBuilder projectBuilder )
    {
        this.artifactResolver = artifactResolver;
        this.mavenProjectBuilder = projectBuilder;
    }

    public Set retrieve( Artifact artifact, ArtifactRepository localRepository, List remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        try
        {
            List dependencies = null;

            if ( mavenProjectBuilder != null )
            {
                MavenProject project = mavenProjectBuilder.getCachedProject( artifact.getGroupId(),
                                                                             artifact.getArtifactId(),
                                                                             artifact.getVersion() );
                if ( project != null )
                {
                    dependencies = project.getDependencies();
                }
            }

            if ( dependencies == null )
            {
                Artifact metadataArtifact = artifactFactory.createArtifact( artifact.getGroupId(),
                                                                            artifact.getArtifactId(),
                                                                            artifact.getVersion(), artifact.getScope(),
                                                                            "pom", "pom", null );

                artifactResolver.resolve( metadataArtifact, remoteRepositories, localRepository );

                // [jdcasey/03-Feb-2005]: Replacing with ProjectBuilder, to enable
                // post-processing and inheritance calculation before retrieving the
                // associated artifacts. This should improve consistency.
                try
                {
                    if ( mavenProjectBuilder != null )
                    {
                        MavenProject p = mavenProjectBuilder.buildFromRepository( metadataArtifact, localRepository );
                        dependencies = p.getDependencies();
                    }
                    else
                    {
                        Model model = reader.read( new FileReader( metadataArtifact.getFile() ) );
                        dependencies = model.getDependencies();
                    }
                }
                catch ( Exception e )
                {
                    throw new ArtifactMetadataRetrievalException(
                        "Cannot read artifact source: " + metadataArtifact.getPath(), e );
                }
            }
            return artifactFactory.createArtifacts( dependencies, localRepository, artifact.getScope() );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new ArtifactMetadataRetrievalException( "Error while resolving metadata artifact", e );
        }
    }
}
