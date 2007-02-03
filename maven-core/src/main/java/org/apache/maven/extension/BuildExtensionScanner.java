package org.apache.maven.extension;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.profiles.ProfileManager;
import org.codehaus.plexus.cache.Cache;

import java.io.File;

public interface BuildExtensionScanner
{
    
    String ROLE = BuildExtensionScanner.class.getName();
    
    void scanForBuildExtensions( File pom, ArtifactRepository localRepository, ProfileManager globalProfileManager, Cache pomFilesById )
        throws ExtensionScanningException;

}
