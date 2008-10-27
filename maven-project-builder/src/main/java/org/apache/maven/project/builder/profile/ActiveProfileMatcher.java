package org.apache.maven.project.builder.profile;

import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.InterpolatorProperty;

import java.util.List;

public interface ActiveProfileMatcher {
    
    boolean isMatch(ModelContainer modelContainer, List<InterpolatorProperty> properties);
}
