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

public enum InstanceStatusType {
    PENDING( 0 ), RUNNING( 16 ), SHUTTING_DOWN( 32 ), TERMINATED( 48 ), STOPPING( 64 ), STOPPED( 80 );

    private int code;

    private InstanceStatusType( int code ) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static InstanceStatusType forCode( int code ) {
        InstanceStatusType retval = null;

        switch ( code ) {
        case 0:
            retval = PENDING;
            break;
        case 16:
            retval = RUNNING;
            break;
        case 32:
            retval = SHUTTING_DOWN;
            break;
        case 48:
            retval = TERMINATED;
            break;
        case 64:
            retval = STOPPING;
            break;
        case 80:
            retval = STOPPED;
            break;
        default:
            throw new IllegalArgumentException( );
        }

        return retval;
    }

}
