package org.apache.maven.mercury.impl;

import org.apache.maven.mercury.artifact.ArtifactBasicMetadata;

public final class ArtifactBasicMetadataImpl extends ArtifactBasicMetadata {

    private String modelSource;

    public String getModelSource() {
        return modelSource;
    }

    public void setModelSource(String modelSource) {
        this.modelSource = modelSource;
    }
}
