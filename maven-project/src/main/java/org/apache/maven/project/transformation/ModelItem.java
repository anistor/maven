package org.apache.maven.project.transformation;

import org.apache.maven.model.Model;
import org.codehaus.plexus.interpolation.reflection.ClassMap;
import org.codehaus.plexus.interpolation.util.StringUtils;

import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.ArrayList;
import java.util.StringTokenizer;

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

/**
 * This class is used to retrieve items from the Model object or its children and to access
 * the corresponding item in the pom.xml.
 *
 * This class has package scope as it is only used by the DefaultModelTransformer.
 */
class ModelItem
{
    final XPathExpression expression;
    final XPathExpression locationExpression;
    final String modelPath;
    final String insertPath;
    private final String xpath;
    enum Location { BEFORE, AFTER }
    final Location location;

    private final List<Method> getMethods;

    private final List<Method> setMethods;   

    private static final Class[] GET_ARG_TYPES = new Class[0];

    private static final Object[] GET_ARGS = new Object[0];

    private static final Map<Class, ClassMap> classMaps = new WeakHashMap<Class, ClassMap>();

    private static XPathFactory factory = XPathFactory.newInstance();

    /**
     * Create a ModelItem.
     * @param xpath The xpath expression to allow traversal from the beginning of the pom to the desired element.
     * @param modelPath dottend notation to allow access to members, relative to the model object.
     * @throws Exception if an error occurs while compiling the xpath or traversing the model path.
     */
    ModelItem( String xpath, String modelPath)
        throws Exception
    {
        this( xpath, modelPath, null, null);
    }

    ModelItem ( String xpath, String modelPath, String insertPath, Location location)
        throws Exception
    {
        this.expression = factory.newXPath().compile( xpath );
        this.xpath = xpath;
        this.modelPath = modelPath;
        this.getMethods = setupGetters( modelPath );
        this.setMethods = setupSetters( modelPath );
        this.insertPath = insertPath;
        this.location = ( insertPath != null && location == null ) ? Location.AFTER : location;
        this.locationExpression = ( insertPath != null ) ? factory.newXPath().compile( insertPath ) : null;
    }

    /**
     * Retrieve the value associated with this item from the model.
     * @param model The model to retrieve the value from.
     * @return The value or null if the value or one of its parents is not present.
     * @throws Exception if an error occurs traversing the model.
     */
    String getValue( Model model )
        throws Exception
    {
        Object value = model;
        for ( Method method : getMethods )
        {
            value = method.invoke( value, GET_ARGS );
            if ( value == null )
            {
                return null;
            }
        }
        return (String) value;
    }

    /**
     * Set the value associated with this item in the model.
     * @param model The model to retrieve the value from.
     * @param value The value to set the element to.
     * @throws Exception if an error occurs traversing the model.
     */
    void setValue( Model model, Object value )
        throws Exception
    {
        Object obj = model;
        int count = setMethods.size();
        for ( Method method : setMethods )
        {
            if (--count > 0)
            {
                obj = method.invoke( obj, GET_ARGS );
            }
            else
            {
                Object[] args = new Object[] { value };
                method.invoke(obj, args);
                return;
            }

            if ( obj == null )
            {
                return;
            }
        }
    }

    public String toString()
    {
        return xpath;
    }

    /**
     * Returns the name of the element that will contain the text.
     * @return The element name associated with the ModelItem's xpath expression.
     */
    public String getTagName()
    {
        return xpath.substring(xpath.lastIndexOf( "/" ) + 1);
    }

    private List<Method> setupGetters( String modelPath )
        throws Exception
    {
        List<Method> list = new ArrayList<Method>();
        Class clazz = Model.class;

        StringTokenizer parser = new StringTokenizer( modelPath, "." );

        while ( parser.hasMoreTokens() )
        {
            String token = parser.nextToken();

            ClassMap classMap = getClassMap( clazz );

            if ( clazz == null )
            {
                return null;
            }

            String methodBase = StringUtils.capitalizeFirstLetter( token );

            String methodName = "get" + methodBase;

            Method method = classMap.findMethod( methodName, GET_ARG_TYPES );

            if ( method == null )
            {
                // perhaps this is a boolean property??
                methodName = "is" + methodBase;

                method = classMap.findMethod( methodName, GET_ARG_TYPES );
            }

            if ( method == null )
            {
                return null;
            }
            list.add( method );
            clazz = method.getReturnType();
        }
        return list;
    }

    private List<Method> setupSetters( String modelPath )
        throws Exception
    {
        List<Method> list = new ArrayList<Method>();
        Class clazz = Model.class;

        StringTokenizer parser = new StringTokenizer( modelPath, "." );
        int count = parser.countTokens();

        while ( parser.hasMoreTokens() )
        {
            String token = parser.nextToken();

            if ( clazz == null )
            {
                throw new IllegalArgumentException("Could not locate class for " + token + " in " + modelPath );
            }

            ClassMap classMap = getClassMap( clazz );

            String methodBase = StringUtils.capitalizeFirstLetter( token );

            String methodName = "get" + methodBase;

            Method method = classMap.findMethod( methodName, GET_ARG_TYPES );

            if ( method == null )
            {
                throw new IllegalArgumentException( "Could not locate method " + methodName +
                    "() in class " + clazz.getName());
            }

            Class argClass = method.getReturnType();

            if (--count == 0)
            {
                methodName = "set" + methodBase;

                Object[] args = new Object[1];
                args[0] = argClass.newInstance();
                method = classMap.findMethod( methodName, args);

                if (method == null)
                {
                    throw new IllegalArgumentException( "Could not locate method " + methodName +
                        "(" + argClass.getName() + ") in class " + clazz.getName() );
                }
            }
            list.add(method);
        }
        return list;
    }

    private static ClassMap getClassMap( Class clazz )
    {
        ClassMap classMap = classMaps.get( clazz );

        if ( classMap == null )
        {
            classMap = new ClassMap( clazz );

            classMaps.put( clazz, classMap );
        }

        return classMap;
    }
}