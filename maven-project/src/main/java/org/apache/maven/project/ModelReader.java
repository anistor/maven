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

import org.apache.maven.model.Model;
import org.apache.maven.model.io.stax.MavenStaxReader;
import org.apache.maven.model.io.stax.MavenStaxReaderDelegate;
import org.codehaus.plexus.util.IOUtil;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.StringReader;
import java.io.FilterReader;

/**
 * Read a model, selecting the correct version based on the input.
 *
 * @todo make it a component
 */
public class ModelReader
{
    /**
     * The StAX reader delegate to use to read it.
     */
    private MavenStaxReaderDelegate modelReader;

    public ModelReader()
    {
        modelReader = new MavenStaxReaderDelegate();
    }

    public Model readModel( String modelSource, boolean strict )
        throws IOException, XMLStreamException
    {
        // TODO: should be able to use the delegate's reader but it has two limitations:
        //  1) it only takes files, not readers, for input
        //  2) it will return v4_1_0 for v4.1.0 which will require an arbitrary conversion instead of the short cut
        //     reading straight into the right model

//        return modelReader.read( reader, strict );

        StringReader sReader = new StringReader( modelSource );

        String modelVersion;
        try
        {
            modelVersion = modelReader.determineVersion( sReader );
        }
        finally
        {
            IOUtil.close( sReader );
        }

        sReader = new StringReader( modelSource );

        Model result;
        try
        {
            if ( "4.0.0".equals( modelVersion ) )
            {
                // TODO: hack - handle common entity we shouldn't really be using
                modelSource = modelSource.replaceAll( "&oslash;", "\u00f8" );
                sReader = new StringReader( modelSource );

                org.apache.maven.model.v4_0_0.Model model40 =
                    new org.apache.maven.model.v4_0_0.io.stax.MavenStaxReader().read( sReader, strict );

                org.apache.maven.model.v4_1_0.Model model41 =
                    new org.apache.maven.model.v4_0_0.convert.BasicVersionConverter().convertModel( model40 );

                result = new org.apache.maven.model.v4_1_0.convert.BasicVersionConverter().convertModel( model41 );
                result.setOriginalModelVersion( "4.0.0" );
            }
            else if ( "4.1.0".equals( modelVersion ) )
            {
                result = new MavenStaxReader().read( sReader, strict );
            }
            else
            {
                throw new XMLStreamException( "Document version '" + modelVersion + "' has no corresponding reader." );
            }
        }
        finally
        {
            IOUtil.close( sReader );
        }
        return result;
    }
}
