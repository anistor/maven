package org.apache.maven.artifact.ant;

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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;

/**
 * Base class for a repository.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public abstract class Repository
    extends ProjectComponent
{
	private String id;
	
    private String refid;

    private String layout = "default";

    public String getId()
    {
    	    System.out.println("Repository.getId() == " + getInstance().id);
    	    if (getInstance().id == null)
    	    {
    	    	    throw new BuildException("id must be specified for a repository definition");
    	    }
    	    return getInstance().id;
    }
    
    public void setId( String id )
    {
    	    this.id = id;
    }
    
    public String getRefid()
    {
        return refid;
    }

    public void setRefid( String refid )
    {
        this.refid = refid;
    }

    protected Repository getInstance()
    {
        Repository instance = this;
        if ( refid != null )
        {
            instance = (Repository) getProject().getReference( refid );
        }
        return instance;
    }

    public String getLayout()
    {
        return getInstance().layout;
    }

    public void setLayout( String layout )
    {
        this.layout = layout;
    }
}
