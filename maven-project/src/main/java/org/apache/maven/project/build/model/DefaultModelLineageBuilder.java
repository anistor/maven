package org.apache.maven.project.build.model;

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

import org.apache.maven.MavenTools;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.context.BuildContextManager;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.build.ProjectBuildCache;
import org.apache.maven.profiles.build.ProfileAdvisor;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @see org.apache.maven.project.build.model.ModelLineageBuilder
 */
public class DefaultModelLineageBuilder
    implements ModelLineageBuilder, LogEnabled
{

    public static final String ROLE_HINT = "default";

    private ArtifactFactory artifactFactory;

    private ArtifactResolver artifactResolver;

    private MavenTools mavenTools;

    private ProfileAdvisor profileAdvisor;

    private BuildContextManager buildContextManager;

    private Logger logger;

    public DefaultModelLineageBuilder()
    {
    }

    public DefaultModelLineageBuilder( ArtifactResolver resolver, ArtifactFactory artifactFactory, BuildContextManager buildContextManager )
    {
        artifactResolver = resolver;
        this.artifactFactory = artifactFactory;
        this.buildContextManager = buildContextManager;
    }

    /**
     * @see org.apache.maven.project.build.model.ModelLineageBuilder#buildModelLineage(java.io.File, org.apache.maven.artifact.repository.ArtifactRepository, java.util.List)
     */
    public ModelLineage buildModelLineage( File pom, ArtifactRepository localRepository, List remoteRepositories,
                                           ProfileManager profileManager, boolean allowStubs, boolean validProfilesXmlLocation )
        throws ProjectBuildingException
    {
        ProjectBuildCache projectBuildCache = ProjectBuildCache.read( buildContextManager );

        ModelLineage lineage = new DefaultModelLineage();

        List currentRemoteRepositories = remoteRepositories == null ? new ArrayList()
                                                                   : new ArrayList( remoteRepositories );

        ModelAndFile current = new ModelAndFile( readModel( pom, projectBuildCache ), pom, validProfilesXmlLocation );

        do
        {
            if ( lineage.size() == 0 )
            {
                lineage.setOrigin( current.model, current.file, currentRemoteRepositories, current.validProfilesXmlLocation );
            }
            else
            {
                lineage.addParent( current.model, current.file, currentRemoteRepositories, current.validProfilesXmlLocation );
            }

            currentRemoteRepositories = updateRepositorySet( current.model, currentRemoteRepositories, current.file, profileManager, current.validProfilesXmlLocation );

            current = resolveParentPom( current, currentRemoteRepositories, localRepository, projectBuildCache, allowStubs );
        }
        while ( current != null );

        return lineage;
    }

    public void resumeBuildingModelLineage( ModelLineage lineage, ArtifactRepository localRepository,
                                            ProfileManager profileManager, boolean allowStubs )
        throws ProjectBuildingException
    {
        if ( lineage.size() == 0 )
        {
            throw new ProjectBuildingException( "unknown", "Cannot resume a ModelLineage that doesn't contain at least one Model instance." );
        }

        ProjectBuildCache projectBuildCache = ProjectBuildCache.read( buildContextManager );

        List currentRemoteRepositories = lineage.getDeepestAncestorArtifactRepositoryList();

        if ( currentRemoteRepositories == null )
        {
            currentRemoteRepositories = new ArrayList();
        }

        ModelAndFile current = new ModelAndFile( lineage.getDeepestAncestorModel(), lineage.getDeepestAncestorFile(), lineage.isDeepestAncestorUsingProfilesXml() );

        // use the above information to re-bootstrap the resolution chain...
        current = resolveParentPom( current, currentRemoteRepositories, localRepository, projectBuildCache, allowStubs );

        while ( current != null )
        {
            lineage.addParent( current.model, current.file, currentRemoteRepositories, current.validProfilesXmlLocation );

            currentRemoteRepositories = updateRepositorySet( current.model, currentRemoteRepositories, current.file, profileManager, current.validProfilesXmlLocation );

            current = resolveParentPom( current, currentRemoteRepositories, localRepository, projectBuildCache, allowStubs );
        }
    }

    /**
     * Read the Model instance from the given POM file. Skip caching the Model on this call, since
     * it's meant for diagnostic purposes (to determine a parent match).
     */
    private Model readModel( File pomFile )
        throws ProjectBuildingException
    {
        return readModel( pomFile, null, true );
    }

    /**
     * Read the Model instance from the given POM file, and cache it in the given Map before
     * returning it.
     */
    private Model readModel( File pomFile, ProjectBuildCache projectBuildCache )
        throws ProjectBuildingException
    {
        return readModel( pomFile, projectBuildCache, false );
    }

    /**
     * Read the Model instance from the given POM file. Optionally (in normal cases) cache the
     * Model instance in the given Map before returning it. The skipCache flag controls whether the
     * Model instance is actually cached.
     */
    private Model readModel( File pom, ProjectBuildCache projectBuildCache, boolean skipCache )
        throws ProjectBuildingException
    {
        File pomFile = pom;
        if ( pom.isDirectory() )
        {
            pomFile = new File( pom, "pom.xml" );
//            getLogger().debug( "readModel(..): POM: " + pom + " is a directory. Trying: " + pomFile + " instead." );
        }

        Model model;
        Reader reader = null;

        try
        {
            reader = ReaderFactory.newXmlReader( pomFile );
            model = new MavenXpp3Reader().read( reader );
        }
        catch ( IOException e )
        {
            throw new ProjectBuildingException( "unknown", "Failed to read model from: " + pomFile, pomFile, e );
        }
        catch ( XmlPullParserException e )
        {
            throw new ProjectBuildingException( "unknown", "Failed to parse model from: " + pomFile, pomFile, e );
        }
        finally
        {
            IOUtil.close( reader );
        }

        if ( !skipCache )
        {
            projectBuildCache.cacheModelFileForModel( pomFile, model );
            projectBuildCache.store( buildContextManager );
        }

        return model;
    }

    /**
     * Update the remote repository set used to resolve parent POMs, by adding those declared in
     * the given model to the HEAD of a new list, then appending the old remote repositories list.
     * The specified pomFile is used for error reporting.
     * @param profileManager
     */
    private List updateRepositorySet( Model model, List oldArtifactRepositories, File pomFile,
                                      ProfileManager externalProfileManager, boolean useProfilesXml )
        throws ProjectBuildingException
    {
        List repositories = model.getRepositories();

        File projectDir = pomFile == null ? null : pomFile.getParentFile();

        Set artifactRepositories = null;

        if ( repositories != null )
        {
            try
            {
                List lastRemoteRepos = oldArtifactRepositories;
                List remoteRepos = mavenTools.buildArtifactRepositories( repositories );

                loadActiveProfileRepositories( remoteRepos, model, externalProfileManager, projectDir, useProfilesXml );

                artifactRepositories = new LinkedHashSet( remoteRepos.size() + oldArtifactRepositories.size() );

                artifactRepositories.addAll( remoteRepos );
                artifactRepositories.addAll( lastRemoteRepos );
            }
            catch ( InvalidRepositoryException e )
            {
                throw new ProjectBuildingException( model.getId(), "Failed to create ArtifactRepository list for: "
                    + pomFile, pomFile, e );
            }
        }

        return new ArrayList( artifactRepositories );
    }

    private void loadActiveProfileRepositories( List repositories, Model model, ProfileManager profileManager,
                                                File pomFile, boolean useProfilesXml )
        throws ProjectBuildingException
    {
        List explicitlyActive;
        List explicitlyInactive;

        if ( profileManager != null )
        {
            explicitlyActive = profileManager.getExplicitlyActivatedIds();
            explicitlyInactive = profileManager.getExplicitlyDeactivatedIds();
        }
        else
        {
            explicitlyActive = Collections.EMPTY_LIST;
            explicitlyInactive = Collections.EMPTY_LIST;
        }

        LinkedHashSet profileRepos = profileAdvisor.getArtifactRepositoriesFromActiveProfiles( profileManager, pomFile, model.getId() );

        profileRepos.addAll( profileAdvisor.getArtifactRepositoriesFromActiveProfiles( model, pomFile,
                                                                                               explicitlyActive,
                                                                                               explicitlyInactive,
                                                                                               useProfilesXml ) );

        if ( !profileRepos.isEmpty() )
        {
            repositories.addAll( profileRepos );
        }
    }

    /**
     * Pull the parent specification out of the given model, construct an Artifact instance, and
     * resolve that artifact...then, return the resolved POM file for the parent.
     * @param projectBuildCache
     * @param allowStubs
     */
    private ModelAndFile resolveParentPom( ModelAndFile child, List remoteRepositories, ArtifactRepository localRepository,
                                   ProjectBuildCache projectBuildCache, boolean allowStubs )
        throws ProjectBuildingException
    {
        Model model = child.model;
        File modelPomFile = child.file;

        Parent modelParent = model.getParent();

        ModelAndFile result = null;

        if ( modelParent != null )
        {
              validateParentDeclaration( modelParent, model );

            File parentPomFile = projectBuildCache.getCachedModelFile( modelParent );

            if ( ( parentPomFile == null ) && ( modelPomFile != null ) )
            {
                parentPomFile = resolveParentWithRelativePath( modelParent, modelPomFile );
            }

            boolean isResolved = false;

            if ( parentPomFile == null )
            {
                try
                {
                    parentPomFile = resolveParentFromRepositories( modelParent, localRepository, remoteRepositories, model.getId(), modelPomFile );
                    isResolved = true;
                }
                catch( ProjectBuildingException e )
                {
                    if ( allowStubs )
                    {
                        getLogger().debug( "DISREGARDING the error encountered while resolving artifact for: " + modelParent.getId() + ", building a stub model in its place.", e );
                        parentPomFile = null;
                    }
                    else
                    {
                        throw e;
                    }
                }
            }

            if ( parentPomFile == null )
            {
                if ( allowStubs )
                {
                    getLogger().warn( "Cannot find parent POM: " + modelParent.getId() + " for child: " + model.getId() + ". Using stub model instead." );

                    Model parent = new Model();

                    parent.setGroupId( modelParent.getGroupId() );
                    parent.setArtifactId( modelParent.getArtifactId() );
                    parent.setVersion( modelParent.getVersion() );

                    // we act as if the POM was resolved from the repository,
                    // for the purposes of external profiles.xml files...
                    // that's what the last parameter is about.
                    result = new ModelAndFile( parent, parentPomFile, false );
                }
                else
                {
                    getLogger().error( "Cannot find parent POM: " + modelParent.getId() );
                }
            }
            else
            {
                Model parent = readModel( parentPomFile );
                result = new ModelAndFile( parent, parentPomFile, !isResolved );
            }
        }

        return result;
    }

    private void validateParentDeclaration( Parent modelParent, Model model )
        throws ProjectBuildingException
    {
        if ( StringUtils.isEmpty( modelParent.getGroupId() ) )
        {
            throw new ProjectBuildingException( model.getId(), "Missing groupId element from parent element" );
        }
        else if ( StringUtils.isEmpty( modelParent.getArtifactId() ) )
        {
            throw new ProjectBuildingException( model.getId(), "Missing artifactId element from parent element" );
        }
        else if ( modelParent.getGroupId().equals( model.getGroupId() )
            && modelParent.getArtifactId().equals( model.getArtifactId() ) )
        {
            throw new ProjectBuildingException( model.getId(), "Parent element is a duplicate of "
                + "the current project " );
        }
        else if ( StringUtils.isEmpty( modelParent.getVersion() ) )
        {
            throw new ProjectBuildingException( model.getId(), "Missing version element from parent element" );
        }
    }

    private File resolveParentFromRepositories( Parent modelParent, ArtifactRepository localRepository,
                                                List remoteRepositories, String childId, File childPomFile )
        throws ProjectBuildingException
    {
        Artifact parentPomArtifact = artifactFactory.createBuildArtifact( modelParent.getGroupId(), modelParent
            .getArtifactId(), modelParent.getVersion(), "pom" );

        try
        {
            artifactResolver.resolve( parentPomArtifact, remoteRepositories, localRepository );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new ProjectBuildingException( childId, "Failed to resolve parent POM: " + modelParent.getId(), childPomFile, e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new ProjectBuildingException( childId, "Cannot find artifact for parent POM: " + modelParent.getId(), childPomFile, e );
        }

        if ( parentPomArtifact.isResolved() )
        {
            return parentPomArtifact.getFile();
        }
        else
        {
            return null;
        }
    }

    private File resolveParentWithRelativePath( Parent modelParent, File modelPomFile )
        throws ProjectBuildingException
    {
        String relativePath = modelParent.getRelativePath();
        File modelDir = modelPomFile.getParentFile();

        File parentPomFile = new File( modelDir, relativePath );

        if ( parentPomFile.isDirectory() )
        {
//            getLogger().debug( "Parent relative-path is a directory; assuming \'pom.xml\' file exists within." );
            parentPomFile = new File( parentPomFile, "pom.xml" );
        }

//        getLogger().debug( "Looking for parent: " + modelParent.getId() + " in: " + parentPomFile );

        if ( parentPomFile.exists() )
        {
            Model parentModel = readModel( parentPomFile );

            boolean groupsMatch = ( parentModel.getGroupId() == null )
                || parentModel.getGroupId().equals( modelParent.getGroupId() );
            boolean versionsMatch = ( parentModel.getVersion() == null )
                || parentModel.getVersion().equals( modelParent.getVersion() );

            if ( groupsMatch && versionsMatch && parentModel.getArtifactId().equals( modelParent.getArtifactId() ) )
            {
                return parentPomFile;
            }
        }

        return null;
    }

    private Logger getLogger()
    {
        if ( logger == null )
        {
            logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "DefaultModelLineageBuilder:internal" );
        }

        return logger;
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

    private static final class ModelAndFile
    {
        private final Model model;
        private final File file;
        private final boolean validProfilesXmlLocation;

        ModelAndFile( Model model, File file, boolean validProfilesXmlLocation )
        {
            this.model = model;
            this.file = file;
            this.validProfilesXmlLocation = validProfilesXmlLocation;
        }
    }

}
