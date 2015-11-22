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

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lukacsd.aws.scheme.Constants;

import android.annotation.SuppressLint;

import com.amazonaws.services.ec2.model.GroupIdentifier;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceNetworkInterface;
import com.amazonaws.services.ec2.model.NetworkInterface;
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.Tag;

public class InstanceResource extends ClusterResource {
    private static final long serialVersionUID = -6430343860495413566L;

    private String scheme;
    private String name;
    private String instanceId;
    private String instanceType;
    private String architecture;
    private Date launchTime;
    private String rootDeviceType;
    private List<String> securityGroups = new LinkedList<String>( );
    private String placementZone;
    private String vpcId;
    private List<NetworkInterfaceResource> interfaces = new LinkedList<NetworkInterfaceResource>( );
    private InstanceStatusType state;

    public InstanceResource( String account, Instance instance ) {
        super( account );

        this.scheme = getTagValue( instance, Constants.KEY_SCHEME );
        this.name = getTagValue( instance, "Name" );
        if ( name == null ) {
            this.name = "???";
        }
        this.instanceId = instance.getInstanceId( );
        this.instanceType = instance.getInstanceType( );
        this.architecture = instance.getArchitecture( );
        this.launchTime = instance.getLaunchTime( );
        this.rootDeviceType = instance.getRootDeviceType( );

        for ( GroupIdentifier group : instance.getSecurityGroups( ) ) {
            this.securityGroups.add( group.getGroupName( ) );
        }
        Collections.sort( securityGroups );

        List<InstanceNetworkInterface> ifaces = new LinkedList<InstanceNetworkInterface>( instance.getNetworkInterfaces( ) );
        Collections.sort( ifaces, new Comparator<InstanceNetworkInterface>( ) {
            @Override
            public int compare( InstanceNetworkInterface lhs, InstanceNetworkInterface rhs ) {
                return lhs.getAttachment( ).getDeviceIndex( ).compareTo( rhs.getAttachment( ).getDeviceIndex( ) );
            }
        } );

        Placement placement = instance.getPlacement( );
        this.placementZone = placement.getAvailabilityZone( );
        this.vpcId = instance.getVpcId( );
        this.state = InstanceStatusType.forCode( instance.getState( ).getCode( ) );
    }

    public String getName() {
        return name;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public String getArchitecture() {
        return architecture;
    }

    public Date getLaunchTime() {
        return launchTime;
    }

    public String getRootDeviceType() {
        return rootDeviceType;
    }

    public List<String> getSecurityGroups() {
        return securityGroups;
    }

    public String getPlacementZone() {
        return placementZone;
    }

    public List<String> getPublicIps() {
        List<String> retval = new LinkedList<String>( );

        for ( NetworkInterfaceResource iface : interfaces ) {
            if ( iface.getPublicIp( ) != null ) {
                retval.add( iface.getPublicIp( ) );
            }
        }

        return retval;
    }

    public List<String> getDesignatedPublicIps() {
        Set<String> retval = new HashSet<String>( );

        for ( NetworkInterfaceResource iface : interfaces ) {
            if ( iface.getDesignatedPublicIp( ) != null ) {
                retval.add( iface.getDesignatedPublicIp( ) );
            }
        }

        return new LinkedList<String>( retval );
    }

    public Map<String, String> getDesignatedPublicIpsWithResolvedHostnames() {
        Map<String, String> retval = new HashMap<String, String>( );

        for ( NetworkInterfaceResource iface : interfaces ) {
            if ( iface.getDesignatedPublicIp( ) != null ) {
                retval.put( iface.getDesignatedPublicIp( ), iface.getDesignatedHostname( ) );
            }
        }

        return retval;
    }

    public boolean isIpOwned( String ip ) {
        boolean retval = false;

        for ( NetworkInterfaceResource iface : interfaces ) {
            retval = ip.equals( iface.getPublicIp( ) );

            if ( retval ) {
                break;
            }
        }

        return retval;
    }

    public boolean areDesignatedIpsOwned() {
        boolean retval = true;

        for ( NetworkInterfaceResource iface : interfaces ) {
            if ( iface.getDesignatedPublicIp( ) != null ) {
                retval &= iface.getDesignatedPublicIp( ).equals( iface.getPublicIp( ) );
            }
        }

        return retval;
    }

    public List<NetworkInterfaceResource> getInterfaces() {
        return interfaces;
    }

    public String getVpcId() {
        return vpcId;
    }

    public InstanceStatusType getState() {
        return state;
    }

    public String getScheme() {
        return scheme;
    }

    public void addNetworkInterface( NetworkInterface networkInterface ) {
        if ( networkInterface != null ) {
            interfaces.add( new NetworkInterfaceResource( getAccountName( ), networkInterface ) );
        }
    }

    private String getTagValue( Instance instance, String tagKey ) {
        String retval = null;

        for ( Tag tag : instance.getTags( ) ) {
            if ( tagKey.equals( tag.getKey( ) ) ) {
                retval = tag.getValue( ).trim( );
                break;
            }
        }

        return retval;
    }

    @SuppressLint( "DefaultLocale" )
    private List<String> getTagValues( Instance instance, String tagKey ) {
        List<String> retval = null;
        String value = getTagValue( instance, tagKey );

        if ( value != null ) {
            retval = new LinkedList<String>( );
            String[ ] vhosts = value.split( "," );
            for ( String vhost : vhosts ) {
                retval.add( vhost.trim( ).toLowerCase( ) );
            }
            Collections.sort( retval );
        }

        return retval;
    }

}
