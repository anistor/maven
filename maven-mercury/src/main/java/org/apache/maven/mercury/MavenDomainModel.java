package org.apache.maven.mercury;

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

import org.apache.maven.shared.model.*;
import org.apache.maven.shared.model.impl.DefaultModelDataSource;
import org.apache.maven.project.builder.ProjectUri;
import org.apache.maven.project.builder.ArtifactModelContainerFactory;
import org.apache.maven.project.builder.IdModelContainerFactory;
import org.apache.maven.mercury.artifact.ArtifactBasicMetadata;
import org.apache.maven.mercury.artifact.ArtifactMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Provides a wrapper for the maven model.
 */
public final class MavenDomainModel implements DomainModel {

    /**
     * Bytes containing the underlying model
     */
    private final List<ModelProperty> modelProperties;

    /**
     * History of joins and deletes of model properties
     */
    private String eventHistory;

    private ArtifactBasicMetadata parentMetadata;


    /**
     * Constructor
     *
     * @throws IOException if there is a problem constructing the model
     */
    public MavenDomainModel(byte[] bytes)
            throws IOException {
        this(new ByteArrayInputStream(bytes));
    }

    /**
     * Constructor
     *
     * @throws IOException if there is a problem constructing the model
     */
    public MavenDomainModel(InputStream inputStream)
            throws IOException {
        this(ModelMarshaller.marshallXmlToModelProperties(inputStream, ProjectUri.baseUri, MercuryPomTransformer.URIS));
    }

    /**
     * Constructor
     *
     * @throws IOException if there is a problem constructing the model
     */
    public MavenDomainModel(List<ModelProperty> modelProperties)
            throws IOException {
        if (modelProperties == null) {
            throw new IllegalArgumentException("modelProperties: null");
        }

        this.modelProperties = new ArrayList<ModelProperty>(modelProperties);
    }

    public boolean hasParent() {
        //TODO: Expensive call if no parent
        return getParentMetadata() != null;
    }

    public List<ArtifactBasicMetadata> getDependencyMetadata() throws DataSourceException {
        List<ArtifactBasicMetadata> metadatas = new ArrayList<ArtifactBasicMetadata>();

        ModelDataSource source = new DefaultModelDataSource();
        source.init(modelProperties, Arrays.asList(new ArtifactModelContainerFactory(), new IdModelContainerFactory()));
        for(ModelContainer modelContainer: source.queryFor(ProjectUri.Dependencies.Dependency.xUri)) {
            metadatas.add(transformContainerToMetadata(modelContainer));
        }

        return metadatas;
    }

    public ArtifactBasicMetadata getParentMetadata() {
        if (parentMetadata != null) {
            return copyArtifactBasicMetadata(parentMetadata);
        }
        String groupId = null, artifactId = null, version = null;

        for (ModelProperty mp : modelProperties) {
            System.out.println(mp);
            if (mp.getUri().equals(ProjectUri.Parent.version)) {
                version = mp.getValue();
            } else if (mp.getUri().equals(ProjectUri.Parent.artifactId)) {
                artifactId = mp.getValue();
            } else if (mp.getUri().equals(ProjectUri.Parent.groupId)) {
                groupId = mp.getValue();
            }
            if (groupId != null && artifactId != null && version != null) {
                break;
            }
        }

        if (groupId == null || artifactId == null || version == null) {
            System.out.println(groupId + ":" + artifactId + ":" + version);
            return null;
        }
        parentMetadata = new ArtifactBasicMetadata();
        parentMetadata.setArtifactId(artifactId);
        parentMetadata.setVersion(version);
        parentMetadata.setGroupId(groupId);

        return copyArtifactBasicMetadata(parentMetadata);
    }

    private ArtifactBasicMetadata copyArtifactBasicMetadata(ArtifactBasicMetadata metadata) {
        ArtifactMetadata amd = new ArtifactMetadata();
        amd.setArtifactId(metadata.getArtifactId());
        amd.setGroupId(metadata.getGroupId());
        amd.setVersion(metadata.getVersion());
        return amd;
    }

    /**
     * @see org.apache.maven.shared.model.DomainModel#getEventHistory()
     */
    public String getEventHistory() {
        return eventHistory;
    }

    /**
     * @see org.apache.maven.shared.model.DomainModel#setEventHistory(String)
     */
    public void setEventHistory(String eventHistory) {
        if (eventHistory == null) {
            throw new IllegalArgumentException("eventHistory: null");
        }
        this.eventHistory = eventHistory;
    }

    public List<ModelProperty> getModelProperties() {
        return new ArrayList<ModelProperty>(modelProperties);
    }

    private static ArtifactBasicMetadata transformContainerToMetadata( ModelContainer container  )
    {
        List<ModelProperty> modelProperties = container.getProperties();

        ArtifactBasicMetadata metadata = new ArtifactBasicMetadata();
        for ( ModelProperty mp : modelProperties )
        {
            if(mp.getUri().equals(ProjectUri.Dependencies.Dependency.groupId)) {
                metadata.setGroupId(mp.getValue());
            } else if(mp.getUri().equals(ProjectUri.Dependencies.Dependency.artifactId)) {
                metadata.setArtifactId(mp.getValue());
            }  else if(mp.getUri().equals(ProjectUri.Dependencies.Dependency.version)) {
                metadata.setVersion(mp.getValue());
            } else if(mp.getUri().equals(ProjectUri.Dependencies.Dependency.classifier)) {
                metadata.setClassifier(mp.getValue());
            }
        }
        return metadata;
    }
}
