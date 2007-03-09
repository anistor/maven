package org.apache.maven.lifecycle;

import org.apache.maven.lifecycle.model.MojoBinding;

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
