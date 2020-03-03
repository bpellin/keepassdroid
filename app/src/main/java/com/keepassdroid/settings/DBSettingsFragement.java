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
import androidx.fragment.app.DialogFragment;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.android.keepass.R;
import com.keepassdroid.Database;
import com.keepassdroid.app.App;
import com.keepassdroid.database.PwEncryptionAlgorithm;

public class DBSettingsFragement extends PreferenceFragmentCompat {
	public static boolean KEYFILE_DEFAULT = false;
	
	private BackupManager backupManager;
	
	public static void Launch(Context ctx) {
		Intent i = new Intent(ctx, DBSettingsFragement.class);
		
		ctx.startActivity(i);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
	}

	private static final String ROUNDS_PREFERENCE_TAG = "ROUNDS";
	@Override
	public void onDisplayPreferenceDialog(Preference preference) {
		if (getFragmentManager().findFragmentByTag(ROUNDS_PREFERENCE_TAG) != null) {
			return;
		}

		String key = preference.getKey();
		if ((preference instanceof EditTextPreference) && key.equals(getString(R.string.rounds_key)) ) {
			final DialogFragment f = RoundsPreferenceFragment.newInstance(preference.getKey());
			f.setTargetFragment(this, 0);
			f.show(getFragmentManager(), ROUNDS_PREFERENCE_TAG);
		} else {
			super.onDisplayPreferenceDialog(preference);
		}
	}


	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		addPreferencesFromResource(R.xml.preferences_db);

		Database db = App.getDB();
		if ( db.Loaded() && db.pm.appSettingsEnabled() ) {
			Preference rounds = findPreference(getString(R.string.rounds_key));
			rounds.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

				public boolean onPreferenceChange(Preference preference, Object newValue) {
					setRounds(App.getDB(), preference);
					return true;
				}
			});

			setRounds(db, rounds);

			Preference algorithm = findPreference(getString(R.string.algorithm_key));
			setAlgorithm(db, algorithm);
		}

		backupManager = new BackupManager(getContext());
	}

	@Override
	public void onStop() {
		backupManager.dataChanged();
		
		super.onStop();
	}

	private void setRounds(Database db, Preference rounds) {
		rounds.setSummary(Long.toString(db.pm.getNumRounds()));
	}
	
	private void setAlgorithm(Database db, Preference algorithm) {
		int resId;
		if ( db.pm.getEncAlgorithm() == PwEncryptionAlgorithm.Rjindal ) {
			resId = R.string.rijndael;
		} else  {
			resId = R.string.twofish;
		}
		
		algorithm.setSummary(resId);
	}
	
	

}
