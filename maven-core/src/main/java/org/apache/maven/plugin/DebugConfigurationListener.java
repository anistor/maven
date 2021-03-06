package org.apache.maven.plugin;

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

import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.logging.Logger;

/**
 * Log at debug level the mojo configuration.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class DebugConfigurationListener
    implements ConfigurationListener
{
    private Logger logger;

    public DebugConfigurationListener( Logger logger )
    {
        this.logger = logger;
    }

    public void notifyFieldChangeUsingSetter( String fieldName, Object value, Object target )
    {
        if ( logger.isDebugEnabled() )
        {
            logger.debug( "  (s) " + fieldName + " = " + value );
        }
    }

    public void notifyFieldChangeUsingReflection( String fieldName, Object value, Object target )
    {
        if ( logger.isDebugEnabled() )
        {
            logger.debug( "  (f) " + fieldName + " = " + value );
        }
    }
}
