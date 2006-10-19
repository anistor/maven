package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0072Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Verifies that property references with dotted notation work within
        POM interpolation. */
public void testit0072() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0072 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0072", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("package");
verifier.assertFilePresent("target/maven-core-it0072-1.0-SNAPSHOT.jar");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

