package org.apache.maven.settings;

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

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;

public class SettingsUtilsTest
    extends TestCase
{

    @SuppressWarnings("unchecked")
    public void testShouldAppendRecessivePluginGroupIds()
    {
        Settings dominant = new Settings();
        dominant.addPluginGroup( "org.apache.maven.plugins" );
        dominant.addPluginGroup( "org.codehaus.modello" );

        Settings recessive = new Settings();
        recessive.addPluginGroup( "org.codehaus.plexus" );

        SettingsUtils.merge( dominant, recessive, Settings.GLOBAL_LEVEL );

        List pluginGroups = dominant.getPluginGroups();

        assertNotNull( pluginGroups );
        assertEquals( 3, pluginGroups.size() );
        assertEquals( "org.apache.maven.plugins", pluginGroups.get( 0 ) );
        assertEquals( "org.codehaus.modello", pluginGroups.get( 1 ) );
        assertEquals( "org.codehaus.plexus", pluginGroups.get( 2 ) );
    }
    
    public void testMergeKeyrings()
    {
        Settings dominant = new Settings();
        Security security = new Security();
        dominant.setSecurity( security );
        security.addPublicKeyRing( "key-ring-user" );
        
        Settings recessive = new Settings();
        security = new Security();
        recessive.setSecurity( security );
        security.addPublicKeyRing( "key-ring-global" );
        
        SettingsUtils.merge( dominant, recessive, Settings.GLOBAL_LEVEL );
        
        assertNotNull( dominant.getSecurity() );
        assertEquals( Arrays.asList( "key-ring-user", "key-ring-global" ), dominant.getSecurity().getPublicKeyRings() );
    }
    
    public void testMergeKeyringsUserEmpty()
    {
        Settings dominant = new Settings();
        
        Settings recessive = new Settings();
        Security security = new Security();
        recessive.setSecurity( security );
        security.addPublicKeyRing( "key-ring-global" );
        
        SettingsUtils.merge( dominant, recessive, Settings.GLOBAL_LEVEL );
        
        assertNotNull( dominant.getSecurity() );
        assertEquals( Arrays.asList( "key-ring-global" ), dominant.getSecurity().getPublicKeyRings() );
    }
    
    public void testMergeKeyringsGlobalEmpty()
    {
        Settings dominant = new Settings();
        Security security = new Security();
        dominant.setSecurity( security );
        security.addPublicKeyRing( "key-ring-user" );
        
        Settings recessive = new Settings();
        
        SettingsUtils.merge( dominant, recessive, Settings.GLOBAL_LEVEL );
        
        assertNotNull( dominant.getSecurity() );
        assertEquals( Arrays.asList( "key-ring-user" ), dominant.getSecurity().getPublicKeyRings() );
    }
    
    public void testMergeKeyringsBothEmpty()
    {
        Settings dominant = new Settings();
        
        Settings recessive = new Settings();
        
        SettingsUtils.merge( dominant, recessive, Settings.GLOBAL_LEVEL );
        
        assertNull( dominant.getSecurity() );
    }
}
