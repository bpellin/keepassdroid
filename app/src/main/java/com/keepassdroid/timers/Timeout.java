/*
 * Copyright 2012-2019 Brian Pellin.
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
package com.keepassdroid.timers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.keepass.KeePass;
import com.android.keepass.R;
import com.keepassdroid.intents.Intents;

public class Timeout {
	private static final int REQUEST_ID = 0;
	private static final long DEFAULT_TIMEOUT = 5 * 60 * 1000;  // 5 minutes
	private static String TAG = "KeePass Timeout";

	private static PendingIntent buildIntent(Context ctx) {
		Intent intent = new Intent(Intents.TIMEOUT);
		intent.setPackage("com.android.keepass");
		int flags = PendingIntent.FLAG_CANCEL_CURRENT;
		if (Build.VERSION.SDK_INT >= 23) {
			flags |= PendingIntent.FLAG_IMMUTABLE;
		}
		PendingIntent sender = PendingIntent.getBroadcast(ctx, REQUEST_ID, intent, flags);

		return sender;
	}
	
	public static void start(Context ctx) {


		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		String sTimeout = prefs.getString(ctx.getString(R.string.app_timeout_key), ctx.getString(R.string.clipboard_timeout_default));
		
		long timeout;
		try {
			timeout = Long.parseLong(sTimeout);
		} catch (NumberFormatException e) {
			timeout = DEFAULT_TIMEOUT;
		}
		
		if ( timeout == -1 ) {
			// No timeout don't start timeout service
			return;
		}
		
		long triggerTime = System.currentTimeMillis() + timeout;
		AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		
		Log.d(TAG, "Timeout start");
		am.set(AlarmManager.RTC, triggerTime, buildIntent(ctx));
	}
	
	public static void cancel(Context ctx) {
		AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		
		Log.d(TAG, "Timeout cancel");
		am.cancel(buildIntent(ctx));
	}

}
