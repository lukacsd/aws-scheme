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

package org.lukacsd.aws.scheme.ec2;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.lukacsd.aws.scheme.AccountProperties;
import org.lukacsd.aws.scheme.Constants;
import org.lukacsd.aws.scheme.ec2.model.ClusterSnapshot;
import org.lukacsd.aws.scheme.ec2.model.ClusterSnapshotBuilder;
import org.lukacsd.aws.scheme.ec2.model.InstanceResource;
import org.lukacsd.aws.scheme.ec2.model.InstanceStatusType;
import org.lukacsd.aws.scheme.ec2.model.NetworkInterfaceResource;
import org.lukacsd.aws.scheme.util.Multiplexer;
import org.lukacsd.aws.scheme.util.ProgressUpdate;
import org.lukacsd.aws.scheme.util.StringBundle;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Address;
import com.amazonaws.services.ec2.model.AssociateAddressRequest;
import com.amazonaws.services.ec2.model.AvailabilityZone;
import com.amazonaws.services.ec2.model.DescribeAddressesRequest;
import com.amazonaws.services.ec2.model.DescribeAddressesResult;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeNetworkInterfacesRequest;
import com.amazonaws.services.ec2.model.DescribeNetworkInterfacesResult;
import com.amazonaws.services.ec2.model.DescribeTagsRequest;
import com.amazonaws.services.ec2.model.DescribeTagsResult;
import com.amazonaws.services.ec2.model.DisassociateAddressRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.NetworkInterface;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.TagDescription;

public class Ec2Manager {
    public static enum GET_SNAPSHOT {
        STEP1, STEP2, STEP3
    };

    public static enum SET_SCHEME {
        STEP1, STEP2, STEP3
    };

    private CustomEC2Client ec2Client;

    public Ec2Manager( AccountProperties properties ) {
        this.ec2Client = getEc2Client( properties );
    }

    public ClusterSnapshot getClusterSnapshot( StringBundle bundle, ProgressUpdate progress ) {
        ClusterSnapshotBuilder builder = new ClusterSnapshotBuilder( );
        builder.withAccountProperties( ec2Client.getAccountProperties( ) );

        progress.setMax( GET_SNAPSHOT.values( ).length );

        // query zones
        progress.advance( bundle.getString( GET_SNAPSHOT.STEP1 ) );

        DescribeAvailabilityZonesResult zones = ec2Client.describeAvailabilityZones( );
        for ( AvailabilityZone zone : zones.getAvailabilityZones( ) ) {
            builder.withZone( ec2Client.getEndpoint( ).toString( ), zone );
        }

        // collect scheme names from current endpoint
        progress.advance( bundle.getString( GET_SNAPSHOT.STEP2 ) );

        DescribeTagsResult schemes = ec2Client.describeSchemes( );

        if ( !schemes.getTags( ).isEmpty( ) ) {
            // collect instanceIds from all schemes
            Set<String> instanceIdList = new HashSet<String>( );
            for ( TagDescription tag : schemes.getTags( ) ) {
                instanceIdList.add( tag.getResourceId( ) );
            }

            // collect instance infos
            progress.advance( bundle.getString( GET_SNAPSHOT.STEP3 ) );

            List<InstanceResource> instances = ec2Client.describeInstances( instanceIdList );
            for ( InstanceResource instance : instances ) {
                builder.withInstance( instance );
            }
        }

        return builder.build( );
    }

    public void setActiveScheme( StringBundle bundle, ClusterSnapshot currentSnapshot, String newScheme, ProgressUpdate progress ) {
        List<String> newInstanceIds = currentSnapshot.getInstanceIdsForScheme( newScheme );

        progress.setMax( SET_SCHEME.values( ).length );

        // start instances associated to the new scheme
        progress.advance( bundle.getString( SET_SCHEME.STEP1 ) );

        new Multiplexer( ).task( startInstance( newInstanceIds ) ).executeAndBlock( );
        new Multiplexer( ).task( isInstancesRunning( newInstanceIds ) ).executeAndBlock( );

        // assign the public ip to the public instance in the new scheme
        progress.advance( bundle.getString( SET_SCHEME.STEP2 ) );

        Set<String> oldInstances = new HashSet<String>( );
        List<InstanceResource> newInstances = currentSnapshot.getInstancesForScheme( newScheme );
        for ( InstanceResource newInstance : newInstances ) {
            for ( NetworkInterfaceResource iface : newInstance.getInterfaces( ) ) {
                String previous = ec2Client.allocateDesignatedPublicIp( iface );
                if ( previous != null ) {
                    oldInstances.add( previous );
                }
            }
        }

        if ( oldInstances.size( ) > 0 ) {
            // shut down old scheme instances
            progress.advance( bundle.getString( SET_SCHEME.STEP3 ) );

            oldInstances.addAll( currentSnapshot.getSchemeNeighbours( oldInstances.iterator( ).next( ) ) );
            oldInstances.removeAll( newInstanceIds );

            ec2Client.stopInstances( oldInstances );
        }
    }

    private Runnable startInstance( final List<String> newInstances ) {
        return new Runnable( ) {
            @Override
            public void run() {
                ec2Client.startInstances( newInstances );
            }
        };
    }

