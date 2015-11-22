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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lukacsd.aws.scheme.AccountProperties;

public class ClusterSnapshot implements Serializable {
    private static final long serialVersionUID = 2450031361505167601L;

    private AccountProperties accountProperties;
    private List<ZoneMetadata> zones = new LinkedList<ZoneMetadata>( );
    private Map<String, ClusterScheme> schemeByName = new HashMap<String, ClusterScheme>( );

    public ClusterSnapshot( ) {
    }

    public ClusterSnapshot( AccountProperties accountProperties, List<ZoneMetadata> zones, List<InstanceResource> instances ) {
        this.accountProperties = accountProperties;

        addZones( zones );
        addInstances( instances );
    }

    public AccountProperties getAccountProperties() {
        return accountProperties;
    }

    public List<ZoneMetadata> getZones() {
        return Collections.unmodifiableList( zones );
    }

    public List<String> getSchemeNames() {
        List<String> retval = new ArrayList<String>( schemeByName.keySet( ) );

        Collections.sort( retval );

        return retval;
    }

    public List<ClusterScheme> getSchemes() {
        List<ClusterScheme> retval = new ArrayList<ClusterScheme>( schemeByName.values( ) );

        Collections.sort( retval, new Comparator<ClusterScheme>( ) {

            @Override
            public int compare( ClusterScheme lhs, ClusterScheme rhs ) {
                return lhs.getName( ).compareTo( rhs.getName( ) );
            }
        } );

        return retval;
    }

    public Set<String> getActiveSchemes() {
        Set<String> retval = new HashSet<String>( );

        for ( ClusterScheme scheme : schemeByName.values( ) ) {
            if ( scheme.isActive( ) ) {
                retval.add( scheme.getName( ) );
            }
        }

        return retval;
    }

    public List<String> getInstanceIdsForScheme( String schemeName ) {
        return schemeByName.get( schemeName ).getInstanceIds( );
    }

    public List<InstanceResource> getInstancesForScheme( String schemeName ) {
        return schemeByName.get( schemeName ).getInstances( );
    }

    public List<String> getSchemeNeighbours( String instanceId ) {
        Set<String> retval = new HashSet<String>( );

        for ( ClusterScheme scheme : schemeByName.values( ) ) {
            if ( scheme.hasInstance( instanceId ) ) {
                retval.addAll( scheme.getInstanceIds( ) );
            }
        }

        return new LinkedList<String>( retval );
    }

    public ClusterScheme getScheme( String scheme ) {
        return schemeByName.get( scheme );
    }

    private void addZones( List<ZoneMetadata> zones ) {
        this.zones.addAll( zones );
    }

    private void addInstances( List<InstanceResource> instances ) {
        for ( InstanceResource instance : instances ) {
            ClusterScheme scheme = schemeByName.get( instance.getScheme( ) );
            if ( scheme == null ) {
                scheme = new ClusterScheme( instance.getScheme( ) );
                schemeByName.put( instance.getScheme( ), scheme );
            }

            scheme.addInstance( instance );
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( accountProperties == null ) ? 0 : accountProperties.hashCode( ) );
        result = prime * result + ( ( schemeByName == null ) ? 0 : schemeByName.hashCode( ) );
        result = prime * result + ( ( zones == null ) ? 0 : zones.hashCode( ) );
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
        ClusterSnapshot other = ( ClusterSnapshot ) obj;
        if ( accountProperties == null ) {
            if ( other.accountProperties != null )
                return false;
        } else if ( !accountProperties.equals( other.accountProperties ) )
            return false;
        if ( schemeByName == null ) {
            if ( other.schemeByName != null )
                return false;
        } else if ( !schemeByName.equals( other.schemeByName ) )
            return false;
        if ( zones == null ) {
            if ( other.zones != null )
                return false;
        } else if ( !zones.equals( other.zones ) )
            return false;
        return true;
    }

}
