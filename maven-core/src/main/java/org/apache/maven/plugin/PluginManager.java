package org.apache.maven.plugin;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.goal.GoalExecutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;

import java.util.Map;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public interface PluginManager
{
    String ROLE = PluginManager.class.getName();

    PluginExecutionResponse executeMojo( MavenSession session, String goalName )
         throws GoalExecutionException;

    void processPluginDescriptor( MavenPluginDescriptor pluginDescriptor )
        throws Exception;

    // TODO: not currently used
    Map getMojoDescriptors();

    // TODO: not currently used (usages are in the phases that are no longer used)
    MojoDescriptor getMojoDescriptor( String goalId );

    void verifyPluginForGoal( String pluginId, MavenSession session )
        throws Exception;
}
