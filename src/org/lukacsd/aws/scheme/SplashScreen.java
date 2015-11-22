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

import org.lukacsd.aws.scheme.ec2.Ec2Manager;
import org.lukacsd.aws.scheme.ec2.Ec2ManagerFactory;
import org.lukacsd.aws.scheme.ec2.UpdateClusterSnapshotTask;
import org.lukacsd.aws.scheme.util.ProgressUpdate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.ProgressBar;

public class SplashScreen extends Activity {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.activity_splash );

        if ( !isNetworkConnected( ) ) {
            alertThenTerminate( getString( R.string.networkConnectionRequiredMsg ) );

            return;
        }

        updateClusterSnapshotAndStartMain( );
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = ( ConnectivityManager ) getSystemService( Context.CONNECTIVITY_SERVICE );

        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo( );

        return activeNetworkInfo != null && activeNetworkInfo.isConnected( );
    }

    private void updateClusterSnapshotAndStartMain() {
        final ProgressBar progressBar = ( ProgressBar ) findViewById( R.id.splashPogress );
        ProgressUpdate progressUpdate = new ProgressUpdate( ) {
            private int passes = 0;

            @Override
            public void advance( final String msg ) {
                runOnUiThread( new Runnable( ) {
                    @Override
                    public void run() {
                        progressBar.setProgress( ++passes );
                    }
                } );
            }

            @Override
            public void setMax( int max ) {
                progressBar.setMax( max );
            }
        };

        AccountManager awsAccountManager = new AccountManager( getApplicationContext( ) );
        Ec2Manager ec2Manager = new Ec2ManagerFactory( ).createManager( awsAccountManager.getSelectedOrDefaultAccount( this ) );

        final UpdateClusterSnapshotTask task = new UpdateClusterSnapshotTask( SplashScreen.this, ec2Manager, progressUpdate );
        task.setPostExecute( new Runnable( ) {
            @Override
            public void run() {
                if ( task.hasTaskException( ) ) {
                    alertThenTerminate( task.getTaskExceptionMessage( ) );
                } else {
                    Intent i = new Intent( SplashScreen.this, MainActivity.class );
                    i.putExtra( Constants.CLUSTER_SNAPSHOT_UPDATE_SNAPSHOT_ARG, task.getTaskResult( ) );
                    startActivity( i );
                    finish( );
                }
            }
        } );

        task.execute( );
    }

    private void alertThenTerminate( final String message ) {
        runOnUiThread( new Runnable( ) {
            @Override
            public void run() {
                new AlertDialog.Builder( SplashScreen.this ).setMessage( message ).setCancelable( false )
                        .setNeutralButton( R.string.buttonOK, new DialogInterface.OnClickListener( ) {
                            public void onClick( DialogInterface dialog, int which ) {
                                SplashScreen.this.finish( );
                            }
                        } ).create( ).show( );
            }
        } );
    }

}
