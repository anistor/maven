package org.apache.maven.model.building;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;

/**
 * Describes a tag used by the model builder to access a {@link ModelCache}. This interface basically aggregates a name
 * and a class to provide some type safety when working with the otherwise untyped cache.
 * 
 * @author Benjamin Bentmann
 * @param <T> The type of data associated with the tag.
 */
interface ModelCacheTag<T>
{

    /**
     * Gets the name of the tag.
     * 
     * @return The name of the tag, must not be {@code null}.
     */
    String getName();

    /**
     * Gets the type of data associated with this tag.
     * 
     * @return The type of data, must not be {@code null}.
     */
    Class<T> getType();

    /**
     * Creates a copy of the data suitable for storage in the cache. The original data to store can be mutated after the
     * cache is populated but the state of the cache must not change so we need to make a copy.
     * 
     * @param data The data to store in the cache, must not be {@code null}.
     * @return The data being stored in the cache, never {@code null}.
     */
    T intoCache( T data );

    /**
     * Creates a copy of the data suitable for retrieval from the cache. The retrieved data can be mutated after the
     * cache is queried but the state of the cache must not change so we need to make a copy.
     * 
     * @param data The data to retrieve from the cache, must not be {@code null}.
     * @return The data being retrieved from the cache, never {@code null}.
     */
    T fromCache( T data );

    /**
     * The tag used to denote raw model data.
     */
    public static final ModelCacheTag<ModelData> RAW = new ModelCacheTag<ModelData>()
    {

        public String getName()
        {
            return "raw";
        }

        public Class<ModelData> getType()
        {
            return ModelData.class;
        }

        public ModelData intoCache( ModelData data )
        {
            Model model = ModelUtils.cloneModel( data.getModel() );
            return new ModelData( model, data.getGroupId(), data.getArtifactId(), data.getVersion() );
        }

        public ModelData fromCache( ModelData data )
        {
            return intoCache( data );
        }

    };

    /**
     * The tag used to denote an effective dependency management section from an imported model.
     */
    public static final ModelCacheTag<DependencyManagement> IMPORT = new ModelCacheTag<DependencyManagement>()
    {

        public String getName()
        {
            return "import";
        }

        public Class<DependencyManagement> getType()
        {
            return DependencyManagement.class;
        }

        public DependencyManagement intoCache( DependencyManagement data )
        {
            return ModelUtils.cloneDependencyManagement( data );
        };

        public DependencyManagement fromCache( DependencyManagement data )
        {
            return intoCache( data );
        };

    };

}
