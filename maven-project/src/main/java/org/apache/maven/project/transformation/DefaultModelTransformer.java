package org.apache.maven.project.transformation;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathConstants;

import org.xml.sax.SAXException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.model.Model;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.CollectionUtils;
import org.codehaus.plexus.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.w3c.dom.ls.LSOutput;

/**
 * This class itentifies the fields in the Model that must have their variables resolved before the pom can
 * be stored into a repository. It provides the utility methods to replace the variables and create the new
 * project file.
 */
public final class DefaultModelTransformer
    extends AbstractLogEnabled
    implements ModelTransformer
{
    /**
     * The document builder
     */
    private final DocumentBuilder builder;

    private final ModelItem[] items;


    public DefaultModelTransformer() throws Exception
    {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setValidating( false );
        builderFactory.setExpandEntityReferences( false );
        builderFactory.setNamespaceAware( false );
        builderFactory.setAttribute( "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                                      Boolean.FALSE );
        builder = builderFactory.newDocumentBuilder();

        items = new ModelItem[] {
            new ModelItem("/project/parent/version", "parent.version", "/project/parent/artifactId",
                          ModelItem.Location.AFTER),
            new ModelItem("/project/groupId", "groupId", "/project/artifactId", ModelItem.Location.BEFORE),
            new ModelItem("/project/artifactId", "artifactId"),
            new ModelItem("/project/version", "version", "project/artifactId", ModelItem.Location.AFTER)
        };
    }

    /**
     * Transform the model into a form acceptable to be stored into a repository.
     * @param source The source pom file.
     * @param target The target pom file.
     * @param project The project created from the source pom file.
     * @param projectId The current projectId for use in Exception messages.
     * @param debug true if debug should be enabled.
     * @return true if a new pom was written to the target directory.
     * @throws ModelTransformationException If an error occurs processing the source file or creating the target.
     */
    public boolean transformModel( File source,
                                   File target,
                                   MavenProject project,
                                   String projectId,
                                   boolean debug )
        throws ModelTransformationException
    {
        Logger logger = getLogger();
        Document document;
        Model model = project.getModel();
        Model originalModel = project.getOriginalModel();
        try
        {
            document = builder.parse(source);
        }
        catch ( IOException ioe )
        {
            throw new ModelTransformationException( "Parse failed in " + projectId + " for " +
                                                     source.getAbsolutePath(), ioe );
        }
        catch ( SAXException ioe )
        {
            throw new ModelTransformationException( "Parse failed in " + projectId + " for " +
                                                    source.getAbsolutePath(), ioe );
        }

        boolean replaced = false;

        for ( ModelItem item : items )
        {
            // getValue returns null if the value in the original and current model are the same.
            String value = getValue(item, model, originalModel);
            if (value != null)
            {
                try
                {
                    Node node = (Node)item.expression.evaluate( document, XPathConstants.NODE );
                    if (node != null)
                    {
                        node.setTextContent( value );
                    }
                    else if (item.location != null)
                    {
                        Node newNode = document.createElement( item.getTagName() );
                        newNode.setTextContent( value );
                        node = (Node)item.locationExpression.evaluate( document, XPathConstants.NODE );
                        if (node != null)
                        {
                            Node previous = node.getPreviousSibling();
                            Node parentNode = node.getParentNode();
                            if (item.location == ModelItem.Location.AFTER)
                            {
                                Node sibling = node.getNextSibling();
                                if (sibling == null)
                                {
                                    if ( previous != null && previous.getNodeType() == Node.TEXT_NODE )
                                    {
                                        Node text = previous.cloneNode( false );
                                        parentNode.appendChild( text );
                                    }
                                    parentNode.appendChild( newNode );
                                }
                                else
                                {
                                    if ( previous != null && previous.getNodeType() == Node.TEXT_NODE )
                                    {
                                        Node text = previous.cloneNode( false );
                                        parentNode.insertBefore( text, sibling );
                                    }
                                    parentNode.insertBefore( newNode, sibling );
                                }
                            }
                            else
                            {
                                parentNode.insertBefore( newNode, node );
                                Node sibling = node.getNextSibling();
                                if ( sibling != null && sibling.getNodeType() == Node.TEXT_NODE )
                                {
                                    Node text = sibling.cloneNode( false );
                                    parentNode.insertBefore( text, node );
                                }
                            }
                        }
                    }
                    replaced = true;
                }
                catch (Error err)
                {
                    throw new ModelTransformationException ("Unable to insert" + item, err);
                }
                catch (XPathExpressionException xpee)
                {
                    throw new ModelTransformationException( "Failed to evaluate " + item, xpee);
                }
            }
        }
        if (!replaced)
        {
            return replaced;
        }

        try
        {
            logger.debug("Writing project to " + target.getAbsolutePath());
            target.getParentFile().mkdirs();
            DOMImplementation domImpl = document.getImplementation();

            DOMImplementationLS domImplLS = (DOMImplementationLS) domImpl.getFeature("LS", "3.0");

            LSSerializer serializer = domImplLS.createLSSerializer();

            LSOutput serializerOut = domImplLS.createLSOutput();
            serializerOut.setByteStream(new FileOutputStream(target));

            serializer.write(document.getDocumentElement(), serializerOut);
            return true;
        }
        catch ( FileNotFoundException e )
        {
            throw new ModelTransformationException("FileNotFoundException: " + e );
        }
    }

    /**
     * Replace the variables in the model items.
     * @param model The target model.
     * @param projectId The project Id for exception messages.
     * @param config The current ProjectBuilderConfiguration.
     * @throws ProjectBuildingException if an exception occurs interpolating the variables.
     */
    public void resolveModel(Model model, String projectId, ProjectBuilderConfiguration config )
        throws ProjectBuildingException
    {        
        Map[] propMaps = new Map[] { config.getExecutionProperties(),
                                     model.getProperties(),
                                     config.getUserProperties() };

        Map<String, String> props = CollectionUtils.mergeMaps( propMaps );

        for (ModelItem item : items )
        {
            String value;
            try
            {
                String src = item.getValue( model );
                if (src != null)
                {
                    value = StringUtils.interpolate( src, props );
                    if (value == null || src.equals(value))
                    {
                        continue;
                    }
                }
                else
                {
                    continue;
                }
            }
            catch ( Exception ex )
            {
                throw new ProjectBuildingException( projectId, "Unable to resolve " + item.modelPath, ex);
            }
            try
            {
                item.setValue( model, value );
            }
            catch ( Exception ex)
            {
                throw new ProjectBuildingException( projectId, "Unable to set value for " + item.modelPath, ex);
            }
        }
    }

    private String getValue(ModelItem item,
                            Model model,
                            Model originalModel)
    {
        try
        {
            String originalValue = item.getValue( originalModel );

            String value = item.getValue( model );

            if (value.equals(originalValue))
            {
                return null;
            }
            return value;
        }
        catch (Exception ex)
        {
            return null;
        }
    }
}