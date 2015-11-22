/*
 * Copyright 2014 David Lukacs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lukacsd.aws.scheme.ec2.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ZoneMetadata implements Serializable {
    private static final long serialVersionUID = -7452887901145137265L;

    private String regionName;
    private String zoneName;
    private String endpoint;
    private ZoneStatusType state;
    private List<String> messages = new LinkedList<String>( );

    public ZoneMetadata( ) {
    }

    public ZoneMetadata( String regionName, String zoneName, String endpoint ) {
        this.regionName = regionName;
        this.zoneName = zoneName;
        this.endpoint = endpoint;
    }

    public String getRegionName() {
        return regionName;
    }

    public String getZoneName() {
        return zoneName;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public ZoneStatusType getState() {
        return state;
    }

    public void setState( String code ) {
        this.state = ZoneStatusType.forCode( code );
    }

    public List<String> getMessages() {
        return Collections.unmodifiableList( messages );
    }

    public void addMessage( String message ) {
        String msg = message.trim( );

        if ( !messages.contains( msg ) ) {
            messages.add( msg );
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( endpoint == null ) ? 0 : endpoint.hashCode( ) );
        result = prime * result + ( ( regionName == null ) ? 0 : regionName.hashCode( ) );
        result = prime * result + ( ( zoneName == null ) ? 0 : zoneName.hashCode( ) );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass( ) != obj.getClass( ) )
            return false;
        ZoneMetadata other = ( ZoneMetadata ) obj;
        if ( endpoint == null ) {
            if ( other.endpoint != null )
                return false;
        } else if ( !endpoint.equals( other.endpoint ) )
            return false;
        if ( regionName == null ) {
            if ( other.regionName != null )
                return false;
        } else if ( !regionName.equals( other.regionName ) )
            return false;
        if ( zoneName == null ) {
            if ( other.zoneName != null )
                return false;
        } else if ( !zoneName.equals( other.zoneName ) )
            return false;
        return true;
    }

}
