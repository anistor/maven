package org.apache.maven.usability;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;

public class ArtifactNotFoundDiagnoser
    implements ErrorDiagnoser
{

    private WagonManager wagonManager;

    public boolean canDiagnose( Throwable error )
    {
        return DiagnosisUtils.containsInCausality( error, ArtifactNotFoundException.class );
    }

    public String diagnose( Throwable error )
    {
        ArtifactNotFoundException exception = (ArtifactNotFoundException) DiagnosisUtils.getFromCausality( error,
                                                                                                           ArtifactNotFoundException.class );

        StringBuffer message = new StringBuffer();

        message.append( "Failed to resolve artifact." );
        message.append( "\n\n" );
        message.append( exception.getMessage() );

        if ( !wagonManager.isOnline() )
        {
            message.append( "\n" ).append( DiagnosisUtils.getOfflineWarning() );
        }

        Throwable root = DiagnosisUtils.getRootCause( exception );

        if ( root != null )
        {
            message.append( "\nRoot Cause: " ).append( root.getMessage() );
        }

        message.append( "\n" );

        return message.toString();
    }

}
