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

import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.codehaus.plexus.component.discovery.AbstractComponentDiscoverer;
import org.codehaus.plexus.component.repository.ComponentSetDescriptor;
import org.codehaus.plexus.configuration.PlexusConfigurationException;

import java.io.Reader;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class MavenPluginDiscoverer
    extends AbstractComponentDiscoverer
{
    private PluginDescriptorBuilder builder;

    public MavenPluginDiscoverer()
    {
        builder = new PluginDescriptorBuilder();
    }

    public String getComponentDescriptorLocation()
    {
        return "META-INF/maven/plugin.xml";
    }

    public ComponentSetDescriptor createComponentDescriptors( Reader componentDescriptorConfiguration, String source )
        throws PlexusConfigurationException
    {
        return builder.build( componentDescriptorConfiguration, source );
    }
}
