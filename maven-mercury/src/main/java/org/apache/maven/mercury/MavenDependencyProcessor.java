package org.apache.maven.mercury;

import java.io.IOException;
import java.util.*;

import org.apache.maven.mercury.artifact.ArtifactBasicMetadata;
import org.apache.maven.mercury.builder.api.DependencyProcessor;
import org.apache.maven.mercury.builder.api.MetadataReader;
import org.apache.maven.mercury.builder.api.MetadataReaderException;
import org.apache.maven.project.builder.ArtifactModelContainerFactory;
import org.apache.maven.project.builder.IdModelContainerFactory;
import org.apache.maven.project.builder.ProjectUri;
import org.apache.maven.project.builder.PomInterpolatorTag;
import org.apache.maven.shared.model.*;

public final class MavenDependencyProcessor implements DependencyProcessor {

    public MavenDependencyProcessor() {

    }

    public List<ArtifactBasicMetadata> getDependencies(ArtifactBasicMetadata bmd, MetadataReader mdReader, Map system, Map user)
            throws MetadataReaderException {
        if (bmd == null) {
            throw new IllegalArgumentException("bmd: null");
        }

        if (mdReader == null) {
            throw new IllegalArgumentException("mdReader: null");
        }

        List<InterpolatorProperty> interpolatorProperties = new ArrayList<InterpolatorProperty>();
        if(system != null) {
            interpolatorProperties.addAll( InterpolatorProperty.toInterpolatorProperties( system,
                    PomInterpolatorTag.SYSTEM_PROPERTIES.name()));
        }
        if(user != null) {
            interpolatorProperties.addAll( InterpolatorProperty.toInterpolatorProperties( user,
                    PomInterpolatorTag.USER_PROPERTIES.name()));
        }
        
        List<DomainModel> domainModels = new ArrayList<DomainModel>();
        try {
//            MavenDomainModel superPom =
//                    new MavenDomainModel(MavenDependencyProcessor.class.getResourceAsStream( "pom-4.0.0.xml" ));
//            domainModels.add(superPom);

            MavenDomainModel domainModel = new MavenDomainModel(mdReader.readMetadata(bmd));
            domainModels.add(domainModel);

            Collection<ModelContainer> activeProfiles = domainModel.getActiveProfileContainers(interpolatorProperties);

            for(ModelContainer mc : activeProfiles) {
                domainModels.add(new MavenDomainModel(transformProfiles(mc.getProperties())));
            }


            domainModels.addAll(getParentsOfDomainModel(domainModel, mdReader));
        } catch (IOException e) {
            throw new MetadataReaderException("Failed to create domain model. Message = " + e.getMessage());
        }

        MercuryPomTransformer transformer = new MercuryPomTransformer();
        ModelTransformerContext ctx = new ModelTransformerContext(
                Arrays.asList(new ArtifactModelContainerFactory(), new IdModelContainerFactory()));

        try {
            MavenDomainModel model = ((MavenDomainModel) ctx.transform(domainModels,
                    transformer,
                    transformer,
                    null,
                    interpolatorProperties));
            return model.getDependencyMetadata();
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

    private static List<ModelProperty> transformProfiles(List<ModelProperty> modelProperties) {
        List<ModelProperty> properties = new ArrayList<ModelProperty>();
        for(ModelProperty mp : modelProperties) {
            if(mp.getUri().startsWith(ProjectUri.Profiles.Profile.xUri)
                    && !mp.getUri().equals(ProjectUri.Profiles.Profile.id)
                    && !mp.getUri().startsWith(ProjectUri.Profiles.Profile.Activation.xUri)) {
                properties.add(new ModelProperty(mp.getUri().replace( ProjectUri.Profiles.Profile.xUri, ProjectUri.xUri ),
                    mp.getResolvedValue() ));
            }
        }
        return properties;
    }
}
