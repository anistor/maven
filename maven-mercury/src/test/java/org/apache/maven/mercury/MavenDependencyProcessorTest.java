package org.apache.maven.mercury;
import static junit.framework.Assert.*;
import org.apache.maven.mercury.artifact.ArtifactBasicMetadata;
import org.apache.maven.mercury.builder.api.MetadataReaderException;
import org.junit.Before;

import java.util.List;
import java.util.Stack;
import java.io.File;

public class MavenDependencyProcessorTest {

   // private static File resources = new File(System.getProperty( "basedir" ), "resources");

    private String basedir;

    @Before
    public void setUp()
        throws Exception
    {
        basedir = System.getProperty( "basedir" );

        if ( basedir == null )
        {
            basedir = new File( "maven-mercury" ).getCanonicalPath();
        }
    }

    @org.junit.Test
    public void testSinglePom() throws MetadataReaderException {
        Stack<File> files = new Stack<File>();
        files.add(new File(basedir, "src/test/resources/simple.xml"));

        MavenDependencyProcessor processor = new MavenDependencyProcessor();
        List<ArtifactBasicMetadata> dependencies = processor.getDependencies(new ArtifactBasicMetadata(),
                new MetadataReaderStub(files), null, null);
        assertEquals("Dependencies incorrect", 2, dependencies.size());

    }

    @org.junit.Test
    public void testSingleParentPom() throws MetadataReaderException {
        Stack<File> files = new Stack<File>();
        files.add(new File(basedir, "src/test/resources/parent.xml"));
        files.add(new File(basedir, "src/test/resources/child.xml"));

        MavenDependencyProcessor processor = new MavenDependencyProcessor();
        List<ArtifactBasicMetadata> dependencies = processor.getDependencies(new ArtifactBasicMetadata(),
                new MetadataReaderStub(files), null, null);
        assertEquals("Dependencies incorrect", 2, dependencies.size());
    }

    @org.junit.Test
    public void testSingleParentPomWithDependencyManagement() throws MetadataReaderException {
        Stack<File> files = new Stack<File>();
        files.add(new File(basedir, "src/test/resources/parent.xml"));
        files.add(new File(basedir, "src/test/resources/child-withDepMng.xml"));

        MavenDependencyProcessor processor = new MavenDependencyProcessor();
        List<ArtifactBasicMetadata> dependencies = processor.getDependencies(new ArtifactBasicMetadata(),
                new MetadataReaderStub(files), null, null);
        assertEquals("Dependencies incorrect", 2, dependencies.size());
        assertTrue("Dependency Management", hasDependency(dependencies, "c-v-1"));
    }

    private static boolean hasDependency(List<ArtifactBasicMetadata> metadatas, String version) {
        for(ArtifactBasicMetadata metadata : metadatas ) {
            if(metadata.getVersion().equals(version)) {
                return true;
            }
        }
        return false;
    }
}
