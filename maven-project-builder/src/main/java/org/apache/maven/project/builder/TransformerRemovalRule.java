package org.apache.maven.project.builder;

import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.shared.model.DataSourceException;

import java.util.List;

public interface TransformerRemovalRule {

        List<ModelProperty> executeWithReturnPropertiesToRemove(List<ModelProperty> modelProperties, int domainIndex)
            throws DataSourceException;
}
