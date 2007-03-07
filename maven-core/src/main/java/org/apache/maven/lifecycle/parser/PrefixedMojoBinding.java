package org.apache.maven.lifecycle.parser;

import org.apache.maven.lifecycle.MojoBinding;

public class PrefixedMojoBinding
    extends MojoBinding
{
    
    private String prefix;
    
    public void setPrefix( String prefix )
    {
        this.prefix = prefix;
    }
    
    public String getPrefix()
    {
        return prefix;
    }

}
