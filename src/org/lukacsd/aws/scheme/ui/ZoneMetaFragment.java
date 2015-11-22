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
import java.util.Collection;

import org.lukacsd.aws.scheme.AccountProperties;
import org.lukacsd.aws.scheme.Constants;
import org.lukacsd.aws.scheme.R;
import org.lukacsd.aws.scheme.ec2.model.ClusterSnapshot;
import org.lukacsd.aws.scheme.ec2.model.ZoneMetadata;
import org.lukacsd.aws.scheme.s3.model.BillingData;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ZoneMetaFragment extends Fragment implements Nameable {
    private static final String NO_BILLING_LABEL = "-";

    private String title;
    private String accountId;
    private String accountAlias;
    private String monthToDate;
    private String previousMonth;
    private Collection<ZoneMetadata> zones;
    private ClusterSnapshotUpdateListener clusterSnapshotUpdateListener;
    private AccountDetailUpdateListener accountDetailUpdateListener;

    public ZoneMetaFragment( ) {
    }

    public ZoneMetaFragment( String title, AccountProperties account, Collection<ZoneMetadata> zones ) {
        this.title = title;
        this.accountId = account.getAccountId( );
        this.accountAlias = account.getAccountAlias( );
        this.monthToDate = getBillingDataAsString( account.getBillingForCurrentMonth( ) );
        this.previousMonth = getBillingDataAsString( account.getBillingForPreviousMonth( ) );
        this.zones = zones;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
        View retval = inflater.inflate( R.layout.zonemeta_scheme, container, false );

        clusterSnapshotUpdateListener = new ClusterSnapshotUpdateListener( );
        accountDetailUpdateListener = new AccountDetailUpdateListener( );

        if ( savedInstanceState != null ) {
            accountId = savedInstanceState.getString( "accountId" );
            accountAlias = savedInstanceState.getString( "accountAlias" );
            monthToDate = savedInstanceState.getString( "monthToDate" );
            previousMonth = savedInstanceState.getString( "previousMonth" );
            zones = ( Collection<ZoneMetadata> ) savedInstanceState.getSerializable( "zones" );
        }

        updateUi( getActivity( ), retval );

        return retval;
    }

    @Override
    public void onResume() {
        super.onResume( );

        LocalBroadcastManager.getInstance( getActivity( ) ).registerReceiver( clusterSnapshotUpdateListener,
                new IntentFilter( Constants.CLUSTER_SNAPSHOT_UPDATE_BROADCAST ) );
        LocalBroadcastManager.getInstance( getActivity( ) ).registerReceiver( accountDetailUpdateListener,
                new IntentFilter( Constants.ACCOUNT_UPDATE_BROADCAST ) );
    }

    @Override
    public void onPause() {
        super.onPause( );

        LocalBroadcastManager.getInstance( getActivity( ) ).unregisterReceiver( clusterSnapshotUpdateListener );
        LocalBroadcastManager.getInstance( getActivity( ) ).unregisterReceiver( accountDetailUpdateListener );
    }

    @Override
    public void onSaveInstanceState( Bundle outState ) {
        super.onSaveInstanceState( outState );

        if ( zones != null ) {
            outState.putString( "accountId", accountId );
            outState.putString( "accountAlias", accountAlias );
            outState.putString( "monthToDate", monthToDate );
            outState.putString( "previousMonth", previousMonth );
            outState.putSerializable( "zones", ( Serializable ) zones );
        }
    }

    @SuppressLint( "DefaultLocale" )
    private String getBillingDataAsString( BillingData billingData ) {
        if ( billingData == null || billingData.getCcy( ) == null && billingData.getTotal( ) == null && billingData.getTax( ) == null ) {
            return NO_BILLING_LABEL;
        }

        String ccy = billingData.getCcy( ) == null ? "???" : billingData.getCcy( );
        Double total = billingData.getTotal( ) == null ? 0 : billingData.getTotal( );
        Double tax = billingData.getTax( ) == null ? 0 : billingData.getTax( );

        return String.format( "%s%.2f", ccy, total + tax );
    }

    private void updateUi( Context context, View view ) {
        LinearLayout layout = ( LinearLayout ) view.findViewById( R.id.zoneMetaLayout );

        layout.removeAllViews( );

        layout.addView( textView( context ).textSmall( ).text( R.string.accountParagraphTitle ).boldItalic( ).build( ) );
        layout.addView( getDivider( context ) );
        layout.addView( getKeyValueTextLayout( context, textView( context ).text( R.string.accountParagraphId ).padLeft( 10 ).build( ),
                textView( context ).text( accountId ).bold( ).build( ) ) );
        layout.addView( getKeyValueTextLayout( context, textView( context ).text( R.string.accountParagraphAlias ).padLeft( 10 ).build( ),
                textView( context ).text( accountAlias ).bold( ).build( ) ) );

        layout.addView( textView( context ).textSmall( ).text( R.string.billingParagraphTitle ).boldItalic( ).padTop( 50 ).build( ) );
        layout.addView( getDivider( context ) );
        layout.addView( getKeyValueTextLayout( context, textView( context ).text( R.string.billingParagraphMontToDate ).padLeft( 10 )
                .build( ), textView( context ).text( monthToDate ).bold( ).color( R.color.icsYellow ).build( ) ) );
        layout.addView( getKeyValueTextLayout( context, textView( context ).text( R.string.billingParagraphPreviousMonth ).padLeft( 10 )
                .italic( ).build( ), textView( context ).text( previousMonth ).italic( ).build( ) ) );

        layout.addView( textView( context ).textSmall( ).text( R.string.regionParagraphTitle ).boldItalic( ).padTop( 50 ).build( ) );
        layout.addView( getDivider( context ) );

        if ( zones != null ) {
            for ( ZoneMetadata zone : zones ) {
                layout.addView( new ZoneView( context, zone ) );
            }
        }
    }

    private View getDivider( Context context ) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, 2 );
        lp.setMargins( 0, 3, 0, 15 );

        View divider = new View( context );
        divider.setLayoutParams( lp );
        divider.setBackgroundColor( getResources( ).getColor( R.color.icsGray ) );

        return divider;
    }

    private View getKeyValueTextLayout( Context context, TextView keyText, TextView valueText ) {
        RelativeLayout retval = new RelativeLayout( context );
        retval.setLayoutParams( new RelativeLayout.LayoutParams( LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT ) );

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams( LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT );

        layoutParams.addRule( RelativeLayout.ALIGN_PARENT_LEFT );
        retval.addView( keyText, layoutParams );

        layoutParams = new RelativeLayout.LayoutParams( LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT );
        layoutParams.addRule( RelativeLayout.ALIGN_PARENT_RIGHT );

        retval.addView( valueText, layoutParams );

        return retval;
    }

    private TextViewBuilder textView( Context context ) {
        return new TextViewBuilder( context );
    }

    public class ClusterSnapshotUpdateListener extends BroadcastReceiver {

        @Override
        public void onReceive( Context context, Intent intent ) {
            ClusterSnapshot updatedSnapshot = ( ClusterSnapshot ) intent
                    .getSerializableExtra( Constants.CLUSTER_SNAPSHOT_UPDATE_SNAPSHOT_ARG );

            if ( updatedSnapshot == null ) {
                accountId = "";
                accountAlias = "";
                zones = null;
            } else {
                accountId = updatedSnapshot.getAccountProperties( ).getAccountId( );
                accountAlias = updatedSnapshot.getAccountProperties( ).getAccountAlias( );
                monthToDate = getBillingDataAsString( updatedSnapshot.getAccountProperties( ).getBillingForCurrentMonth( ) );
                previousMonth = getBillingDataAsString( updatedSnapshot.getAccountProperties( ).getBillingForPreviousMonth( ) );
                zones = updatedSnapshot.getZones( );
            }

            updateUi( getActivity( ), getView( ) );
        }
    }

    public class AccountDetailUpdateListener extends BroadcastReceiver {

        @Override
        public void onReceive( Context context, Intent intent ) {
            AccountProperties account = ( AccountProperties ) intent.getSerializableExtra( Constants.ACCOUNT_UPDATE_PROPERTIES_ARG );

            if ( account != null ) {
                monthToDate = getBillingDataAsString( account.getBillingForCurrentMonth( ) );
                previousMonth = getBillingDataAsString( account.getBillingForPreviousMonth( ) );
            }

            updateUi( getActivity( ), getView( ) );
        }
    }

}
