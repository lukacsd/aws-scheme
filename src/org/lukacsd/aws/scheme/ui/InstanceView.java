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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.lukacsd.aws.scheme.R;
import org.lukacsd.aws.scheme.ec2.model.InstanceResource;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class InstanceView extends LinearLayout {

    public InstanceView( Context context, InstanceResource model ) {
        super( context );

        LayoutInflater.from( context ).inflate( R.layout.instance_view, this, true );

        LinearLayout layout = ( LinearLayout ) findViewById( R.id.instanceViewLayout );

        layout.addView( textView( context ).bold( ).color( R.color.icsDarkBlue ).textMedium( ).text( model.getName( ) ).padLeft( 3 )
                .build( ) );
        LinearLayout idLayout = new LinearLayout( context );
        idLayout.setOrientation( HORIZONTAL );
        layout.addView( idLayout );
        idLayout.addView( textView( context ).bold( ).color( R.color.icsDarkGray ).textMedium( ).text( model.getInstanceId( ) )
                .padLeft( 10 ).build( ) );
        idLayout.addView( textView( context ).italic( ).color( R.color.icsGray ).textSmall( )
                .text( String.format( "%s, %s, %s", model.getInstanceType( ), model.getArchitecture( ), model.getRootDeviceType( ) ) )
                .padLeft( 10 ).build( ) );
        if ( model.getPublicIps( ).size( ) > 0 ) {
            layout.addView( textView( context ).italic( ).color( R.color.icsDarkGray ).textSmall( )
                    .text( String.format( "PublicIp %s", model.getPublicIps( ) ) ).padLeft( 10 ).build( ) );
        }
        layout.addView( textView( context ).italic( ).color( R.color.icsGray ).textSmall( )
                .text( String.format( "Placement [%s]", getPlacementZone( model ) ) ).padLeft( 10 ).build( ) );
        layout.addView( textView( context ).italic( ).color( R.color.icsGray ).textSmall( )
                .text( String.format( "Network [%s]", model.getVpcId( ) ) ).padLeft( 10 ).build( ) );
        layout.addView( textView( context ).italic( ).color( R.color.icsGray ).textSmall( )
                .text( String.format( "Groups %s", getGroupNames( model ) ) ).padLeft( 10 ).build( ) );
        layout.addView( textView( context ).italic( ).color( R.color.icsGray ).textSmall( )
                .text( String.format( "Launch [%s]", getLaunchTime( model ) ) ).padLeft( 10 ).build( ) );

        TextView stateText = ( TextView ) findViewById( R.id.instanceStateText );
        switch ( model.getState( ) ) {
        case RUNNING:
            stateText.setBackgroundColor( getResources( ).getColor( R.color.icsDarkGreen ) );
            break;
        case STOPPED:
            stateText.setBackgroundColor( getResources( ).getColor( R.color.icsRed ) );
            break;
        case TERMINATED:
            stateText.setBackgroundColor( getResources( ).getColor( R.color.icsBlack ) );
            break;
        default:
            stateText.setBackgroundColor( getResources( ).getColor( R.color.icsYellow ) );
            break;
        }
    }

    private List<String> getGroupNames( InstanceResource model ) {
        List<String> retval = model.getSecurityGroups( );

        if ( retval.isEmpty( ) ) {
            retval = new LinkedList<String>( Arrays.asList( "none" ) );
        }

        return retval;
    }

    private CharSequence getLaunchTime( InstanceResource model ) {
        CharSequence retval = "unknown";

        if ( model.getLaunchTime( ) != null ) {
            retval = DateFormat.format( "yyyy-MM-dd hh:mm:ss", model.getLaunchTime( ) );
        }

        return retval;
    }

    private CharSequence getPlacementZone( InstanceResource model ) {
        CharSequence retval = "unknown";

        if ( model.getPlacementZone( ) != null ) {
            retval = model.getPlacementZone( );
        }

        return retval;
    }

    private TextViewBuilder textView( Context context ) {
        return new TextViewBuilder( context );
    }

}
