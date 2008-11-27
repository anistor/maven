package org.apache.maven.artifact.resolver.conflict;

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

/**
 * A factory that produces conflict resolvers of various types.
 *
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @version $Id$
 * @see ConflictResolver
 * @since 3.0
 */
public interface ConflictResolverFactory
{
    // constants --------------------------------------------------------------

    /** The plexus role for this component. */
    String ROLE = ConflictResolverFactory.class.getName();

    // methods ----------------------------------------------------------------

    /**
     * Gets a conflict resolver of the specified type.
     *
     * @param type the type of conflict resolver to obtain
     * @return the conflict resolver
     * @throws ConflictResolverNotFoundException
     *          if the specified type was not found
     */
    ConflictResolver getConflictResolver( String type )
        throws ConflictResolverNotFoundException;
}
