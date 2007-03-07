package org.apache.maven.plugin.loader;

import org.apache.maven.model.Plugin;

public class PluginLoaderException
    extends Exception
{

    private final Plugin plugin;

    public PluginLoaderException( Plugin plugin, String message, Throwable cause )
    {
        super( message, cause );
        this.plugin = plugin;
    }

    public PluginLoaderException( Plugin plugin, String message )
    {
        super( message );
        this.plugin = plugin;
    }
    
    public Plugin getPlugin()
    {
        return plugin;
    }

}
