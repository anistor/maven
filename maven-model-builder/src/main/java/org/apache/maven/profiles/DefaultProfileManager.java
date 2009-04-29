package org.apache.maven.profiles;

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

import org.apache.maven.model.Activation;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.interpolator.PomInterpolatorTag;
import org.apache.maven.profiles.ProfileActivationContext;
import org.apache.maven.profiles.ProfileActivationException;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.profiles.matchers.DefaultMatcher;
import org.apache.maven.profiles.matchers.FileMatcher;
import org.apache.maven.profiles.matchers.JdkMatcher;
import org.apache.maven.profiles.matchers.ProfileMatcher;
import org.apache.maven.profiles.matchers.PropertyMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class DefaultProfileManager
    implements ProfileManager
{

    private Map<String, Profile> profilesById = new LinkedHashMap<String, Profile>();

    private ProfileActivationContext profileActivationContext;
    
    private static final ProfileMatcher defaultMatcher = new DefaultMatcher();

    private static final List<ProfileMatcher> matchers =
        Collections.unmodifiableList( Arrays.asList( new PropertyMatcher(), new FileMatcher(), new JdkMatcher() ) );    

    /**
     * the properties passed to the profile manager are the props that
     * are passed to maven, possibly containing profile activator properties
     */
    public DefaultProfileManager( ProfileActivationContext profileActivationContext )
    {
        if ( profileActivationContext == null )
        {
            this.profileActivationContext = createDefaultActivationContext();
        }
        else
        {
            this.profileActivationContext = profileActivationContext;
        }
    }

    private ProfileActivationContext createDefaultActivationContext()
    {

        return new ProfileActivationContext(System.getProperties(), false );
    }

    public ProfileActivationContext getProfileActivationContext()
    {
        return profileActivationContext;
    }

    public void setProfileActivationContext( ProfileActivationContext profileActivationContext )
    {
        this.profileActivationContext = profileActivationContext;
    }

    public Map<String, Profile> getProfilesById()
    {
        return profilesById;
    }

    /* (non-Javadoc)
    * @see org.apache.maven.project.ProfileManager#addProfile(org.apache.maven.model.Profile)
    */
    public void addProfile( Profile profile )
    {
        String profileId = profile.getId();

        Profile existing = profilesById.get( profileId );
        if ( existing != null )
        {
            System.out.println( "Overriding profile: \'" + profileId + "\' (source: " + existing.getSource() +
                ") with new instance from source: " + profile.getSource() );
        }

        profilesById.put( profile.getId(), profile );

        Activation activation = profile.getActivation();

        if ( ( activation != null ) && activation.isActiveByDefault() )
        {
            activateAsDefault( profileId );
        }
    }

	public List<Profile> getActiveProfiles() throws ProfileActivationException {
		return getActiveProfiles(null);
	}
	
    public List<Profile> getActiveProfiles( Model model )
        throws ProfileActivationException
    {
        List<Profile> activeFromPom = new ArrayList<Profile>();
        List<Profile> activeExternal = new ArrayList<Profile>();
        for ( Map.Entry<String, Profile> entry : profilesById.entrySet() )
        {
            String profileId = entry.getKey();
            Profile profile = entry.getValue();

            if ( !profileActivationContext.isExplicitlyInactive( profileId )
            		&& (profileActivationContext.isExplicitlyActive( profileId ) || isActive( profile, profileActivationContext ) ) )
            {
                if ( "pom".equals( profile.getSource() ) )
                {
                    activeFromPom.add( profile );
                }
                else
                {
                    activeExternal.add( profile );
                }
            }
        }

        if ( activeFromPom.isEmpty() )
        {
            List<String> defaultIds = profileActivationContext.getActiveByDefaultProfileIds();

            List<String> deactivatedIds = profileActivationContext.getExplicitlyInactiveProfileIds();

            for ( String profileId : defaultIds )
            {
                // If this profile was excluded, don't add it back in
                // Fixes MNG-3545
                if ( deactivatedIds.contains( profileId ) )
                {
                    continue;
                }
                Profile profile = profilesById.get( profileId );

                if ( profile != null )
                {
                    activeFromPom.add( profile );
                }
            }
        }

        List<Profile> allActive = new ArrayList<Profile>( activeFromPom.size() + activeExternal.size() );
       // System.out.println("Active From POM: " + activeFromPom.size() + ": EXTERNAL:" + activeExternal.size());
        allActive.addAll( activeExternal );
        allActive.addAll( activeFromPom );
     
        List<Profile> defaults = getDefaultProfiles(allActive);
        if(defaults.size() < allActive.size())
        {
            allActive.removeAll( defaults );
        }
        return allActive;
    }
    
    public static List<Profile> getActiveProfilesFrom(ProfileManager globalProfileManager, Properties properties, Model model)
		throws ProfileActivationException
	{
	    List<Profile> projectProfiles = new ArrayList<Profile>();
	    
	    ProfileActivationContext profileActivationContext = (globalProfileManager == null) ? new ProfileActivationContext( new Properties(), false ):
	        globalProfileManager.getProfileActivationContext();
	    profileActivationContext.getExecutionProperties().putAll(properties);
	    
	    if(globalProfileManager != null)
	    {           
	    	projectProfiles.addAll( globalProfileManager.getActiveProfiles() );    
	    }

	    ProfileManager profileManager = new DefaultProfileManager( profileActivationContext );
	    profileManager.addProfiles( model.getProfiles() );
	    projectProfiles.addAll( profileManager.getActiveProfiles() ); 
	    return projectProfiles;
	}   
 
    public static Collection<Profile> getActiveProfiles(List<Profile> profiles, ProfileManagerInfo profileContextInfo)
    {
        Properties properties = profileContextInfo.getInterpolatorProperties();
        Collection<String> activeProfileIds = profileContextInfo.getActiveProfileIds();
        Collection<String> inactiveProfileIds = profileContextInfo.getInactiveProfileIds();
        
        List<Profile> matchedProfiles = new ArrayList<Profile>();
        List<Profile> defaultProfiles = new ArrayList<Profile>();
        for ( Profile profile : profiles )
        {
            String profileId = profile.getId();

            if ( !inactiveProfileIds.contains( profileId ) )
            {
                if ( activeProfileIds.contains( profileId ) )
                {
                    matchedProfiles.add( profile );
                }
                else if ( defaultMatcher.isMatch( profile, properties ) )
                {
                    defaultProfiles.add( profile );
                }
                else
                {
                    for ( ProfileMatcher matcher : matchers )
                    {
                        if ( matcher.isMatch( profile, properties ) )
                        {
                            matchedProfiles.add( profile );
                            break;
                        }
                    }
                }
            }
        }

        if ( matchedProfiles.isEmpty() )
        {
            matchedProfiles = defaultProfiles;
        }

        return matchedProfiles;
    }    

    /* (non-Javadoc)
     * @see org.apache.maven.project.ProfileManager#addProfiles(java.util.List)
     */
    public void addProfiles( List<Profile> profiles )
    {
        for ( Profile profile : profiles )
        {
            addProfile( profile );
        }
    }   
    
    private static List<Profile> getDefaultProfiles(List<Profile> profiles)
    {
        List<Profile> defaults = new ArrayList<Profile>();
        for(Profile p : profiles)
        {
            if ( p.getActivation() != null && p.getActivation().isActiveByDefault() )
            {
                defaults.add( p );
            }
        }
        return defaults;
    }

    private boolean isActive( Profile profile, ProfileActivationContext context )
        throws ProfileActivationException
    {
        for(ProfileMatcher matcher : matchers)
        {
            if(matcher.isMatch(profile, context.getExecutionProperties()))
            {
                return true;
            }
        }
        return false;
    }

    private void activateAsDefault( String profileId )
    {
        List<String> defaultIds = profileActivationContext.getActiveByDefaultProfileIds();

        if ( !defaultIds.contains( profileId ) )
        {
            profileActivationContext.setActiveByDefault( profileId );
        }
    }
}
