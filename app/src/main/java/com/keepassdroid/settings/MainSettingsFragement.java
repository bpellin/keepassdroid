/*
 * Copyright 2020 Brian Pellin.
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
package com.keepassdroid.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.android.keepass.R;
import com.keepassdroid.Database;
import com.keepassdroid.app.App;

public class MainSettingsFragement extends PreferenceFragmentCompat {
	public static boolean KEYFILE_DEFAULT = false;
	
	public static void Launch(Context ctx) {
		Intent i = new Intent(ctx, MainSettingsFragement.class);
		
		ctx.startActivity(i);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		addPreferencesFromResource(R.xml.preferences);

		Database db = App.getDB();
		if ( !(db.Loaded() && db.pm.appSettingsEnabled()) ) {
			Preference dbSettings = findPreference(getString(R.string.db_key));
			dbSettings.setEnabled(false);
		}
	}
}
