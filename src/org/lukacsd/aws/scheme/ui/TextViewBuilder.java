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
import android.graphics.Typeface;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public class TextViewBuilder {
    private Context context;
    private int width = LayoutParams.WRAP_CONTENT;
    private int height = LayoutParams.WRAP_CONTENT;
    private int textAppearance;
    private int typeface;
    private int paddingLeft = 2;
    private int paddingTop = 0;
    private int paddingRight = 2;
    private int paddingBottom = 0;
    private int textColor = R.color.icsWhite;
    private String text;
    private Integer textId;

    public TextViewBuilder( Context context ) {
        this.context = context;
    }

    public TextViewBuilder matchWidth() {
        this.width = LayoutParams.MATCH_PARENT;

        return this;
    }

    public TextViewBuilder matchHeight() {
        this.height = LayoutParams.MATCH_PARENT;

        return this;
    }

    public TextViewBuilder textSmall() {
        this.textAppearance = android.R.style.TextAppearance_Small;

        return this;
    }

    public TextViewBuilder textMedium() {
        this.textAppearance = android.R.style.TextAppearance_Medium;

        return this;
    }

    public TextViewBuilder color( int color ) {
        this.textColor = color;

        return this;
    }

    public TextViewBuilder bold() {
        this.typeface = Typeface.BOLD;

        return this;
    }

    public TextViewBuilder italic() {
        this.typeface = Typeface.ITALIC;

        return this;
    }

    public TextViewBuilder boldItalic() {
        this.typeface = Typeface.BOLD_ITALIC;

        return this;
    }

    public TextViewBuilder padLeft( int pad ) {
        this.paddingLeft = pad;

        return this;
    }

    public TextViewBuilder padTop( int pad ) {
        this.paddingTop = pad;

        return this;
    }

    public TextViewBuilder text( String text ) {
        this.text = text;

        return this;
    }

    public TextViewBuilder text( int textId ) {
        this.textId = textId;

        return this;
    }

    public TextView build() {
        LayoutParams layoutParams = new LayoutParams( width, height );

        TextView retval = new TextView( context );
        retval.setLayoutParams( layoutParams );
        if ( textId != null ) {
            retval.setText( context.getString( textId ) );
        } else {
            retval.setText( text );
        }
        retval.setPadding( paddingLeft, paddingTop, paddingRight, paddingBottom );
        retval.setTextAppearance( context, textAppearance );
        retval.setTextColor( context.getResources( ).getColor( textColor ) );
        retval.setTypeface( null, typeface );

        return retval;
    }

}
