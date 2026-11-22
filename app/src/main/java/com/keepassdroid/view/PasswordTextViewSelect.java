/*
 * Copyright 2011-2020 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.view;

import android.content.Context;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.MovementMethod;
import android.util.AttributeSet;
import android.graphics.Typeface;
import android.text.InputType;

import androidx.appcompat.widget.AppCompatEditText;

public class PasswordTextViewSelect extends AppCompatEditText {

	private Typeface mTypeface;

	public PasswordTextViewSelect(Context context) {
		this(context, null);
	}

	public PasswordTextViewSelect(Context context, AttributeSet attrs) {
		// this class extends from EditText,
		// so use default EditText style as defStyle
		this(context, attrs, android.R.attr.editTextStyle);
	}
	
	public PasswordTextViewSelect(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		mTypeface = getTypeface();
		
		setFocusable(true);
		setFocusableInTouchMode(true);
	}


	@Override
	protected MovementMethod getDefaultMovementMethod() {
		return ArrowKeyMovementMethod.getInstance();
	}

	@Override
	protected boolean getDefaultEditable() {
		return false;
	}

	@Override
	public void setInputType(int type) {
		super.setInputType(type);

		// some system will set font to default after invoking setInputType
		// so we have to fix it here
		if ((type & InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) != 0) {
			setTypeface(mTypeface);
		}
	}

	@Override
	public void setText(CharSequence text, BufferType type) {
		super.setText(text, BufferType.EDITABLE);
	}

}
