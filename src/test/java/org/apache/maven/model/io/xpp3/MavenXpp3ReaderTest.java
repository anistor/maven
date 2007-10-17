package org.apache.maven.model.io.xpp3;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;

import org.apache.maven.model.Model;

public class MavenXpp3ReaderTest extends AbstractMavenModelTestCase {

	/**
	 * This test case will not fail, because 8bit encodings won't corrupt the parser.
	 * @throws Exception
	 */
	public void testISOEncodedModel() throws Exception {
		File f = getFileForClasspathResource( "iso-8859-15-encoded-pom.xml" );
		MavenXpp3Reader reader = new MavenXpp3Reader();
		
		Model model = reader.read( new InputStreamReader( new FileInputStream( f ), "ISO-8859-15" ) );
		assertEquals( "цдья", model.getDescription() );
		
		model = reader.read( new FileReader( f ) );
		assertEquals( "цдья", model.getDescription() );
	}

	/**
	 * This test case WILL FAIL, because FileReader-instances cannot cope with UTF-8 encoded files.
	 * Maven uses FileReaders by default to read POMs. 
	 * @throws Exception
	 */
	public void testUTFEncodedModel() throws Exception {
		File f = getFileForClasspathResource( "utf-8-encoded-pom.xml" );
		MavenXpp3Reader reader = new MavenXpp3Reader();
		
		// Here's how to read the POM the right way, by telling the InputStream to use a certain encoding.
		Model model = reader.read( new InputStreamReader( new FileInputStream( f ), "UTF-8" ) );
		assertEquals( "цдья", model.getDescription() );
		
		// Here's how Maven reads POMs: THIS TEST WILL FAIL
		model = reader.read( new FileReader( f ) );
		assertEquals( "цдья", model.getDescription() );
	}
}
