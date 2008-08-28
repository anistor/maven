package org.apache.maven.project.transformation;

import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.model.Model;

import java.io.File;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public interface ModelTransformer
{
    /**
     * Transform the model into a form acceptable to be stored into a repository.
     * @param source The source pom file.
     * @param target The target pom file.
     * @param project The project created from the source pom file.
     * @param projectId The current projectId for use in Exception messages.
     * @param debug true if debug should be enabled.
     * @return true if a new pom was written to the target directory.
     * @throws ModelTransformationException If an error occurs processing the source file or creating the target.
     */
    boolean transformModel( File source,
                            File target,
                            MavenProject project,
                            String projectId,
                            boolean debug )
        throws ModelTransformationException;

    /**
     * Replace the variables in the model items.
     * @param model The target model.
     * @param projectId The project Id for exception messages.
     * @param config The current ProjectBuilderConfiguration.
     * @throws ProjectBuildingException if an exception occurs interpolating the variables.
     */
    void resolveModel( Model model,
                       String projectId,
                       ProjectBuilderConfiguration config )
        throws ProjectBuildingException;
}