package org.apache.maven.artifact.resolver;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.VersionRange;
import org.codehaus.plexus.logging.Logger;

/**
 * Send resolution events to the debug log.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class DebugResolutionListener
    implements ResolutionListener
{
    private Logger logger;

    private String indent = "";

    public DebugResolutionListener( Logger logger )
    {
        this.logger = logger;
    }

    public void testArtifact( Artifact node )
    {
    }

    public void startProcessChildren( Artifact artifact )
    {
        indent += "  ";
    }

    public void endProcessChildren( Artifact artifact )
    {
        indent = indent.substring( 2 );
    }

    public void includeArtifact( Artifact artifact )
    {
        logger.debug( indent + artifact + " (selected for " + artifact.getScope() + ")" );
    }

    public void omitForNearer( Artifact omitted, Artifact kept )
    {
        logger.debug( indent + omitted + " (removed - nearer found: " + kept.getVersion() + ")" );
    }

    public void omitForCycle( Artifact omitted )
    {
        logger.debug( indent + omitted + " (removed - causes a cycle in the graph)" );
    }

    public void updateScopeCurrentPom( Artifact artifact, String scope )
    {
        logger.debug( indent + artifact + " (not setting scope to: " + scope + "; local scope " + artifact.getScope() +
            " wins)" );
    }

    public void updateScope( Artifact artifact, String scope )
    {
        logger.debug( indent + artifact + " (setting scope to: " + scope + ")" );
    }

    public void selectVersionFromRange( Artifact artifact )
    {
        logger.debug( indent + artifact + " (setting version to: " + artifact.getVersion() + " from range: " +
            artifact.getVersionRange() + ")" );
    }

    public void restrictRange( Artifact artifact, Artifact replacement, VersionRange newRange )
    {
        logger.debug( indent + artifact + " (range restricted from: " + artifact.getVersionRange() + " and: " +
            replacement.getVersionRange() + " to: " + newRange + " )" );
    }

    public void manageArtifact( Artifact artifact, Artifact replacement )
    {
        String msg = indent + artifact;
        msg += " (";
        if ( replacement.getVersion() != null )
        {
            msg += "applying version: " + replacement.getVersion() + ";";
        }
        if ( replacement.getScope() != null )
        {
            msg += "applying scope: " + replacement.getScope();
        }
        msg += ")";
        logger.debug( msg );
    }
}
