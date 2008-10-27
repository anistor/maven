package org.apache.maven.project.builder.profile;

import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.InterpolatorProperty;
import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.project.builder.ProjectUri;

import java.util.List;

public class OperatingSystemMatcher implements ActiveProfileMatcher {

    public boolean isMatch(ModelContainer modelContainer, List<InterpolatorProperty> properties) {
        if(modelContainer == null ) {
            throw new IllegalArgumentException("modelContainer: null");
        }

        String operatingSystem = null, version = null, arch = null, family = null;

        for(InterpolatorProperty property : properties) {
            if(property.getKey().equals("${os.name}")) {
                operatingSystem = property.getValue();
            } else if(property.getKey().equals("${os.version}")) {
               version = property.getValue();
            } else if(property.getKey().equals("${os.arch}")) {
                arch = property.getValue();
            } else if(property.getKey().equals("${os.family}")) {
                family = property.getValue();
            }
        }

        boolean matchesOs, matchesVersion, matchesArch, matchesFamily;

        
        for(ModelProperty property : modelContainer.getProperties()) {
            if(arch !=null && property.getUri().equals(ProjectUri.Profiles.Profile.Activation.Os.arch)) {
                
            }
        }

        return false;
    }
}