    private Runnable isInstancesRunning( final List<String> instanceIds ) {
        return new Runnable( ) {
            @Override
            public void run() {
                int[ ] sleepQuantums = new int[ ] { 15, 10, 10, 10, 5, 5, 5, 5, 5 };
                Random rnd = new Random( );

                Map<String, InstanceStatusType> statuses = null;

                for ( int i = 0; i < sleepQuantums.length; i++ ) {
                    statuses = getInstanceStatusById( instanceIds );

                    if ( statuses.size( ) == instanceIds.size( ) ) {
                        HashSet<InstanceStatusType> set = new HashSet<InstanceStatusType>( statuses.values( ) );
                        if ( set.size( ) == 1 && set.contains( InstanceStatusType.RUNNING ) ) {
                            statuses = null;

                            break;
                        }
                    }

                    int variation = rnd.nextInt( 400 ) + 500;
                    int sleepMillis = sleepQuantums[ i ] * 1000 + variation;

                    try {
                        Thread.sleep( sleepMillis );
                    } catch ( InterruptedException e ) {
                    }
                }

                if ( statuses != null ) {
                    throw new IllegalStateException( String.format( "not all instances are running [%s]", statuses ) );
                }
            }

            private Map<String, InstanceStatusType> getInstanceStatusById( List<String> instanceIds ) {
                Map<String, InstanceStatusType> retval = new HashMap<String, InstanceStatusType>( );

                DescribeInstanceStatusResult result = ec2Client.describeInstanceStatus( new DescribeInstanceStatusRequest( )
                        .withInstanceIds( instanceIds ) );

                for ( InstanceStatus status : result.getInstanceStatuses( ) ) {
                    InstanceState instanceState = status.getInstanceState( );
                    retval.put( status.getInstanceId( ), InstanceStatusType.forCode( instanceState.getCode( ) ) );
                }

                return retval;
            }
        };
    }

    private CustomEC2Client getEc2Client( AccountProperties properties ) {
        CustomEC2Client ec2Client = new CustomEC2Client( properties );
        ec2Client.setEndpoint( properties.getEc2Region( ) );

        return ec2Client;
    }

    public class CustomEC2Client extends AmazonEC2Client {
        private AccountProperties accountProperties;

        public CustomEC2Client( AccountProperties properties ) {
            super( properties );

            this.accountProperties = properties;
        }

        public void startInstances( Collection<String> instanceIds ) {
            if ( instanceIds.size( ) > 0 ) {
                startInstances( new StartInstancesRequest( ).withInstanceIds( instanceIds ) );
            }
        }

        public void stopInstances( Collection<String> instanceIds ) {
            if ( instanceIds.size( ) > 0 ) {
                stopInstances( new StopInstancesRequest( ).withInstanceIds( instanceIds ) );
            }
        }

        public String allocateDesignatedPublicIp( NetworkInterfaceResource iface ) {
            String ipAllocationId = null;
            String associationId = null;
            String currentIface = null;
            String currentInstance = null;

            if ( iface.getDesignatedPublicIp( ) != null ) {
                DescribeAddressesResult addresses = describeAddresses( iface.getDesignatedPublicIp( ) );

                if ( addresses.getAddresses( ).size( ) == 1 ) {
                    for ( Address address : addresses.getAddresses( ) ) {
                        ipAllocationId = address.getAllocationId( );
                        associationId = address.getAssociationId( );
                        currentIface = address.getNetworkInterfaceId( );
                        currentInstance = address.getInstanceId( );
                    }

                    if ( !iface.getNetworkInterfaceId( ).equals( currentIface ) ) {
                        if ( associationId != null ) {
                            disassociateAddress( new DisassociateAddressRequest( ).withAssociationId( associationId ) );
                        }
                        associateAddress( new AssociateAddressRequest( ).withNetworkInterfaceId( iface.getNetworkInterfaceId( ) )
                                .withAllocationId( ipAllocationId ) );
                    }
                }
            }

            return currentInstance;
        }

        public DescribeAddressesResult describeAddresses( String publicIp ) {
            return describeAddresses( new DescribeAddressesRequest( ).withPublicIps( publicIp ) );
        }

        public DescribeTagsResult describeSchemes() {
            return describeTags( new DescribeTagsRequest( ).withFilters( new Filter( ).withName( "tag:" + Constants.KEY_SCHEME )
                    .withValues( "*" ) ) );
        }

        public List<InstanceResource> describeInstances( Collection<String> instanceIdList ) {
            List<InstanceResource> retval = new LinkedList<InstanceResource>( );

            DescribeInstancesResult instances = describeInstances( new DescribeInstancesRequest( ).withInstanceIds( instanceIdList ) );
            DescribeNetworkInterfacesResult networkInterfaces = describeNetworkInterfaces( new DescribeNetworkInterfacesRequest( )
                    .withFilters( new Filter( ).withName( "attachment.instance-id" ).withValues( instanceIdList ) ) );

            Map<String, List<NetworkInterface>> ifaces = new HashMap<String, List<NetworkInterface>>( );
            for ( NetworkInterface iface : networkInterfaces.getNetworkInterfaces( ) ) {
                List<NetworkInterface> ifaceList = ifaces.get( iface.getAttachment( ).getInstanceId( ) );

                if ( ifaceList == null ) {
                    ifaceList = new LinkedList<NetworkInterface>( );
                    ifaces.put( iface.getAttachment( ).getInstanceId( ), ifaceList );
                }

                ifaceList.add( iface );
            }

            for ( Reservation reservation : instances.getReservations( ) ) {
                for ( Instance instance : reservation.getInstances( ) ) {
                    InstanceResource instanceResource = new InstanceResource( accountProperties.getAccountAlias( ), instance );
                    if ( ifaces.containsKey( instance.getInstanceId( ) ) ) {
                        for ( NetworkInterface iface : ifaces.get( instance.getInstanceId( ) ) ) {
                            instanceResource.addNetworkInterface( iface );
                        }
                    }

                    retval.add( instanceResource );
                }
            }

            return retval;
        }

        public URI getEndpoint() {
            return endpoint;
        }

        public AccountProperties getAccountProperties() {
            return accountProperties;
        }
    }

}
