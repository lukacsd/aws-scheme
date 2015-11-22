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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ClusterScheme implements Serializable {
    private static final long serialVersionUID = 8204291284478048751L;

    private String name;
    private Map<String, InstanceResource> instanceById = new HashMap<String, InstanceResource>( );

    public ClusterScheme( String name ) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addInstance( InstanceResource instance ) {
        instanceById.put( instance.getInstanceId( ), instance );
    }

    public boolean hasInstance( String instanceId ) {
        return instanceById.containsKey( instanceId );
    }

    public List<String> getInstanceIds() {
        List<String> retval = new LinkedList<String>( instanceById.keySet( ) );

        Collections.sort( retval );

        return retval;
    }

    public List<InstanceResource> getInstances() {
        List<InstanceResource> retval = new LinkedList<InstanceResource>( );

        retval.addAll( instanceById.values( ) );

        Collections.sort( retval, new Comparator<InstanceResource>( ) {
            @Override
            public int compare( InstanceResource lhs, InstanceResource rhs ) {
                return lhs.getName( ).compareTo( rhs.getName( ) );
            }
        } );

        return retval;
    }

    public boolean isActive() {
        boolean retval = true;

        for ( InstanceResource instance : instanceById.values( ) ) {
            retval &= InstanceStatusType.RUNNING.equals( instance.getState( ) );
            retval &= instance.areDesignatedIpsOwned( );

            if ( !retval ) {
                break;
            }
        }

        return retval;
    }

    public Map<String, String> getDesignatedPublicIpsWithResolvedHostnames() {
        Map<String, String> retval = new HashMap<String, String>( );

        for ( InstanceResource instance : instanceById.values( ) ) {
            retval.putAll( instance.getDesignatedPublicIpsWithResolvedHostnames( ) );
        }

        return retval;
    }

}
