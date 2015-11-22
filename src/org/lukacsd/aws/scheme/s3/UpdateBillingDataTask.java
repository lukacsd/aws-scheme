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

package org.lukacsd.aws.scheme.s3;

import org.joda.time.DateTime;
import org.lukacsd.aws.scheme.util.ProgressUpdate;

import android.os.AsyncTask;

public class UpdateBillingDataTask extends AsyncTask<Object, Void, Void> {
    private S3Manager s3Manager;
    private ProgressUpdate progressUpdate;
    private Exception exceptionDuringAsyncTask;
    private Runnable postExecute;

    public UpdateBillingDataTask( S3Manager s3Manager, ProgressUpdate progressUpdate ) {
        this.s3Manager = s3Manager;
        this.progressUpdate = progressUpdate;
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

    @Override
    protected Void doInBackground( Object... params ) {
        try {
            s3Manager.getAccount( ).setBillingForPreviousMonth(
                    s3Manager.getBillingForMonth( DateTime.now( ).minusMonths( 1 ).toString( "YYYY-MM" ), progressUpdate ) );
            s3Manager.getAccount( ).setBillingForCurrentMonth(
                    s3Manager.getBillingForMonth( DateTime.now( ).toString( "YYYY-MM" ), progressUpdate ) );
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

}
