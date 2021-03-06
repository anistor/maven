package org.apache.maven.profiles.activation;

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
import org.apache.maven.model.ActivationFile;
import org.apache.maven.model.Profile;
import org.codehaus.plexus.util.FileUtils;

public class FileProfileActivator
    implements ProfileActivator
{
    
    public boolean canDetermineActivation( Profile profile )
    {
        return profile.getActivation() != null && profile.getActivation().getFile() != null;
    }
    
    public boolean isActive( Profile profile )
    {
        Activation activation = profile.getActivation();

        ActivationFile actFile = activation.getFile();

        if ( actFile != null )
        {
            // check if the file exists, if it does then the profile will be active
            String fileString = actFile.getExists();

            if ( fileString != null && !"".equals( fileString ) )
            {
                return FileUtils.fileExists( fileString );
            }

            // check if the file is missing, if it is then the profile will be active
            fileString = actFile.getMissing();

            if ( fileString != null && !"".equals( fileString ) )
            {
                return !FileUtils.fileExists( fileString );
            }
        }

        return false;
    }

}
