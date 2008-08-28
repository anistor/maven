package org.apache.maven.monitor.event;

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

 import java.util.EventObject;

/**
 *  Represents events that occur during the build.
 */
public class MavenEvent extends EventObject
{
    private final String eventType;

    /**
     * Construct the MavenEvent.
     * @param type The type of the event. See MavenEvents.
     * @param source The Event Source (See java.util.EventObject.
     */
    public MavenEvent(String type, Object source)
    {
        super(source);
        this.eventType = type;
    }

    /**
     * @return the Event type.
     */
    public String getEventType()
    {
        return this.eventType;
    }

    /**
     * @return the Event type.
     */
    public String toString()
    {
        return this.eventType;
    }
}
