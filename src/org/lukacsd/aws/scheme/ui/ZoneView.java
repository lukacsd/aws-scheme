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

import org.lukacsd.aws.scheme.R;
import org.lukacsd.aws.scheme.ec2.model.ZoneMetadata;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ZoneView extends LinearLayout {

    public ZoneView( Context context, ZoneMetadata model ) {
        super( context );

        LayoutInflater.from( context ).inflate( R.layout.zone_view, this, true );

        LinearLayout layout = ( LinearLayout ) findViewById( R.id.zoneViewLayout );

        layout.addView( textView( context ).bold( ).color( R.color.icsDarkBlue ).textMedium( ).text( model.getZoneName( ) ).padLeft( 3 )
                .build( ) );
        layout.addView( textView( context ).italic( ).color( R.color.icsDarkGray ).textSmall( )
                .text( String.format( "Region [%s]", model.getRegionName( ) ) ).padLeft( 10 ).build( ) );
        layout.addView( textView( context ).italic( ).color( R.color.icsGray ).textSmall( )
                .text( String.format( "Endpoint [%s]", model.getEndpoint( ) ) ).padLeft( 10 ).build( ) );

        for ( String msg : model.getMessages( ) ) {
            layout.addView( textView( context ).italic( ).color( R.color.icsGray ).textSmall( ).text( String.format( "* %s *", msg ) )
                    .padLeft( 10 ).build( ) );
        }

        TextView stateText = ( TextView ) findViewById( R.id.zoneStateText );
        switch ( model.getState( ) ) {
        case AVAILABLE:
            stateText.setBackgroundColor( getResources( ).getColor( R.color.icsDarkGreen ) );
            break;
        default:
            stateText.setBackgroundColor( getResources( ).getColor( R.color.icsRed ) );
            break;
        }
    }

    private TextViewBuilder textView( Context context ) {
        return new TextViewBuilder( context );
    }

}
