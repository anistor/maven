package org.apache.maven.mercury;

import org.apache.maven.mercury.builder.api.MetadataReader;
import org.apache.maven.mercury.builder.api.MetadataReaderException;
import org.apache.maven.mercury.artifact.ArtifactBasicMetadata;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Stack;

public class MetadataReaderStub implements MetadataReader {

    private int x;

    Stack<File> files;

    public MetadataReaderStub(Stack<File> files) {
        this.files = files;
    }

    public byte[] readRawData(ArtifactBasicMetadata artifactBasicMetadata, String s, String s1) throws MetadataReaderException {
        return new byte[0];
    }

    public byte[] readMetadata(ArtifactBasicMetadata artifactBasicMetadata) throws MetadataReaderException {
        File file = files.pop();
        try {
            return IOUtil.toByteArray(new FileInputStream(file));
        } catch (IOException e) {
            throw new MetadataReaderException(e);
        }
    }
}
