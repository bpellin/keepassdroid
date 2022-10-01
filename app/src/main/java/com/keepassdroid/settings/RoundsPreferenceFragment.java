/*
 * Copyright 2009-2020 Brian Pellin.
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


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.EditTextPreference;
import androidx.preference.EditTextPreferenceDialogFragmentCompat;
import androidx.preference.Preference;

import com.android.keepass.R;
import com.keepassdroid.Database;
import com.keepassdroid.ProgressTask;
import com.keepassdroid.app.App;
import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.edit.OnFinish;
import com.keepassdroid.database.edit.SaveDB;

public class RoundsPreferenceFragment extends EditTextPreferenceDialogFragmentCompat {

	private EditText mEditText;
	private PwDatabase mPM;

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);

		mEditText = view.findViewById(android.R.id.edit);
		mEditText.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_NORMAL);

		Database db = App.getDB();
		mPM = db.pm;
		mEditText.setText(Long.toString(db.pm.getNumRounds()));

	}


	public static RoundsPreferenceFragment newInstance(String key) {
		final RoundsPreferenceFragment
				fragment = new RoundsPreferenceFragment();
		final Bundle b = new Bundle(1);
		b.putString(ARG_KEY, key);
		fragment.setArguments(b);
		return fragment;
	}

	@Override
	public void onDialogClosed(boolean positiveResult) {
	    EditTextPreference pref = (EditTextPreference) getPreference();

		if ( positiveResult ) {
			int rounds;

			try {
				String strRounds = mEditText.getText().toString();
				rounds = Integer.parseInt(strRounds);
			} catch (NumberFormatException e) {
				Toast.makeText(getContext(), R.string.error_rounds_not_number, Toast.LENGTH_LONG).show();
				return;
			}

			if ( rounds < 1 ) {
				rounds = 1;
			}


			long oldRounds = mPM.getNumRounds();
			try {
				mPM.setNumRounds(rounds);
			} catch (NumberFormatException e) {
				Toast.makeText(getContext(), R.string.error_rounds_too_large, Toast.LENGTH_LONG).show();
				mPM.setNumRounds(Integer.MAX_VALUE);
			}

			Handler handler = new Handler();
			FragmentActivity activity = getActivity();
			SaveDB save = new SaveDB(getContext(), App.getDB(), new AfterSave(activity, activity.getSupportFragmentManager(), handler, oldRounds));
			ProgressTask pt = new ProgressTask(getActivity(), save, R.string.saving_database);
			pt.run();

		}

	}

	private class AfterSave extends OnFinish {
		private long mOldRounds;
		private Context mCtx;
		FragmentManager mFm;

		public AfterSave(Context ctx, FragmentManager fm, Handler handler, long oldRounds) {
			super(handler);

			mCtx = ctx;
			mFm = fm;
			mOldRounds = oldRounds;
		}

		@Override
		public void run() {
			if ( mSuccess ) {
				Preference preference = getPreference();
				Preference.OnPreferenceChangeListener listner = preference.getOnPreferenceChangeListener();
				if ( listner != null ) {
					listner.onPreferenceChange(preference, null);
				}
			} else {
				displayMessage(mCtx, mFm);
				mPM.setNumRounds(mOldRounds);
			}
			
			super.run();
		}
		
	}

}
