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

import org.lukacsd.aws.scheme.Constants;
import org.lukacsd.aws.scheme.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.EditText;

@SuppressLint( "DefaultLocale" )
public class JumpToHostDialogFragment extends DialogFragment {
    private String editedHostname;

    @Override
    public Dialog onCreateDialog( Bundle savedInstanceState ) {
        final Activity activity = getActivity( );
        final String oldHostname = getArguments( ).getString( Constants.SETTINGS_HOSTNAME ).toLowerCase( );
        final EditText hostnameText = new EditText( activity );
        hostnameText.setText( oldHostname );

        AlertDialog.Builder builder = new AlertDialog.Builder( activity );
        builder.setTitle( R.string.dialogJumpToHost );
        builder.setView( hostnameText );
        builder.setPositiveButton( R.string.buttonOK, new DialogInterface.OnClickListener( ) {
            @Override
            public void onClick( DialogInterface dialog, int id ) {
                editedHostname = hostnameText.getText( ).toString( ).toLowerCase( );
            }
        } );
        builder.setNegativeButton( R.string.buttonCancel, new DialogInterface.OnClickListener( ) {
            @Override
            public void onClick( DialogInterface dialog, int id ) {
                dialog.cancel( );
            }
        } );

        return builder.create( );
    }

    @Override
    public void onStop() {
        super.onStop( );

        Intent intent = new Intent( Constants.HOSTNAME_UPDATED_BROADCAST );
        intent.putExtra( Constants.HOSTNAME_UPDATED_BROADCAST_ARG, editedHostname );
        LocalBroadcastManager.getInstance( getActivity( ) ).sendBroadcast( intent );
    }

}
