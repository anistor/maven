package org.apache.maven.project.artifact;

import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.InterpolationPostProcessor;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.RecursionInterceptor;
import org.codehaus.plexus.interpolation.SimpleRecursionInterceptor;
import org.codehaus.plexus.interpolation.ValueSource;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.XmlStreamWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class XPathVersionExpressionInterpolator
    implements Interpolator
{
    private static final List VERSION_INTERPOLATION_TARGET_XPATHS;

    static
    {
        List targets = new ArrayList();

        targets.add( "/project/parent/version/text()" );
        targets.add( "/project/version/text()" );

        targets.add( "/project/dependencies/dependency/version/text()" );
        targets.add( "/project/dependencyManagement/dependencies/dependency/version/text()" );

        targets.add( "/project/build/plugins/plugin/version/text()" );
        targets.add( "/project/build/pluginManagement/plugins/plugin/version/text()" );
        targets.add( "/project/build/plugins/plugin/dependencies/dependency/version/text()" );
        targets.add( "/project/build/pluginManagement/plugins/plugin/dependencies/dependency/version/text()" );

        targets.add( "/project/reporting/plugins/plugin/version/text()" );

        targets.add( "/project/profiles/profile/dependencies/dependency/version/text()" );
        targets.add( "/project/profiles/profile/dependencyManagement/dependencies/dependency/version/text()" );

        targets.add( "/project/profiles/profile/build/plugins/plugin/version/text()" );
        targets.add( "/project/profiles/profile/build/pluginManagement/plugins/plugin/version/text()" );
        targets.add( "/project/profiles/profile/build/plugins/plugin/dependencies/dependency/version/text()" );
        targets.add( "/project/profiles/profile/build/pluginManagement/plugins/plugin/dependencies/dependency/version/text()" );

        targets.add( "/project/profiles/profile/reporting/plugins/plugin/version/text()" );

        targets = Collections.unmodifiableList( targets );

        VERSION_INTERPOLATION_TARGET_XPATHS = targets;
    }

    private List postProcessors = new ArrayList();

    private List valueSources = new ArrayList();

    private Map answers = new HashMap();

    private List feedback = new ArrayList();

    private final Logger logger;

    private String encoding;

    public XPathVersionExpressionInterpolator( Logger logger )
    {
        this.logger = logger;
    }

    public void setEncoding( String encoding )
    {
        this.encoding = encoding;
    }

    public String interpolate( String input, RecursionInterceptor recursionInterceptor )
        throws InterpolationException
    {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        TransformerFactory txFactory = TransformerFactory.newInstance();
        XPathFactory xpFactory = XPathFactory.newInstance();

        DocumentBuilder builder;
        Transformer transformer;
        XPath xpath;
        try
        {
            builder = dbFactory.newDocumentBuilder();
            transformer = txFactory.newTransformer();
            xpath = xpFactory.newXPath();
        }
        catch ( ParserConfigurationException e )
        {
            throw new InterpolationException( "Failed to construct XML DocumentBuilder: " + e.getMessage(), "-NONE-", e );
        }
        catch ( TransformerConfigurationException e )
        {
            throw new InterpolationException( "Failed to construct XML Transformer: " + e.getMessage(), "-NONE-", e );
        }

        Document document;
        try
        {
            document = builder.parse( new InputSource( new StringReader( input ) ) );
        }
        catch ( SAXException e )
        {
            throw new InterpolationException( "Failed to parse XML: " + e.getMessage(), "-NONE-", e );
        }
        catch ( IOException e )
        {
            throw new InterpolationException( "Failed to parse XML: " + e.getMessage(), "-NONE-", e );
        }

        inteprolateInternal( document, xpath );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlStreamWriter writer;
        try
        {
            writer = WriterFactory.newXmlWriter( baos );
        }
        catch ( IOException e )
        {
            throw new InterpolationException( "Failed to get XML writer: " + e.getMessage(), "-NONE-", e );
        }

        StreamResult r = new StreamResult( writer );
        DOMSource s = new DOMSource( document );

        try
        {
            if ( encoding != null )
            {
                logger.info( "Writing transformed POM using encoding: " + encoding );
                transformer.setOutputProperty( OutputKeys.ENCODING, encoding );
            }
            else
            {
                logger.info( "Writing transformed POM using default encoding" );
            }

            transformer.transform( s, r );
        }
        catch ( TransformerException e )
        {
            throw new InterpolationException( "Failed to render interpolated XML: " + e.getMessage(), "-NONE-", e );
        }

        try
        {
            return baos.toString( writer.getEncoding() );
        }
        catch ( UnsupportedEncodingException e )
        {
            throw new InterpolationException( "Failed to render interpolated XML: " + e.getMessage(), "-NONE-", e );
        }
    }

    private void inteprolateInternal( Document document, XPath xp )
        throws InterpolationException
    {
        for ( Iterator it = VERSION_INTERPOLATION_TARGET_XPATHS.iterator(); it.hasNext(); )
        {
            String expr = (String) it.next();
            NodeList nodes;
            try
            {
                XPathExpression xpath = xp.compile( expr );
                nodes = (NodeList) xpath.evaluate( document, XPathConstants.NODESET );
            }
            catch ( XPathExpressionException e )
            {
                throw new InterpolationException(
                                                  "Failed to evaluate XPath: " + expr + " (" + e.getMessage() + ")",
                                                  "-NONE-", e );
            }

            if ( nodes != null )
            {
                for ( int idx = 0; idx < nodes.getLength(); idx++ )
                {
                    Node node = nodes.item( idx );
                    Object value = node.getNodeValue();
                    if ( value == null )
                    {
                        continue;
                    }

                    for ( Iterator vsIt = valueSources.iterator(); vsIt.hasNext(); )
                    {
                        ValueSource vs = (ValueSource) vsIt.next();
                        if ( vs != null )
                        {
                            value = vs.getValue( value.toString() );
                            if ( value != null && !value.equals( node.getNodeValue() ) )
                            {
                                break;
                            }
                            else if ( value == null )
                            {
                                value = node.getNodeValue();
                            }
                        }
                    }

                    if ( value != null && !value.equals( node.getNodeValue() ) )
                    {
                        for ( Iterator ppIt = postProcessors.iterator(); ppIt.hasNext(); )
                        {
                            InterpolationPostProcessor postProcessor = (InterpolationPostProcessor) ppIt.next();
                            if ( postProcessor != null )
                            {
                                value = postProcessor.execute( node.getNodeValue(), value );
                            }
                        }

                        node.setNodeValue( String.valueOf( value ) );
                    }
                }
            }
        }
    }

    public void addPostProcessor( InterpolationPostProcessor postProcessor )
    {
        postProcessors.add( postProcessor );
    }

    public void addValueSource( ValueSource valueSource )
    {
        valueSources.add( valueSource );
    }

    public void clearAnswers()
    {
        answers.clear();
    }

    public void clearFeedback()
    {
        feedback.clear();
    }

    public List getFeedback()
    {
        return feedback;
    }

    public String interpolate( String input )
        throws InterpolationException
    {
        return interpolate( input, new SimpleRecursionInterceptor() );
    }

    public String interpolate( String input, String thisPrefixPattern )
        throws InterpolationException
    {
        return interpolate( input, new SimpleRecursionInterceptor() );
    }

    public String interpolate( String input, String thisPrefixPattern, RecursionInterceptor recursionInterceptor )
        throws InterpolationException
    {
        return interpolate( input, recursionInterceptor );
    }

    public boolean isCacheAnswers()
    {
        return true;
    }

    public void removePostProcessor( InterpolationPostProcessor postProcessor )
    {
        postProcessors.remove( postProcessor );
    }

    public void removeValuesSource( ValueSource valueSource )
    {
        valueSources.remove( valueSource );
    }

    public void setCacheAnswers( boolean cacheAnswers )
    {
    }
}
