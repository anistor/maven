package org.apache.maven.plugin.plugin;

import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;

import java.io.File;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public abstract class AbstractPluginMojo
    extends AbstractPlugin
{

    protected File getJarFile( PluginExecutionRequest request )
    {
        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        String outputDirectory = (String) request.getParameter( "outputDirectory" );

        String jarName = (String) request.getParameter( "jarName" );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        File jarFile = new File( new File( outputDirectory ), jarName + ".jar" );

        return jarFile;
    }




}
