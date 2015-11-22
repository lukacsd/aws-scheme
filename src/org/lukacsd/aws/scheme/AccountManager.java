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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;

public class AccountManager {
    private Map<String, AccountProperties> accountAliasMap = new HashMap<String, AccountProperties>( );

    public AccountManager( Context context ) {
        init( context );
    }

    public AccountProperties getSelectedOrDefaultAccount( Activity activity ) {
        String accountName = activity.getPreferences( Activity.MODE_PRIVATE ).getString( Constants.SETTINGS_ACCOUNT_ALIAS, null );

        if ( accountName == null ) {
            accountName = getAccountAliasArray( )[ 0 ];
        }

        return getAccount( accountName );
    }

    public int getSelectedOrDefaultAccountPosition( Activity activity ) {
        return getAccountAliasList( ).indexOf( getSelectedOrDefaultAccount( activity ).getAccountAlias( ) );
    }

    public List<String> getAccountAliasList() {
        List<String> retval = new LinkedList<String>( accountAliasMap.keySet( ) );

        Collections.sort( retval );

        return retval;
    }

    public String[ ] getAccountAliasArray() {
        String[ ] retval = accountAliasMap.keySet( ).toArray( new String[ 0 ] );

        Arrays.sort( retval );

        return retval;
    }

    public AccountProperties getAccount( String accountAlias ) {
        return accountAliasMap.get( accountAlias );
    }

    public boolean isValidAccount( String accountAlias ) {
        return accountAliasMap.containsKey( accountAlias );
    }

    private void init( Context context ) {
        try {
            String[ ] assets = context.getAssets( ).list( "" );

            for ( String asset : assets ) {
                if ( asset.startsWith( "account_" ) ) {
                    AccountProperties properties = new AccountProperties( context.getAssets( ).open( asset ) );

                    accountAliasMap.put( properties.getAccountAlias( ), properties );
                }
            }
        } catch ( IOException e ) {
            throw new IllegalStateException( e );
        }
    }

}
