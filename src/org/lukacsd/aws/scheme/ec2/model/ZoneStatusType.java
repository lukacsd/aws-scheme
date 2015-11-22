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

import android.annotation.SuppressLint;

public enum ZoneStatusType {
    AVAILABLE;

    @SuppressLint( "DefaultLocale" )
    public static ZoneStatusType forCode( String code ) {
        ZoneStatusType retval = null;

        if ( code != null && "available".equals( code.trim( ).toLowerCase( ) ) ) {
            retval = AVAILABLE;
        }

        if ( retval == null ) {
            throw new IllegalArgumentException( );
        }

        return retval;
    }

}
