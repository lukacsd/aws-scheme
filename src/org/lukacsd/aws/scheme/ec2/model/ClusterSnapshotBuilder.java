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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.lukacsd.aws.scheme.AccountProperties;

import com.amazonaws.services.ec2.model.AvailabilityZone;
import com.amazonaws.services.ec2.model.AvailabilityZoneMessage;

public class ClusterSnapshotBuilder {
    private AccountProperties accountProperties;
    private Set<ZoneMetadata> zones = new HashSet<ZoneMetadata>( );
    private Set<InstanceResource> instances = new HashSet<InstanceResource>( );

    public ClusterSnapshotBuilder withAccountProperties( AccountProperties accountProperties ) {
        this.accountProperties = accountProperties;

        return this;
    }

    public ClusterSnapshotBuilder withZone( String endpoint, AvailabilityZone zone ) {
        ZoneMetadata meta = new ZoneMetadata( zone.getRegionName( ), zone.getZoneName( ), endpoint );
        meta.setState( zone.getState( ) );

        for ( AvailabilityZoneMessage message : zone.getMessages( ) ) {
            meta.addMessage( message.getMessage( ) );
        }

        zones.add( meta );

        return this;
    }

    public ClusterSnapshotBuilder withInstance( InstanceResource instance ) {
        instances.add( instance );

        return this;
    }

    public ClusterSnapshot build() {
        ClusterSnapshot retval = new ClusterSnapshot( accountProperties, new LinkedList<ZoneMetadata>( zones ),
                new LinkedList<InstanceResource>( instances ) );

        return retval;
    }

}
