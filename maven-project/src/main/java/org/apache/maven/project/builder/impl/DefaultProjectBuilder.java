package org.apache.maven.project.builder.impl;

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
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.builder.ArtifactModelContainerFactory;
import org.apache.maven.project.builder.IdModelContainerFactory;
import org.apache.maven.project.builder.PomArtifactResolver;
import org.apache.maven.project.builder.PomClassicDomainModel;
import org.apache.maven.project.builder.PomClassicTransformer;
import org.apache.maven.project.builder.ProjectBuilder;
import org.apache.maven.project.validation.ModelValidationResult;
import org.apache.maven.project.validation.ModelValidator;
import org.apache.maven.shared.model.DomainModel;
import org.apache.maven.shared.model.InterpolatorProperty;
import org.apache.maven.shared.model.ModelTransformerContext;
import org.apache.maven.mercury.builder.api.MetadataProcessor;
import org.apache.maven.mercury.builder.api.MetadataReader;
import org.apache.maven.mercury.builder.api.MetadataProcessingException;
import org.apache.maven.mercury.artifact.ArtifactBasicMetadata;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;

import java.io.*;
import java.util.*;

/**
 * Default implementation of the project builder.
 */
public final class DefaultProjectBuilder
    implements ProjectBuilder, MetadataProcessor, LogEnabled
{
    /**
     * Logger instance
     */
    private Logger logger;

    private ModelValidator validator;

    private MetadataReader metadataReader;


    /**
     * Constructor
     */
    public DefaultProjectBuilder( )
    {
    }

    /**
     *
     * @param metadataReader metadata reader
     */
    public void init(MetadataReader metadataReader) {
        if ( metadataReader == null )
        {
            throw new IllegalArgumentException( "metadataReader: null" );
        }
        this.metadataReader = metadataReader;
    }
    /**
     * @see ProjectBuilder#buildFromLocalPath(java.io.InputStream, java.util.List, java.util.Collection, java.io.File)
     */
    public MavenProject buildFromLocalPath( InputStream pom, List<Model> inheritedModels,
                                            Collection<InterpolatorProperty> interpolatorProperties,
                                            File projectDirectory )
        throws IOException
    {
        if ( pom == null )
        {
            throw new IllegalArgumentException( "pom: null" );
        }

        if ( metadataReader == null )
        {
            throw new IllegalArgumentException( "metadataReader not initialized" );
        }

        if ( projectDirectory == null )
        {
            throw new IllegalArgumentException( "projectDirectory: null" );
        }

        if ( inheritedModels == null )
        {
            inheritedModels = new ArrayList<Model>();
        }
        else
        {
            inheritedModels = new ArrayList<Model>( inheritedModels );
            Collections.reverse( inheritedModels );
        }

        List<InterpolatorProperty> properties;
        if ( interpolatorProperties == null )
        {
            properties = new ArrayList<InterpolatorProperty>();
        }
        else
        {
            properties = new ArrayList<InterpolatorProperty>( interpolatorProperties );
        }

        PomClassicDomainModel domainModel = new PomClassicDomainModel( pom );
        List<DomainModel> domainModels = new ArrayList<DomainModel>();
        domainModels.add( domainModel );

        if ( domainModel.getModel().getParent() != null )
        {
            if ( isParentLocal( domainModel.getModel().getParent(), projectDirectory ) )
            {
                domainModels.addAll( getDomainModelParentsFromLocalPath( domainModel, projectDirectory ) );
            }
            else
            {
                domainModels.addAll( getDomainModelParentsFromRepository( domainModel ) );
            }
        }

        for ( Model model : inheritedModels )
        {
            domainModels.add( new PomClassicDomainModel( model ) );
        }

        PomClassicTransformer transformer = new PomClassicTransformer();
        ModelTransformerContext ctx = new ModelTransformerContext(
            Arrays.asList( new ArtifactModelContainerFactory(), new IdModelContainerFactory() ) );

        PomClassicDomainModel transformedDomainModel =
            ( (PomClassicDomainModel) ctx.transform( domainModels, transformer, transformer, null, properties ) );
        Model model = transformedDomainModel.getModel();
        return new MavenProject( model );
    }

    private boolean isParentLocal( Parent parent, File projectDirectory )
    {
        try
        {
            File f = new File( projectDirectory, parent.getRelativePath() ).getCanonicalFile();
            if ( f.isDirectory() )
            {
                f = new File( f, "pom.xml" );
            }
            return f.exists();
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            return false;
        }
    }

    private List<DomainModel> getDomainModelParentsFromRepository( PomClassicDomainModel domainModel)
        throws IOException
    {

        List<DomainModel> domainModels = new ArrayList<DomainModel>();

        Parent parent = domainModel.getModel().getParent();

        if ( parent == null )
        {
            return domainModels;
        }

        PomClassicDomainModel parentDomainModel;
        try {
            parentDomainModel = new PomClassicDomainModel( new ByteArrayInputStream(
                    metadataReader.readMetadata(domainModel.asArtifactBasicMetadata())) );
        } catch (MetadataProcessingException e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        }

        if ( !parentDomainModel.matchesParent( domainModel.getModel().getParent() ) )
        {
            logger.warn( "Parent pom ids do not match: Id = " + domainModel.getId() );
            return domainModels;
        }

        domainModels.add( parentDomainModel );
        domainModels.addAll( getDomainModelParentsFromRepository( parentDomainModel) );
        return domainModels;
    }


    private List<DomainModel> getDomainModelParentsFromLocalPath( PomClassicDomainModel domainModel,
                                                                  File projectDirectory )
        throws IOException
    {

        List<DomainModel> domainModels = new ArrayList<DomainModel>();

        Parent parent = domainModel.getModel().getParent();

        if ( parent == null )
        {
            return domainModels;
        }

        Model model = domainModel.getModel();

        File parentFile = new File( projectDirectory, model.getParent().getRelativePath() ).getCanonicalFile();
        if ( parentFile.isDirectory() )
        {
            parentFile = new File( parentFile.getAbsolutePath(), "pom.xml" );
        }

        if ( !parentFile.exists() )
        {
            throw new IOException( "File does not exist: File =" + parentFile.getAbsolutePath() );
        }

        PomClassicDomainModel parentDomainModel = new PomClassicDomainModel( new FileInputStream( parentFile ) );
        if ( !parentDomainModel.matchesParent( domainModel.getModel().getParent() ) )
        {
            logger.warn( "Parent pom ids do not match: File = " + parentFile.getAbsolutePath() );
        }

        domainModels.add( parentDomainModel );
        if ( parentDomainModel.getModel().getParent() != null )
        {
            if ( isParentLocal( parentDomainModel.getModel().getParent(), parentFile.getParentFile() ) )
            {
                domainModels.addAll( getDomainModelParentsFromLocalPath( parentDomainModel,
                                                                         parentFile.getParentFile() ) );
            }
            else
            {
                domainModels.addAll( getDomainModelParentsFromRepository( parentDomainModel) );
            }
        }

        return domainModels;
    }


    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

    private void validateModel( Model model )
        throws IOException
    {
        ModelValidationResult validationResult = validator.validate( model );

        if ( validationResult.getMessageCount() > 0 )
        {
            throw new IOException( "Failed to validate: " + validationResult.toString() );
        }
    }


    public List<ArtifactBasicMetadata> getDependencies(ArtifactBasicMetadata bmd, MetadataReader mdReader, Hashtable env) 
            throws MetadataProcessingException {
        //PomClassicDomainModel model = new PomClassicDomainModel(new ByteArrayInputStream(mdReader.readMetadata(bmd)));

       // return model.asArtifactBasicMetadata();
       return null;
    }
}
