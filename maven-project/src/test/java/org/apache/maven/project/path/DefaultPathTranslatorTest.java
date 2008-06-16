package org.apache.maven.project.path;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Resource;

import java.io.File;
import java.util.Collections;

import junit.framework.TestCase;

/**
 * Tests {@link DefaultPathTranslator}.
 *
 * @version $Id$
 */
public class DefaultPathTranslatorTest
    extends TestCase
{

    private PathTranslator pathTranslator;

    private File baseDir;

    public void setUp()
        throws Exception
    {
        super.setUp();

        pathTranslator = new DefaultPathTranslator();
        baseDir = new File( System.getProperty( "java.io.tmpdir" ), "path-translator" ).getAbsoluteFile();
    }

    private String absolute( String path )
    {
        return new File( baseDir, path ).getAbsolutePath();
    }

    private String align( String path )
    {
        return pathTranslator.alignToBaseDirectory( path, baseDir );
    }

    private String unalign( String path )
    {
        return pathTranslator.unalignFromBaseDirectory( path, baseDir );
    }

    public void testAlignToBaseDirectoryNullBaseDir()
    {
        try
        {
            pathTranslator.alignToBaseDirectory( "path", null );
            fail( "path translation did not fail" );
        }
        catch ( Exception e )
        {
            // expected
        }
    }

    public void testAlignToBaseDirectoryNullPath()
    {
        try
        {
            assertNull( align( null ) );
        }
        catch ( Exception e )
        {
            fail( "null path was not accepted" );
        }
    }

    public void testAlignToBaseDirectoryRelativePath()
    {
        String expectedPath = absolute( "target" );
        assertEquals( expectedPath, align( "target" ) );

        expectedPath = baseDir.getAbsolutePath();
        assertEquals( expectedPath, align( "" ) );
    }

    public void testAlignToBaseDirectoryAbsolutePath()
    {
        String expectedPath = new File( "somedir" ).getAbsolutePath();
        assertEquals( expectedPath, align( expectedPath ) );
    }

    public void testAlignToBaseDirectoryAbsolutePathWithBasedirExpression()
    {
        String expectedPath = absolute( "target" );
        assertEquals( expectedPath, align( "${basedir}" + File.separatorChar + "target" ) );
    }

    public void testAlignToBaseDirectoryBasedirExpression()
    {
        String expectedPath = baseDir.getAbsolutePath();
        assertEquals( expectedPath, align( "${basedir}" ) );
    }

    public void testAlignToBaseDirectoryFileSeparatorNormalization()
    {
        String expectedPath = absolute( "target/classes" );
        assertEquals( expectedPath, align( "target/classes" ) );
    }

    public void testAlignToBaseDirectoryNullModel()
    {
        try
        {
            pathTranslator.alignToBaseDirectory( (Model) null, baseDir );
            fail( "path translation did not fail" );
        }
        catch ( Exception e )
        {
            // expected
        }
    }

    public void testAlignToBaseDirectoryModel()
    {
        Resource mainResource = new Resource();
        mainResource.setDirectory( "src/main/resources" );

        Resource testResource = new Resource();
        testResource.setDirectory( "src/test/resources" );

        Build build = new Build();
        build.setDirectory( "target" );
        build.setOutputDirectory( "target/classes" );
        build.setTestOutputDirectory( "target/test-classes" );
        build.setSourceDirectory( "src/main/java" );
        build.setTestSourceDirectory( "src/test/java" );
        build.setScriptSourceDirectory( "src/main/scripts" );
        build.setResources( Collections.singletonList( mainResource ) );
        build.setTestResources( Collections.singletonList( testResource ) );

        Reporting reporting = new Reporting();
        reporting.setOutputDirectory( "target/site" );

        Model model = new Model();
        model.setBuild( build );
        model.setReporting( reporting );

        pathTranslator.alignToBaseDirectory( model, baseDir );

        assertEquals( absolute( "target" ), build.getDirectory() );
        assertEquals( absolute( "target/classes" ), build.getOutputDirectory() );
        assertEquals( absolute( "target/test-classes" ), build.getTestOutputDirectory() );
        assertEquals( absolute( "src/main/java" ), build.getSourceDirectory() );
        assertEquals( absolute( "src/test/java" ), build.getTestSourceDirectory() );
        assertEquals( absolute( "src/main/scripts" ), build.getScriptSourceDirectory() );
        assertEquals( absolute( "src/main/resources" ), ( (Resource) build.getResources().get( 0 ) ).getDirectory() );
        assertEquals( absolute( "src/test/resources" ), ( (Resource) build.getTestResources().get( 0 ) ).getDirectory() );
        assertEquals( absolute( "target/site" ), reporting.getOutputDirectory() );
    }

    public void testUnalignFromBaseDirectoryNullBaseDir()
    {
        try
        {
            pathTranslator.unalignFromBaseDirectory( "path", null );
            fail( "path translation did not fail" );
        }
        catch ( Exception e )
        {
            // expected
        }
    }

    public void testUnalignFromBaseDirectoryNullPath()
    {
        try
        {
            assertNull( unalign( null ) );
        }
        catch ( Exception e )
        {
            fail( "null path was not accepted" );
        }
    }

    public void testUnalignFromBaseDirectoryRelativePath()
    {
        String expectedPath = "target";
        assertEquals( expectedPath, unalign( "target" ) );
    }

    public void testUnalignFromBaseDirectoryAbsolutePath()
    {
        String expectedPath = new File( "somedir" ).getAbsolutePath();
        assertEquals( expectedPath, unalign( expectedPath ) );

        expectedPath = "target";
        assertEquals( expectedPath, unalign( absolute( "target" ) ) );

        expectedPath = "";
        assertEquals( expectedPath, unalign( baseDir.getAbsolutePath() ) );
    }

    public void testUnalignFromBaseDirectoryAbsolutePathWithBasedirExpression()
    {
        String expectedPath = "${basedir}/target";
        assertEquals( expectedPath, unalign( "${basedir}/target" ) );
    }

    public void testUnalignFromBaseDirectoryBasedirExpression()
    {
        String expectedPath = "${basedir}";
        assertEquals( expectedPath, unalign( "${basedir}" ) );
    }

    public void testUnalignFromBaseDirectoryFileSeparatorNormalization()
    {
        String expectedPath = "target/classes";
        assertEquals( expectedPath, unalign( absolute( "target" + File.separator + "classes" ) ) );
    }

    public void testUnalignFromBaseDirectoryNullModel()
    {
        try
        {
            pathTranslator.unalignFromBaseDirectory( (Model) null, baseDir );
            fail( "path translation did not fail" );
        }
        catch ( Exception e )
        {
            // expected
        }
    }

    public void testUnalignFromBaseDirectoryModel()
    {
        Resource mainResource = new Resource();
        mainResource.setDirectory( absolute( "src/main/resources" ) );

        Resource testResource = new Resource();
        testResource.setDirectory( absolute( "src/test/resources" ) );

        Build build = new Build();
        build.setDirectory( absolute( "target" ) );
        build.setOutputDirectory( absolute( "target/classes" ) );
        build.setTestOutputDirectory( absolute( "target/test-classes" ) );
        build.setSourceDirectory( absolute( "src/main/java" ) );
        build.setTestSourceDirectory( absolute( "src/test/java" ) );
        build.setScriptSourceDirectory( absolute( "src/main/scripts" ) );
        build.setResources( Collections.singletonList( mainResource ) );
        build.setTestResources( Collections.singletonList( testResource ) );

        Reporting reporting = new Reporting();
        reporting.setOutputDirectory( absolute( "target/site" ) );

        Model model = new Model();
        model.setBuild( build );
        model.setReporting( reporting );

        pathTranslator.unalignFromBaseDirectory( model, baseDir );

        assertEquals( "target", build.getDirectory() );
        assertEquals( "target/classes", build.getOutputDirectory() );
        assertEquals( "target/test-classes", build.getTestOutputDirectory() );
        assertEquals( "src/main/java", build.getSourceDirectory() );
        assertEquals( "src/test/java", build.getTestSourceDirectory() );
        assertEquals( "src/main/scripts", build.getScriptSourceDirectory() );
        assertEquals( "src/main/resources", ( (Resource) build.getResources().get( 0 ) ).getDirectory() );
        assertEquals( "src/test/resources", ( (Resource) build.getTestResources().get( 0 ) ).getDirectory() );
        assertEquals( "target/site", reporting.getOutputDirectory() );
    }

    public void testAlignUnalign()
    {
        assertNull( unalign( align( null ) ) );

        String expectedPath = "";
        assertEquals( expectedPath, unalign( align( expectedPath ) ) );

        expectedPath = "target";
        assertEquals( expectedPath, unalign( align( expectedPath ) ) );

        expectedPath = "src/main/java";
        assertEquals( expectedPath, unalign( align( expectedPath ) ) );
    }

    public void testUnalignAlign()
    {
        assertNull( align( unalign( null ) ) );

        String expectedPath = baseDir.getAbsolutePath();
        assertEquals( expectedPath, align( unalign( expectedPath ) ) );

        expectedPath = new File( baseDir, "target" ).getAbsolutePath();
        assertEquals( expectedPath, align( unalign( expectedPath ) ) );

        expectedPath = new File( baseDir, "src/main/java" ).getAbsolutePath();
        assertEquals( expectedPath, align( unalign( expectedPath ) ) );
    }

    public void testAlignToBasedirWhereBasedirExpressionIsTheCompleteValue()
    {
        File basedir = new File( System.getProperty( "java.io.tmpdir" ), "test" ).getAbsoluteFile();

        String aligned = new DefaultPathTranslator().alignToBaseDirectory( "${basedir}", basedir );

        assertEquals( basedir.getAbsolutePath(), aligned );
    }

    public void testAlignToBasedirWhereBasedirExpressionIsTheValuePrefix()
    {
        File basedir = new File( System.getProperty( "java.io.tmpdir" ), "test" ).getAbsoluteFile();

        String aligned = new DefaultPathTranslator().alignToBaseDirectory( "${basedir}/dir", basedir );

        assertEquals( new File( basedir, "dir" ).getAbsolutePath(), aligned );
    }

}
