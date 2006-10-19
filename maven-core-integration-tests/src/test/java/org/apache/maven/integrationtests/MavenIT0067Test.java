package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0067Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test activation of a profile from the command line. */
public void testit0067() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0067 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0067", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven", "maven-core-it-support", "1.0", "jar");
List cliOptions = new ArrayList();
cliOptions.add("-P test-profile");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("compile");
verifier.assertFilePresent("target/classes/org/apache/maven/it0021/Person.class");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

