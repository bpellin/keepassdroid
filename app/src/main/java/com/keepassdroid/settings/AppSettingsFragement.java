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

import android.app.backup.BackupManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.android.keepass.R;
import com.keepassdroid.app.App;

public class AppSettingsFragement extends PreferenceFragmentCompat {
	public static boolean KEYFILE_DEFAULT = false;
	
	private BackupManager backupManager;

	public static void Launch(Context ctx) {
		Intent i = new Intent(ctx, AppSettingsFragement.class);
		
		ctx.startActivity(i);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		addPreferencesFromResource(R.xml.preferences_app);

		Preference keyFile = findPreference(getString(R.string.keyfile_key));
		keyFile.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Boolean value = (Boolean) newValue;

				if ( ! value.booleanValue() ) {
					App.getFileHistory().deleteAllKeys();
				}

				return true;
			}
		});

		Preference recentHistory = findPreference(getString(R.string.recentfile_key));
		recentHistory.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Boolean value = (Boolean) newValue;

				if (value == null) {
					value = true;
				}

				if (!value) {
					App.getFileHistory().deleteAll();
				}

				return true;
			}
		});

		backupManager = new BackupManager(getContext());
	}

	@Override
	public void onStop() {
		backupManager.dataChanged();
		
		super.onStop();
	}

}
