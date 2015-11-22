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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.lukacsd.aws.scheme.AccountProperties;
import org.lukacsd.aws.scheme.s3.model.BillingData;
import org.lukacsd.aws.scheme.util.ProgressUpdate;

import android.content.Context;
import android.net.Uri;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

public class S3Manager {
    private static final String BILLING_FILENAME_SKELETON = "%s-aws-billing-csv-%s.csv";
    private static final String STATEMENT_TOTAL_TAG = "StatementTotal";

    private AccountProperties properties;
    private CustomTransferUtility transfer;

    public S3Manager( Context context, AccountProperties properties ) {
        this.properties = properties;
        this.transfer = getS3TransferClient( context, properties );
    }

    public AccountProperties getAccount() {
        return properties;
    }

    private CustomTransferUtility getS3TransferClient( Context context, AccountProperties properties ) {
        AmazonS3 s3 = new AmazonS3Client( properties );
        s3.setRegion( Region.getRegion( Regions.fromName( properties.getS3Region( ) ) ) );

        return new CustomTransferUtility( s3, context, properties );
    }

    public BillingData getBillingForMonth( String month, ProgressUpdate progress ) {
        BillingData retval = null;
        File download = null;

        try {
            download = transfer.downloadBillingData( month, String.format( BILLING_FILENAME_SKELETON, properties.getAccountId( ), month ),
                    progress );

            retval = parseBillingData( download );
        } finally {
            if ( download != null ) {
                download.delete( );
            }
        }

        return retval;
    }

    private BillingData parseBillingData( File billingFile ) {
        BillingData retval = null;
        BufferedReader br = null;

        try {
            try {
                br = new BufferedReader( new FileReader( billingFile ) );

                String line = null;
                outer: while ( ( line = br.readLine( ) ) != null ) {
                    String[ ] split = line.replaceAll( "\"", "" ).split( "," );
                    for ( String field : split ) {
                        if ( STATEMENT_TOTAL_TAG.equals( field ) ) {
                            retval = new BillingData( billingFile.getName( ), split );

                            break outer;
                        }
                    }
                }
            } finally {
                if ( br != null ) {
                    br.close( );
                }
            }
        } catch ( Exception e ) {
            throw new IllegalArgumentException( "Could not parse billing data", e );
        }

        return retval;
    }

    public class CustomTransferUtility extends TransferUtility {
        private Context context;
        private String billingBucket;

        public CustomTransferUtility( AmazonS3 s3, Context context, AccountProperties properties ) {
            super( s3, context );

            if ( properties.getBillingBucket( ) == null || properties.getBillingBucket( ).isEmpty( ) ) {
                throw new IllegalArgumentException( "no billing bucket defined" );
            }

            this.billingBucket = properties.getBillingBucket( );
            this.context = context;
        }

        public File downloadBillingData( String month, String billingFilename, ProgressUpdate progress ) {
            File retval = null;

            try {
                retval = getTempFile( context, billingFilename );

                SyncFileTransferListener transferListener = new SyncFileTransferListener( month, progress );
                TransferObserver observer = transfer.download( billingBucket, billingFilename, retval );
                observer.setTransferListener( transferListener );

                transferListener.blockUntilFinished( );
            } catch ( Exception e ) {
                throw new IllegalArgumentException( "Could not download billing data", e );
            }

            return retval;
        }

        private File getTempFile( Context context, String url ) throws IOException {
            String fileName = Uri.parse( url ).getLastPathSegment( );

            return File.createTempFile( fileName, null, context.getCacheDir( ) );
        }
    }

    public class SyncFileTransferListener implements TransferListener {
        private String filename;
        private ProgressUpdate progress;
        private TransferState state;

        public SyncFileTransferListener( String filename, ProgressUpdate progress ) {
            this.filename = filename;
            this.progress = progress;
        }

        public TransferState blockUntilFinished() {
            while ( state == null || TransferState.IN_PROGRESS.equals( state ) ) {
                try {
                    Thread.sleep( 500 );
                } catch ( InterruptedException e ) {
                }
            }

            return state;
        }

        @Override
        public void onStateChanged( int id, TransferState state ) {
            this.state = state;

            synchronized ( this ) {
                notify( );
            }
        }

        @Override
        public void onProgressChanged( int id, long bytesCurrent, long bytesTotal ) {
            progress.advance( String.format( "Download in progress..\n%s%% of %s", ( int ) ( bytesCurrent / bytesTotal * 100 ), filename ) );
        }

        @Override
        public void onError( int id, Exception ex ) {
            synchronized ( this ) {
                notify( );
            }
        }
    }

}
