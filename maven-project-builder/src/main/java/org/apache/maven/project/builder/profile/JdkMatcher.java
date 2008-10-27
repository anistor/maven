package org.apache.maven.project.builder.profile;

import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.InterpolatorProperty;
import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.project.builder.ProjectUri;

import java.util.List;

public class JdkMatcher implements ActiveProfileMatcher {

    public boolean isMatch(ModelContainer modelContainer, List<InterpolatorProperty> properties) {
        if(modelContainer == null ) {
            throw new IllegalArgumentException("modelContainer: null");
        }

        for(InterpolatorProperty property : properties) {
            if(property.getKey().equals("${java.specification.version}")) {
                String version = property.getValue();
                for(ModelProperty modelProperty : modelContainer.getProperties()) {
                    if(modelProperty.getUri().equals(ProjectUri.Profiles.Profile.Activation.jdk)) {
                        return version.equals(modelProperty.getValue());
                    }
                }
                return false;   
            }
        }
        return false;
    }
}

