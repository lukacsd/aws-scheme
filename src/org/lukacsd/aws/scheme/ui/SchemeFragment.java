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

package org.lukacsd.aws.scheme.ui;

import java.io.Serializable;
import java.util.Map;

import org.lukacsd.aws.scheme.Constants;
import org.lukacsd.aws.scheme.R;
import org.lukacsd.aws.scheme.ec2.model.ClusterScheme;
import org.lukacsd.aws.scheme.ec2.model.ClusterSnapshot;
import org.lukacsd.aws.scheme.ec2.model.InstanceResource;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.Switch;

public class SchemeFragment extends Fragment implements Nameable {
    private String name;
    private ClusterScheme scheme;
    private Switch activateSwitch;
    private ClusterSnapshotUpdateListener clusterSnapshotUpdateListener;
    private ActivateSwitchClickListener activateSwitchClickListener;
    private ActivateSwitchCheckedChangeListener activateSwitchCheckedChangeListener;

    public SchemeFragment( ) {
    }

    public SchemeFragment( ClusterScheme scheme ) {
        this.name = scheme.getName( );
        this.scheme = scheme;
    }

    @Override
    public String getTitle() {
        return name;
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
        View retval = inflater.inflate( R.layout.fragment_scheme, container, false );

        clusterSnapshotUpdateListener = new ClusterSnapshotUpdateListener( );
        activateSwitchClickListener = new ActivateSwitchClickListener( );
        activateSwitchCheckedChangeListener = new ActivateSwitchCheckedChangeListener( );

        if ( savedInstanceState != null ) {
            name = savedInstanceState.getString( "name" );
            scheme = ( ClusterScheme ) savedInstanceState.getSerializable( "scheme" );
        }

        activateSwitch = ( Switch ) retval.findViewById( R.id.activateSchemeSwitch );
        activateSwitch.setLongClickable( false );
        activateSwitch.setOnClickListener( activateSwitchClickListener );
        activateSwitch.setOnCheckedChangeListener( activateSwitchCheckedChangeListener );

        updateUi( getActivity( ), retval );

        return retval;
    }

    @Override
    public void onResume() {
        super.onResume( );

        LocalBroadcastManager.getInstance( getActivity( ) ).registerReceiver( clusterSnapshotUpdateListener,
                new IntentFilter( Constants.CLUSTER_SNAPSHOT_UPDATE_BROADCAST ) );
    }

    @Override
    public void onPause() {
        super.onPause( );

        LocalBroadcastManager.getInstance( getActivity( ) ).unregisterReceiver( clusterSnapshotUpdateListener );
    }

    @Override
    public void onSaveInstanceState( Bundle outState ) {
        super.onSaveInstanceState( outState );

        outState.putString( "name", name );
        outState.putSerializable( "scheme", ( Serializable ) scheme );
    }

    public ClusterScheme getScheme() {
        return scheme;
    }

    private boolean isSchemeActive() {
        boolean retval = false;

        if ( scheme != null ) {
            retval = scheme.isActive( );
        }

        return retval;
    }

    private void silentlyAdjustSwitchToSchemeState() {
        activateSwitch.setOnCheckedChangeListener( null );
        activateSwitch.setChecked( isSchemeActive( ) );
        activateSwitch.setOnCheckedChangeListener( activateSwitchCheckedChangeListener );
    }

    private void updateUi( Context context, View view ) {
        LinearLayout layout = ( LinearLayout ) view.findViewById( R.id.instanceLayout );

        layout.removeAllViews( );

        if ( scheme != null ) {
            Map<String, String> designatedPublicIps = scheme.getDesignatedPublicIpsWithResolvedHostnames( );

            for ( InstanceResource instance : scheme.getInstances( ) ) {
                for ( String designatedPublicIp : instance.getDesignatedPublicIps( ) ) {
                    layout.addView( new HostView( context, designatedPublicIp, designatedPublicIps.get( designatedPublicIp ), instance
                            .isIpOwned( designatedPublicIp ) ) );
                }

                layout.addView( new InstanceView( context, instance ) );
            }
        }

        silentlyAdjustSwitchToSchemeState( );
    }

    public class ClusterSnapshotUpdateListener extends BroadcastReceiver {

        @Override
        public void onReceive( Context context, Intent intent ) {
            ClusterSnapshot updatedSnapshot = ( ClusterSnapshot ) intent
                    .getSerializableExtra( Constants.CLUSTER_SNAPSHOT_UPDATE_SNAPSHOT_ARG );

            if ( updatedSnapshot == null ) {
                scheme = null;
            } else {
                scheme = updatedSnapshot.getScheme( name );
            }

            updateUi( getActivity( ), getView( ) );
        }
    }

    public class ActivateSwitchClickListener implements OnClickListener {

        @Override
        public void onClick( View v ) {
            silentlyAdjustSwitchToSchemeState( );

            if ( !isSchemeActive( ) ) {
                AlertDialog.Builder builder = new AlertDialog.Builder( getActivity( ) );
                builder.setMessage( getString( R.string.activateSchemeMessage, scheme.getName( ) ) );
                builder.setPositiveButton( R.string.buttonOK, new DialogInterface.OnClickListener( ) {
                    @Override
                    public void onClick( DialogInterface dialog, int id ) {
                        Intent intent = new Intent( Constants.ACTIVATE_SCHEME_BROADCAST );
                        intent.putExtra( Constants.ACTIVATE_SCHEME_BROADCAST_ARG, scheme.getName( ) );
                        LocalBroadcastManager.getInstance( getActivity( ) ).sendBroadcast( intent );
                    }
                } );
                builder.setNegativeButton( R.string.buttonCancel, new DialogInterface.OnClickListener( ) {
                    @Override
                    public void onClick( DialogInterface dialog, int id ) {
                    }
                } );
                builder.show( );
            }
        }
    }

    public class ActivateSwitchCheckedChangeListener implements OnCheckedChangeListener {

        @Override
        public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
            silentlyAdjustSwitchToSchemeState( );
        }
    }

}
