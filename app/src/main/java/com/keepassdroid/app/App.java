/*
 * Copyright 2009-2022 Brian Pellin.
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
package com.keepassdroid.app;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.multidex.MultiDexApplication;

import com.keepassdroid.Database;
import com.keepassdroid.compat.PRNGFixes;
import com.keepassdroid.fileselect.RecentFileHistory;
import com.keepassdroid.intents.Intents;

import java.util.Calendar;

public class App extends MultiDexApplication {
	private static Database db = null;
	private static boolean shutdown = false;
	private static Calendar calendar = null;
	private static RecentFileHistory fileHistory = null;
	private static final String TAG = "KeePassDroid Timer";

	private BroadcastReceiver mIntentReceiver;

	public static Database getDB() {
		if ( db == null ) {
			db = new Database();
		}
		
		return db;
	}
	
	public static RecentFileHistory getFileHistory() {
		return fileHistory;
	}
	
	public static void setDB(Database d) {
		db = d;
	}
	
	public static boolean isShutdown() {
		return shutdown;
	}
	
	public static void setShutdown() {
		shutdown = true;
	}
	
	public static void clearShutdown() {
		shutdown = false;
	}
	
	public static Calendar getCalendar() {
		if ( calendar == null ) {
			calendar = Calendar.getInstance();
		}
		
		return calendar;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		
		fileHistory = new RecentFileHistory(this);
		
		PRNGFixes.apply();

		mIntentReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();

				if ( action.equals(Intents.TIMEOUT) ) {
					timeout(context);
				}
			}
		};

		IntentFilter filter = new IntentFilter();
		filter.addAction(Intents.TIMEOUT);
		registerReceiver(mIntentReceiver, filter);
	}

	private void timeout(Context context) {
		Log.d(TAG, "Timeout");
		App.setShutdown();

		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		nm.cancelAll();
	}

	@Override
	public void onTerminate() {
		if ( db != null ) {
			db.clear(getApplicationContext());
		}

		unregisterReceiver(mIntentReceiver);
		super.onTerminate();
	}
}
