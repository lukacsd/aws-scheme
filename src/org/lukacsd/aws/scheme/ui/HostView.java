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

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HostView extends LinearLayout {

    public HostView( Context context, String publicIp, String hostname, boolean active ) {
        super( context );

        LayoutInflater.from( context ).inflate( R.layout.host_view, this, true );

        LinearLayout layout = ( LinearLayout ) findViewById( R.id.hostViewLayout );

        layout.addView( textView( context ).bold( ).color( R.color.icsDarkBlue ).textMedium( ).text( hostname ).padLeft( 3 ).build( ) );
        layout.addView( textView( context ).bold( ).color( R.color.icsDarkGray ).textSmall( ).text( publicIp ).padLeft( 10 ).build( ) );

        TextView stateText = ( TextView ) findViewById( R.id.hostnameStateText );
        if ( active ) {
            stateText.setBackgroundColor( getResources( ).getColor( R.color.icsDarkGreen ) );
        } else {
            stateText.setBackgroundColor( getResources( ).getColor( R.color.icsRed ) );
        }
    }

    private TextViewBuilder textView( Context context ) {
        return new TextViewBuilder( context );
    }

}
