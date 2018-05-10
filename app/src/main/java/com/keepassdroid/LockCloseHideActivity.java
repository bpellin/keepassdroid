/*
 * Copyright 2015-2018 Brian Pellin.
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
package com.keepassdroid;

import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager.LayoutParams;

/** 
 * Locking Close Activity that sets FLAG_SECURE to prevent screenshots, and from
 * appearing in the recent app preview
 * @author Brian Pellin
 *
 */
public abstract class LockCloseHideActivity extends LockCloseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Several gingerbread devices have problems with FLAG_SECURE
		int ver = Build.VERSION.SDK_INT;
		if (ver >= Build.VERSION_CODES.ICE_CREAM_SANDWICH || ver < Build.VERSION_CODES.GINGERBREAD) {
		    getWindow().setFlags(LayoutParams.FLAG_SECURE, LayoutParams.FLAG_SECURE);
		}
	}

}
