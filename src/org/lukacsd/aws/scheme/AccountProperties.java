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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.lukacsd.aws.scheme.s3.model.BillingData;

import com.amazonaws.auth.AWSCredentials;

public class AccountProperties extends Properties implements AWSCredentials {
    private static final long serialVersionUID = -1113381418575724253L;

    private BillingData previousMonth;
    private BillingData currentMonth;

    public AccountProperties( InputStream stream ) throws IOException {
        load( stream );
    }

    public BillingData getBillingForPreviousMonth() {
        return previousMonth;
    }

    public void setBillingForPreviousMonth( BillingData previousMonth ) {
        this.previousMonth = previousMonth;
    }

    public BillingData getBillingForCurrentMonth() {
        return currentMonth;
    }

    public void setBillingForCurrentMonth( BillingData currentMonth ) {
        this.currentMonth = currentMonth;
    }

    public String getAccountId() {
        return getProperty( Constants.KEY_AWS_ACCOUNT_ID );
    }

    public String getAccountAlias() {
        return getProperty( Constants.KEY_AWS_ACCOUNT_ALIAS );
    }

    @Override
    public String getAWSAccessKeyId() {
        return getProperty( Constants.KEY_ACCESS_KEY );
    }

    @Override
    public String getAWSSecretKey() {
        return getProperty( Constants.KEY_SECRET_KEY );
    }

    public String getEc2Region() {
        return getProperty( Constants.KEY_EC2_REGION );
    }

    public String getS3Region() {
        return getProperty( Constants.KEY_S3_REGION );
    }

    public String getBillingBucket() {
        return getProperty( Constants.KEY_S3_BILLING_BUCKET );
    }

}