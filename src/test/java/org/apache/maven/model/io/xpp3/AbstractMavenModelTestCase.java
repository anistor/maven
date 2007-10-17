package org.apache.maven.model.io.xpp3;


/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

import junit.framework.TestCase;

public abstract class AbstractMavenModelTestCase extends TestCase
{
    protected File getFileForClasspathResource( String resource )
        throws FileNotFoundException
    {
        ClassLoader cloader = Thread.currentThread().getContextClassLoader();

        URL resourceUrl = cloader.getResource( resource );

        File resourceFile = null;
        if ( resourceUrl != null )
        {
            resourceFile = new File( resourceUrl.getPath() );
        }
        else
        {
            throw new FileNotFoundException( "Unable to find: " + resource );
        }

        return resourceFile;
    }
}
