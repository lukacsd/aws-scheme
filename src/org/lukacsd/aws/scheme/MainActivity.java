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

package org.lukacsd.aws.scheme;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.lukacsd.aws.scheme.ec2.Ec2Manager;
import org.lukacsd.aws.scheme.ec2.Ec2ManagerFactory;
import org.lukacsd.aws.scheme.ec2.UpdateClusterSchemeTask;
import org.lukacsd.aws.scheme.ec2.UpdateClusterSnapshotTask;
import org.lukacsd.aws.scheme.ec2.model.ClusterScheme;
import org.lukacsd.aws.scheme.ec2.model.ClusterSnapshot;
import org.lukacsd.aws.scheme.s3.S3Manager;
import org.lukacsd.aws.scheme.s3.S3ManagerFactory;
import org.lukacsd.aws.scheme.s3.UpdateBillingDataTask;
import org.lukacsd.aws.scheme.ui.JumpToHostDialogFragment;
import org.lukacsd.aws.scheme.ui.Nameable;
import org.lukacsd.aws.scheme.ui.SchemeFragment;
import org.lukacsd.aws.scheme.ui.ZoneMetaFragment;
import org.lukacsd.aws.scheme.util.ProgressUpdate;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity {
    private CharSequence mTitle;
    private ActionBar actionBar;
    private SectionsPagerAdapter pagerAdapter;
    private ViewPager viewPager;
    private HighlightingArrayAdapter drawerAdapter;
    private ListView drawerList;
    private DrawerLayout drawerLayout;
    private RelativeLayout drawerNavigator;
    private ActionBarDrawerToggle drawerToggle;
    private AccountManager awsAccountManager;
    private S3Manager s3Manager;
    private Ec2Manager ec2Manager;
    private ClusterSnapshot clusterSnapshot;
    private ActivateSchemeListener activateSchemeListener;
    private HostnameUpdateListener hostnameUpdateListener;
    private boolean lastFragmentPositionRestored;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.activity_main );

        awsAccountManager = new AccountManager( getApplicationContext( ) );

        actionBar = getSupportActionBar( );
        actionBar.setDisplayHomeAsUpEnabled( true );
        actionBar.setHomeButtonEnabled( true );

        pagerAdapter = new SectionsPagerAdapter( getSupportFragmentManager( ) );

        viewPager = ( ViewPager ) findViewById( R.id.mainViewPager );
        viewPager.setAdapter( pagerAdapter );
        viewPager.setOnPageChangeListener( new ViewPager.SimpleOnPageChangeListener( ) {
            @Override
            public void onPageSelected( int position ) {
                super.onPageSelected( position );

                clickOnDrawerItem( position );
            }
        } );

        drawerAdapter = new HighlightingArrayAdapter( this, R.layout.drawer_item );

        drawerList = ( ListView ) findViewById( R.id.nav_drawer );
        drawerList.setAdapter( drawerAdapter );
        drawerList.setDivider( null );
        drawerList.setOnItemClickListener( new ListView.OnItemClickListener( ) {
            @Override
            public void onItemClick( AdapterView<?> parent, View view, int position, long id ) {
                setTitle( pagerAdapter.getPageTitle( position ) );
                viewPager.setCurrentItem( position, false );

                drawerList.setItemChecked( position, true );
                drawerLayout.closeDrawer( drawerNavigator );
            }
        } );

        drawerLayout = ( DrawerLayout ) findViewById( R.id.mainLayout );
        drawerNavigator = ( RelativeLayout ) findViewById( R.id.navigation_drawer );

        drawerToggle = new ActionBarDrawerToggle( this, drawerLayout, R.string.drawerOpen, R.string.drawerClose ) {
            @Override
            public void onDrawerClosed( View view ) {
                actionBar.setTitle( mTitle );
            }

            @Override
            public void onDrawerOpened( View view ) {
                actionBar.setTitle( R.string.app_name );
            }
        };

        drawerLayout.setDrawerListener( drawerToggle );

        activateSchemeListener = new ActivateSchemeListener( );
        hostnameUpdateListener = new HostnameUpdateListener( );

        ArrayAdapter<String> accountAdapter = new ArrayAdapter<String>( this, android.R.layout.simple_spinner_item,
                awsAccountManager.getAccountAliasArray( ) );
        accountAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );

        Spinner accountSpinner = ( Spinner ) findViewById( R.id.drawer_header );
        accountSpinner.setAdapter( accountAdapter );
        int accountPos = awsAccountManager.getSelectedOrDefaultAccountPosition( this );
        if ( accountPos >= 0 ) {
            accountSpinner.setSelection( accountPos );
        }

        accountSpinner.setOnItemSelectedListener( new OnItemSelectedListener( ) {
            @Override
            public void onItemSelected( AdapterView<?> parent, View view, int position, long id ) {
                if ( setSelectedAccount( parent.getItemAtPosition( position ).toString( ) ) ) {
                    try {
                        setS3ManagerForSelectedOrDefaultAccount( );
                        setManagerForSelectedOrDefaultAccount( );
                        updateClusterSnapshot( ProgressDialog.show( MainActivity.this, getString( R.string.updatingClusterSnapshotMsg ),
                                null, true ) );
                    } catch ( Exception ex ) {
                        alert( ex.toString( ) );
                    }
                }

                drawerLayout.closeDrawer( drawerNavigator );
            }

            @Override
            public void onNothingSelected( AdapterView<?> parent ) {
                drawerLayout.closeDrawer( drawerNavigator );
            }
        } );

        try {
            setS3ManagerForSelectedOrDefaultAccount( );
            setManagerForSelectedOrDefaultAccount( );
            updateUiWithClusterSnapshot( ( ClusterSnapshot ) getIntent( ).getSerializableExtra(
                    Constants.CLUSTER_SNAPSHOT_UPDATE_SNAPSHOT_ARG ) );
        } catch ( Exception ex ) {
            alert( ex.toString( ) );
        }
    }

    @Override
    public void onResume() {
        super.onResume( );

        LocalBroadcastManager.getInstance( this ).registerReceiver( activateSchemeListener,
                new IntentFilter( Constants.ACTIVATE_SCHEME_BROADCAST ) );
        LocalBroadcastManager.getInstance( this ).registerReceiver( hostnameUpdateListener,
                new IntentFilter( Constants.HOSTNAME_UPDATED_BROADCAST ) );
    }

    @Override
    public void onPause() {
        super.onPause( );

        if ( activateSchemeListener != null ) {
            LocalBroadcastManager.getInstance( this ).unregisterReceiver( activateSchemeListener );
        }
        if ( hostnameUpdateListener != null ) {
            LocalBroadcastManager.getInstance( this ).unregisterReceiver( hostnameUpdateListener );
        }
        if ( viewPager != null ) {
            savePreference( Constants.LAST_FRAGMENT_POSITION, viewPager.getCurrentItem( ) );
        }
    }

    @Override
    protected void onPostCreate( Bundle savedInstanceState ) {
        super.onPostCreate( savedInstanceState );

        if ( drawerToggle != null ) {
            drawerToggle.syncState( );
        }
    }

    @Override
    public void onConfigurationChanged( Configuration newConfig ) {
        super.onConfigurationChanged( newConfig );

        drawerToggle.onConfigurationChanged( newConfig );
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        getMenuInflater( ).inflate( R.menu.main, menu );

        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        switch ( item.getItemId( ) ) {
        case ( android.R.id.home ):
            if ( drawerLayout.isDrawerOpen( drawerNavigator ) ) {
                drawerLayout.closeDrawer( drawerNavigator );
            } else {
                drawerLayout.openDrawer( drawerNavigator );
            }
            break;

        case ( R.id.menuJumpToHost ):
            Bundle args = new Bundle( );
            args.putString( Constants.SETTINGS_HOSTNAME, getSavedHostname( ) );

            JumpToHostDialogFragment dialogFragment = new JumpToHostDialogFragment( );
            dialogFragment.setArguments( args );
            dialogFragment.show( getSupportFragmentManager( ), "jumpToHost" );
            break;

        case ( R.id.menuRefreshSnapshot ):
            if ( getString( R.string.zoneMetaFragmentTitle ).equals( pagerAdapter.getPageTitle( viewPager.getCurrentItem( ) ) ) ) {
                updateBillingDataAndClusterSnapshot( ProgressDialog.show( MainActivity.this,
                        getString( R.string.updatingClusterSnapshotMsg ), "Download in progress..", true ) );
            } else {
                updateClusterSnapshot( ProgressDialog
                        .show( MainActivity.this, getString( R.string.updatingClusterSnapshotMsg ), null, true ) );
            }

            break;

        default:
            return super.onOptionsItemSelected( item );
        }

        return true;
    }

    @Override
    public void setTitle( CharSequence title ) {
        mTitle = title;
        actionBar.setTitle( title );
    }

    private void clickOnDrawerItem( int position ) {
        if ( drawerAdapter.getCount( ) > 0 && position > -1 ) {
            drawerList.performItemClick( drawerAdapter.getView( position, null, null ), position, drawerAdapter.getItemId( position ) );
        } else {
            setTitle( R.string.app_name );
        }
    }

    private void alert( String message ) {
        new AlertDialog.Builder( MainActivity.this ).setMessage( message ).setCancelable( false )
                .setNeutralButton( R.string.buttonOK, new DialogInterface.OnClickListener( ) {
                    public void onClick( DialogInterface dialog, int which ) {
                    }
                } ).create( ).show( );
    }

    private void updateBillingDataAndClusterSnapshot( final ProgressDialog progressDialog ) {
        ProgressUpdate progressUpdate = new ProgressUpdate( ) {
            @Override
            public void advance( final String msg ) {
                runOnUiThread( new Runnable( ) {
                    @Override
                    public void run() {
                        progressDialog.setMessage( msg );
                    }
                } );
            }

            @Override
            public void setMax( int max ) {
            }
        };

        final UpdateBillingDataTask task = new UpdateBillingDataTask( s3Manager, progressUpdate );
        task.setPostExecute( new Runnable( ) {
            @Override
            public void run() {
                if ( task.hasTaskException( ) ) {
                    progressDialog.dismiss( );

                    alert( task.getTaskExceptionMessage( ) );
                } else {
                    updateClusterSnapshot( progressDialog );

                    fireAccountDetailsUpdated( );
                }
            }
        } );

        task.execute( );
    }

    private void updateClusterSnapshot( final ProgressDialog progressDialog ) {
        ProgressUpdate progressUpdate = new ProgressUpdate( ) {
            @Override
            public void advance( final String msg ) {
                runOnUiThread( new Runnable( ) {
                    @Override
                    public void run() {
                        progressDialog.setMessage( msg );
                    }
                } );
            }

            @Override
            public void setMax( int max ) {
            }
        };

        final UpdateClusterSnapshotTask task = new UpdateClusterSnapshotTask( MainActivity.this, ec2Manager, progressUpdate );
        task.setPostExecute( new Runnable( ) {
            @Override
            public void run() {
                progressDialog.dismiss( );

                if ( task.hasTaskException( ) ) {
                    alert( task.getTaskExceptionMessage( ) );
                } else {
                    updateUiWithClusterSnapshot( task.getTaskResult( ) );
                }
            }
        } );

        task.execute( );
    }

    private void updateUiWithClusterSnapshot( ClusterSnapshot newSnapshot ) {
        clusterSnapshot = newSnapshot;

        pagerAdapter.setClusterSnapshot( newSnapshot );
        drawerAdapter.setClusterSnapshot( newSnapshot );

        if ( !restoreLastFragmentPosition( newSnapshot ) ) {
            clickOnDrawerItem( viewPager.getCurrentItem( ) );
        }

        fireSnapshotUpdated( newSnapshot );
    }

    private void updateClusterScheme( ClusterSnapshot oldSnapshot, String newScheme ) {
        final ProgressDialog progressDialog = ProgressDialog.show( MainActivity.this, getString( R.string.updatingClusterSchemeMsg ), null,
                true );
        ProgressUpdate progressUpdate = new ProgressUpdate( ) {
            @Override
            public void advance( final String msg ) {
                runOnUiThread( new Runnable( ) {
                    @Override
                    public void run() {
                        progressDialog.setMessage( msg );
                    }
                } );
            }

            @Override
            public void setMax( int max ) {
            }
        };

        final UpdateClusterSchemeTask task = new UpdateClusterSchemeTask( MainActivity.this, ec2Manager, progressUpdate );
        task.setPostExecute( new Runnable( ) {
            @Override
            public void run() {
                progressDialog.dismiss( );

                if ( task.hasTaskException( ) ) {
                    alert( task.getTaskExceptionMessage( ) );
                } else {
                    updateClusterSnapshot( ProgressDialog.show( MainActivity.this, getString( R.string.updatingClusterSnapshotMsg ), null,
                            true ) );
                }
            }
        } );

        task.execute( oldSnapshot, newScheme );
    }

    private boolean restoreLastFragmentPosition( ClusterSnapshot clusterSnapshot ) {
        boolean changePosition = !lastFragmentPositionRestored && clusterSnapshot != null;

        if ( changePosition ) {
            int restoredFragmentPosition = getSavedFragmentPosition( );

            if ( restoredFragmentPosition <= clusterSnapshot.getSchemeNames( ).size( ) + 1 ) {
                clickOnDrawerItem( restoredFragmentPosition );
            }

            lastFragmentPositionRestored = true;
        }

        return changePosition;
    }

    private void fireAccountDetailsUpdated() {
        Intent intent = new Intent( Constants.ACCOUNT_UPDATE_BROADCAST );

        intent.putExtra( Constants.ACCOUNT_UPDATE_PROPERTIES_ARG, ( Serializable ) awsAccountManager.getSelectedOrDefaultAccount( this ) );

        LocalBroadcastManager.getInstance( this ).sendBroadcast( intent );
    }

    private void fireSnapshotUpdated( ClusterSnapshot clusterSnapshot ) {
        Intent intent = new Intent( Constants.CLUSTER_SNAPSHOT_UPDATE_BROADCAST );

        if ( clusterSnapshot != null ) {
            intent.putExtra( Constants.CLUSTER_SNAPSHOT_UPDATE_SNAPSHOT_ARG, ( Serializable ) clusterSnapshot );
        }

        LocalBroadcastManager.getInstance( this ).sendBroadcast( intent );
    }

    private int getSavedFragmentPosition() {
        return getPreferences( MODE_PRIVATE ).getInt( Constants.LAST_FRAGMENT_POSITION, 0 );
    }

    private String getSavedHostname() {
        return getPreferences( MODE_PRIVATE ).getString( Constants.SETTINGS_HOSTNAME, "" );
    }

    private boolean setSelectedAccount( String accountName ) {
        String savedAccountName = getPreferences( MODE_PRIVATE ).getString( Constants.SETTINGS_ACCOUNT_ALIAS, "" );

        if ( !savedAccountName.equals( accountName ) && awsAccountManager.isValidAccount( accountName ) ) {
            savePreference( Constants.SETTINGS_ACCOUNT_ALIAS, accountName );
        }

        return !savedAccountName.equals( "" ) && !savedAccountName.equals( accountName );
    }

    private void setS3ManagerForSelectedOrDefaultAccount() {
        try {
            s3Manager = new S3ManagerFactory( ).createManager( getApplicationContext( ),
                    awsAccountManager.getSelectedOrDefaultAccount( this ) );
        } catch ( Exception ex ) {
            alert( "Cannot create S3 manager, billing information is not available" );
        }
    }

    private void setManagerForSelectedOrDefaultAccount() {
        ec2Manager = new Ec2ManagerFactory( ).createManager( awsAccountManager.getSelectedOrDefaultAccount( this ) );
    }

    private void savePreference( String name, int value ) {
        SharedPreferences.Editor editor = getPreferences( MODE_PRIVATE ).edit( );
        editor.putInt( name, value );
        editor.commit( );
    }

    private void savePreference( String name, String value ) {
        SharedPreferences.Editor editor = getPreferences( MODE_PRIVATE ).edit( );
        editor.putString( name, value );
        editor.commit( );
    }

    public class ActivateSchemeListener extends BroadcastReceiver {

        @Override
        public void onReceive( Context context, Intent intent ) {
            String newScheme = intent.getStringExtra( Constants.ACTIVATE_SCHEME_BROADCAST_ARG );

            updateClusterScheme( clusterSnapshot, newScheme );
        }
    }

    public class HostnameUpdateListener extends BroadcastReceiver {

        @Override
        public void onReceive( Context context, Intent intent ) {
            if ( clusterSnapshot != null ) {
                String jumpToHost = intent.getStringExtra( Constants.HOSTNAME_UPDATED_BROADCAST_ARG );

                if ( jumpToHost != null && !jumpToHost.isEmpty( ) ) {
                    ClusterScheme hostingScheme = null;

                    for ( ClusterScheme scheme : clusterSnapshot.getSchemes( ) ) {
                        if ( scheme.getDesignatedPublicIpsWithResolvedHostnames( ).values( ).contains( jumpToHost ) ) {
                            hostingScheme = scheme;

                            if ( scheme.isActive( ) ) {
                                break;
                            }
                        }
                    }

                    if ( hostingScheme == null ) {
                        alert( String.format( "No schemes seems to host [%s]", jumpToHost ) );
                    } else {
                        clickOnDrawerItem( pagerAdapter.getSchemePosition( hostingScheme ) );
                    }
                }
            }
        }
    }

    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {
        private ClusterSnapshot clusterSnapshot;
        private List<Fragment> fragments = new LinkedList<Fragment>( );

        public SectionsPagerAdapter( FragmentManager fragmentManager ) {
            super( fragmentManager );
        }

        public void setClusterSnapshot( ClusterSnapshot clusterSnapshot ) {
            if ( this.clusterSnapshot == null && clusterSnapshot != null
                    || ( this.clusterSnapshot != null && !this.clusterSnapshot.equals( clusterSnapshot ) ) ) {
                fragments.clear( );

                if ( clusterSnapshot != null ) {
                    fragments.add( new ZoneMetaFragment( getString( R.string.zoneMetaFragmentTitle ), clusterSnapshot
                            .getAccountProperties( ), clusterSnapshot.getZones( ) ) );

                    for ( ClusterScheme scheme : clusterSnapshot.getSchemes( ) ) {
                        fragments.add( new SchemeFragment( scheme ) );
                    }
                }

                notifyDataSetChanged( );
            }

            this.clusterSnapshot = clusterSnapshot;
        }

        @Override
        public Fragment getItem( int position ) {
            return fragments.get( position );
        }

        @Override
        public int getCount() {
            return fragments.size( );
        }

        @Override
        public int getItemPosition( Object item ) {
            int retval = fragments.indexOf( item );

            if ( retval == -1 ) {
                retval = POSITION_NONE;
            }

            return retval;
        }

        @Override
        public CharSequence getPageTitle( int position ) {
            return ( ( Nameable ) getItem( position ) ).getTitle( );
        }

        public int getSchemePosition( ClusterScheme scheme ) {
            int retval = POSITION_NONE;

            for ( int i = 0; i < fragments.size( ); i++ ) {
                Fragment fragment = fragments.get( i );

                if ( fragment instanceof SchemeFragment && ( ( SchemeFragment ) fragment ).getScheme( ).equals( scheme ) ) {
                    retval = i;
                    break;
                }
            }

            return retval;
        }
    }

    public class HighlightingArrayAdapter extends ArrayAdapter<String> {
        private ClusterSnapshot clusterSnapshot;

        public HighlightingArrayAdapter( Context context, int resource ) {
            super( context, resource );

            setNotifyOnChange( false );
        }

        public void setClusterSnapshot( ClusterSnapshot clusterSnapshot ) {
            if ( this.clusterSnapshot == null && clusterSnapshot != null
                    || ( this.clusterSnapshot != null && !this.clusterSnapshot.equals( clusterSnapshot ) ) ) {
                clear( );

                if ( clusterSnapshot != null ) {
                    add( getString( R.string.zoneMetaFragmentTitle ) );
                    addAll( clusterSnapshot.getSchemeNames( ) );
                }

                notifyDataSetChanged( );
            }

            this.clusterSnapshot = clusterSnapshot;
        }

        @Override
        public View getView( int position, View convertView, ViewGroup parent ) {
            View view = super.getView( position, convertView, parent );

            TextView textView = ( TextView ) view;
            if ( getActiveLabels( ).contains( textView.getText( ) ) ) {
                textView.setTextColor( getResources( ).getColor( R.color.icsYellow ) );
                textView.setTypeface( null, Typeface.BOLD );
            } else {
                textView.setTextColor( getResources( ).getColor( R.color.icsWhite ) );
                textView.setTypeface( null, Typeface.NORMAL );
            }

            return view;
        }

        private Set<String> getActiveLabels() {
            Set<String> retval = new HashSet<String>( );

            if ( clusterSnapshot != null ) {
                retval.addAll( clusterSnapshot.getActiveSchemes( ) );
            }

            return retval;
        }
    }

}
