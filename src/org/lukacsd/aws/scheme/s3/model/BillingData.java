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

package org.lukacsd.aws.scheme.s3.model;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BillingData implements Serializable {
    private static final long serialVersionUID = 2979002773690118231L;
    private static final String BILLING_FILENAME_REGEX = "^[0-9]*-aws-billing-csv-([0-9]{4}-[0-9]{2}).csv.*$";

    private String date;
    private String ccy;
    private Double total;
    private Double tax;

    public BillingData( String filename, String[ ] split ) {
        this.date = getDateFromFileName( filename );
        this.ccy = split[ 23 ];
        this.total = Double.valueOf( split[ 24 ] );
        this.tax = Double.valueOf( split[ 26 ] );

        if ( date == null || ccy == null || total == null || tax == null ) {
            throw new IllegalArgumentException( String.format( "Some values missing: filename[%s], line[%s]", filename, split ) );
        }
    }

    private String getDateFromFileName( String filename ) {
        Pattern p = Pattern.compile( BILLING_FILENAME_REGEX );
        Matcher m = p.matcher( filename );
        if ( m.matches( ) ) {
            return m.group( 1 );
        }
        return null;
    }

    public String getDate() {
        return date;
    }

    public String getCcy() {
        return ccy;
    }

    public Double getTotal() {
        return total;
    }

    public Double getTax() {
        return tax;
    }

    @Override
    public String toString() {
        return "BillingData [date=" + date + ", ccy=" + ccy + ", total=" + total + ", tax=" + tax + "]";
    }
}