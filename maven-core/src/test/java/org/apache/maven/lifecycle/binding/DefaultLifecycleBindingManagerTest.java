package org.apache.maven.lifecycle.binding;

import org.codehaus.plexus.PlexusTestCase;

public class DefaultLifecycleBindingManagerTest
    extends PlexusTestCase
{
    
    private DefaultLifecycleBindingManager mgr;
    
    public void setUp() throws Exception
    {
        super.setUp();
        
        this.mgr = (DefaultLifecycleBindingManager) lookup( LifecycleBindingManager.ROLE, "default" );
    }
    
    public void testLookup()
    {
        assertNotNull( mgr );
    }

}
