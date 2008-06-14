package org.apache.maven.project;

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
import org.apache.maven.model.Resource;

import java.io.File;

public class ProjectBaseDirectoryAlignmentTest
    extends AbstractMavenProjectTestCase
{
    private String dir = "src/test/resources/projects/base-directory-alignment/";

    public void testProjectDirectoryBaseDirectoryAlignment()
        throws Exception
    {
        File f = getTestFile( dir + "project-which-needs-directory-alignment.xml" );

        MavenProject project = getProject( f );

        assertNotNull( "Test project can't be null!", project );

        String basedir = new File( getBasedir() ).getAbsolutePath();

        String sourceDirectory = project.getBuild().getSourceDirectory();
        String testSourceDirectory = project.getBuild().getTestSourceDirectory();
        String scriptSourceDirectory = project.getBuild().getScriptSourceDirectory();
        String buildDirectory = project.getBuild().getDirectory();
        String outputDirectory = project.getBuild().getOutputDirectory();
        String testOutputDirectory = project.getBuild().getTestOutputDirectory();
        String reportingOutputDirectory = project.getReporting().getOutputDirectory();

        assertEquals( new File( project.getBasedir(), "my-file.xml").getAbsolutePath(), project.getProperties().getProperty( "myFile" ) );

        assertTrue( sourceDirectory.startsWith( basedir ) );
        assertTrue( testSourceDirectory.startsWith( basedir ) );
        assertTrue( scriptSourceDirectory.startsWith( basedir ) );
        assertTrue( buildDirectory.startsWith( basedir ) );
        assertTrue( outputDirectory.startsWith( basedir ) );
        assertTrue( testOutputDirectory.startsWith( basedir ) );
        assertTrue( reportingOutputDirectory.startsWith( basedir ) );

        Build build = project.getBuild();

        Resource resource = (Resource) build.getResources().get( 0 );

        assertTrue( resource.getDirectory().startsWith( getBasedir() ) );
    }
}
