package org.apache.maven.project.path;

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

import org.apache.maven.model.Model;

import java.io.File;

/**
 * Provides resolution and relativization for filesystem paths against an arbitrary base directory.
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public interface PathTranslator
{

    /**
     * The role name for this component, used by the Plexus container to lookup implementations.
     */
    String ROLE = PathTranslator.class.getName();

    /**
     * Resolves all well-known filesystem paths in the specified project model against the given base directory if
     * necessary to enforce absolute paths.
     *
     * @param model The project model whose paths should be aligned to the base directory, must not be <code>null</code>.
     * @param basedir The absolute path to the base directory used to resolve a relative path against, must not be
     *            <code>null</code>.
     */
    void alignToBaseDirectory( Model model, File basedir );

    /**
     * Resolves the specified filesystem path against the given base directory if necessary.
     *
     * @param path The path to resolve, may be <code>null</code>.
     * @param basedir The absolute path to the base directory used to resolve the input path against, must not be
     *            <code>null</code>.
     * @return The absolute filesystem path corresponding to the input path or <code>null</code> if the input path was
     *         <code>null</code>. The returned path will always use the system-dependent file separator as reported
     *         by {@link java.io.File#separatorChar}.
     */
    String alignToBaseDirectory( String path, File basedir );

    /**
     * Relativizes all well-known filesystem paths in the specified project model to the given base directory if
     * possible.
     *
     * @param model The project model whose paths should be unaligned from the base directory, must not be
     *            <code>null</code>.
     * @param basedir The absolute path to the base directory used to relativize an absolute path to, must not be
     *            <code>null</code>.
     */
    void unalignFromBaseDirectory( Model model, File basedir );

    /**
     * Relativizes the specified filesystem path to the given base directory if possible.
     *
     * @param path The path to relativize, may be <code>null</code>.
     * @param basedir The absolute path to the base directory used to relativize the input path to, must not be
     *            <code>null</code>.
     * @return The relative filesystem path corresponding to the input path if relativization was possible, the absolute
     *         path if it was not and <code>null</code> if the input path was <code>null</code>. The relative paths
     *         returned will always use the forward slash as file separator.
     */
    String unalignFromBaseDirectory( String path, File basedir );
}
