package org.apache.maven.lifecycle;

import java.util.List;

public class LegacyLifecycleParsingTestComponent
{
    
    public static final String ROLE = LegacyLifecycleParsingTestComponent.class.getName();
    private List lifecycles;
    
    public List getLifecycles()
    {
        return lifecycles;
    }

}
