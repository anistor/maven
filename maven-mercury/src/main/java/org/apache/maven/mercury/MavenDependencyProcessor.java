package org.apache.maven.mercury;

import java.io.File;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import org.apache.maven.mercury.artifact.ArtifactBasicMetadata;
import org.apache.maven.mercury.builder.api.DependencyProcessor;
import org.apache.maven.mercury.builder.api.MetadataReader;
import org.apache.maven.mercury.builder.api.MetadataReaderException;
import org.apache.maven.project.builder.ArtifactModelContainerFactory;
import org.apache.maven.project.builder.IdModelContainerFactory;
import org.apache.maven.shared.model.DomainModel;
import org.apache.maven.shared.model.ModelTransformerContext;
import org.apache.maven.shared.model.ModelMarshaller;

public final class MavenDependencyProcessor implements DependencyProcessor {

    public List<ArtifactBasicMetadata> getDependencies(ArtifactBasicMetadata bmd, MetadataReader mdReader, Hashtable env)
            throws MetadataReaderException {
        if (bmd == null) {
            throw new IllegalArgumentException("bmd: null");
        }

        if (mdReader == null) {
            throw new IllegalArgumentException("mdReader: null");
        }

        //TODO: Add super model
        List<DomainModel> domainModels = new ArrayList<DomainModel>();
        try {
            MavenDomainModel domainModel = new MavenDomainModel(mdReader.readMetadata(bmd));
            domainModels.add(domainModel);
            domainModels.addAll(getParentsOfDomainModel(domainModel, mdReader));
        } catch (IOException e) {
            throw new MetadataReaderException("Failed to create domain model. Message = " + e.getMessage());
        }

        MercuryPomTransformer transformer = new MercuryPomTransformer();
        ModelTransformerContext ctx = new ModelTransformerContext(
                Arrays.asList(new ArtifactModelContainerFactory(), new IdModelContainerFactory()));

        try {
            return ((MavenDomainModel) ctx.transform(domainModels,
                    transformer,
                    transformer,
                    null,
                    null)).getDependencyMetadata();
        } catch (IOException e) {
            throw new MetadataReaderException("Unable to transform model");
        }
    }

    private static List<DomainModel> getParentsOfDomainModel(MavenDomainModel domainModel, MetadataReader mdReader)
            throws IOException, MetadataReaderException {
        List<DomainModel> domainModels = new ArrayList<DomainModel>();
        if (domainModel.hasParent()) {
            MavenDomainModel parentDomainModel = new MavenDomainModel(mdReader.readMetadata(domainModel.getParentMetadata()));
            domainModels.add(parentDomainModel);
            domainModels.addAll(getParentsOfDomainModel(parentDomainModel, mdReader));
        }
        return domainModels;
    }    
}
