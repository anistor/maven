package org.apache.maven.plugin;

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

import org.apache.maven.monitor.logging.Log;
import org.apache.maven.monitor.logging.SystemStreamLog;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class PluginExecutionRequest
{
    private Map parameters;

    private Map context;

    private Log log;

    public PluginExecutionRequest( Map parameters )
    {
        context = new HashMap();

        this.parameters = parameters;
    }

    public Map getParameters()
    {
        return parameters;
    }

    public void setParameters( Map parameters )
    {
        this.parameters = parameters;
    }

    public Object getParameter( String key )
    {
        return parameters.get( key );
    }

    public void addContextValue( Object key, Object value )
    {
        context.put( key, value );
    }

    public Object getContextValue( String key )
    {
        return context.get( key );
    }

    public void setLog( Log log )
    {
        this.log = log;
    }
    
    public Log getLog()
    {
        synchronized(this)
        {
            if(log == null)
            {
                log = new SystemStreamLog();
            }
        }
        
        return log;
    }
}
