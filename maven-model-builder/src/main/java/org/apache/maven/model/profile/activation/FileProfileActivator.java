package org.apache.maven.model.profile.activation;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationFile;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.path.PathTranslator;
import org.apache.maven.model.profile.ProfileActivationContext;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.interpolation.AbstractValueSource;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.util.StringUtils;

/**
 * Determines profile activation based on the existence/absence of some file.
 * 
 * @author Benjamin Bentmann
 */
@Component( role = ProfileActivator.class, hint = "file" )
public class FileProfileActivator
    implements ProfileActivator
{

    @Requirement
    private PathTranslator pathTranslator;

    public boolean isActive( Profile profile, ProfileActivationContext context, ModelProblemCollector problems )
    {
        Activation activation = profile.getActivation();

        if ( activation == null )
        {
            return false;
        }

        ActivationFile file = activation.getFile();

        if ( file == null )
        {
            return false;
        }

        String path;
        boolean missing;

        if ( StringUtils.isNotEmpty( file.getExists() ) )
        {
            path = file.getExists();
            missing = false;
        }
        else if ( StringUtils.isNotEmpty( file.getMissing() ) )
        {
            path = file.getMissing();
            missing = true;
        }
        else
        {
            return false;
        }

        RegexBasedInterpolator interpolator = new RegexBasedInterpolator();

        final File basedir = context.getProjectDirectory();

        if ( basedir != null )
        {
            interpolator.addValueSource( new AbstractValueSource( false )
            {
                public Object getValue( String expression )
                {
                    /*
                     * NOTE: We intentionally only support ${basedir} and not ${project.basedir} as the latter form
                     * would suggest that other project.* expressions can be used which is however beyond the design.
                     */
                    if ( "basedir".equals( expression ) )
                    {
                        return basedir.getAbsolutePath();
                    }
                    return null;
                }
            } );
        }
        else if ( path.indexOf( "${basedir}" ) >= 0 )
        {
            return false;
        }

        interpolator.addValueSource( new MapBasedValueSource( context.getUserProperties() ) );

        interpolator.addValueSource( new MapBasedValueSource( context.getSystemProperties() ) );

        try
        {
            path = interpolator.interpolate( path, "" );
        }
        catch ( Exception e )
        {
            problems.addError( "Failed to interpolate file location " + path + " for profile " + profile.getId() + ": "
                + e.getMessage(), e );
            return false;
        }

        path = pathTranslator.alignToBaseDirectory( path, basedir );

        File f = new File( path );

        if ( !f.isAbsolute() )
        {
            return false;
        }

        boolean fileExists = f.exists();

        return missing ? !fileExists : fileExists;
    }

}
