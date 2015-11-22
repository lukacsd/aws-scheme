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

import org.lukacsd.aws.scheme.R;
import org.lukacsd.aws.scheme.ec2.model.ClusterSnapshot;
import org.lukacsd.aws.scheme.util.ProgressUpdate;
import org.lukacsd.aws.scheme.util.StringBundle;

import android.content.Context;
import android.os.AsyncTask;

public class UpdateClusterSchemeTask extends AsyncTask<Object, Void, Void> {
    private Ec2Manager ec2Manager;
    private ProgressUpdate progressUpdate;
    private StringBundle stringBundle;
    private Runnable postExecute;
    private Exception exceptionDuringAsyncTask;

    public UpdateClusterSchemeTask( Context context, Ec2Manager ec2Manager, ProgressUpdate progressUpdate ) {
        this.ec2Manager = ec2Manager;
        this.progressUpdate = progressUpdate;
        this.stringBundle = getStringBundle( context );
    }

    public void setPostExecute( Runnable postExecute ) {
        this.postExecute = postExecute;
    }

    public Exception getTaskException() {
        return exceptionDuringAsyncTask;
    }

    public boolean hasTaskException() {
        return exceptionDuringAsyncTask != null;
    }

    public String getTaskExceptionMessage() {
        return exceptionDuringAsyncTask == null ? null : exceptionDuringAsyncTask.getMessage( );
    }

    public AsyncTask<Object, Void, Void> execute( ClusterSnapshot oldSnapshot, String newScheme ) {
        return super.execute( oldSnapshot, newScheme );
    }

    @Override
    protected Void doInBackground( Object... params ) {
        ClusterSnapshot oldSnapshot = ( ClusterSnapshot ) params[ 0 ];
        String newScheme = ( String ) params[ 1 ];

        try {
            ec2Manager.setActiveScheme( stringBundle, oldSnapshot, newScheme, progressUpdate );
        } catch ( Exception e ) {
            exceptionDuringAsyncTask = e;
        }

        return null;
    }

    @Override
    protected void onPostExecute( Void result ) {
        super.onPostExecute( result );

        if ( postExecute != null ) {
            postExecute.run( );
        }
    }

    private StringBundle getStringBundle( final Context context ) {
        return new StringBundle( ) {
            @Override
            public String getString( Enum<?> key ) {
                switch ( key.ordinal( ) ) {
                case 0:
                    return context.getString( R.string.setActiveRoleStep1 );
                case 1:
                    return context.getString( R.string.setActiveRoleStep2 );
                case 2:
                    return context.getString( R.string.setActiveRoleStep3 );
                default:
                    throw new IllegalArgumentException( );
                }
            }
        };
    }

}
