package org.apache.maven.artifact.installer;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactComponentTestCase;

import java.io.File;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class ArtifactInstallerTest
    extends ArtifactComponentTestCase
{
    private ArtifactInstaller artifactInstaller;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        artifactInstaller = (ArtifactInstaller) lookup( ArtifactInstaller.ROLE );
    }

    protected String component()
    {
        return "installer";
    }

    public void testArtifactInstallation()
        throws Exception
    {
        String artifactBasedir = new File( getBasedir(), "src/test/resources/artifact-install" ).getAbsolutePath();

        Artifact artifact = createArtifact( "artifact", "1.0" );

        artifactInstaller.install( artifactBasedir, artifact, localRepository() );

        assertLocalArtifactPresent( artifact );
    }
}