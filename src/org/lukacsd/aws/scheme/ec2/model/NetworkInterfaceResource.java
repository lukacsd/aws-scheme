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

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.lukacsd.aws.scheme.Constants;

import com.amazonaws.services.ec2.model.NetworkInterface;
import com.amazonaws.services.ec2.model.Tag;

public class NetworkInterfaceResource extends ClusterResource {
    private static final long serialVersionUID = 3023316859533121335L;

    private String interfaceId;
    private String publicIp;
    private String designatedPublicIp;
    private String hostname;

    public NetworkInterfaceResource( String accountName, NetworkInterface networkInterface ) {
        super( accountName );

        this.designatedPublicIp = getTagValue( networkInterface, Constants.KEY_PUBLIC_IP );
        if ( networkInterface.getAssociation( ) != null ) {
            this.publicIp = networkInterface.getAssociation( ).getPublicIp( );
        }
        this.interfaceId = networkInterface.getNetworkInterfaceId( );
    }

    public String getNetworkInterfaceId() {
        return interfaceId;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public String getDesignatedPublicIp() {
        return designatedPublicIp;
    }

    public String getDesignatedHostname() {
        if ( hostname == null ) {
            InetAddress address;
            try {
                address = InetAddress.getByName( designatedPublicIp );
                hostname = address.getHostName( );
            } catch ( UnknownHostException e ) {
                hostname = "unresolvable";
            }
        }

        return hostname;
    }

    private String getTagValue( NetworkInterface iface, String tagKey ) {
        String retval = null;

        for ( Tag tag : iface.getTagSet( ) ) {
            if ( tagKey.equals( tag.getKey( ) ) ) {
                retval = tag.getValue( ).trim( );
                break;
            }
        }

        return retval;
    }

}
