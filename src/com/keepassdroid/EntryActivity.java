/*
 * Copyright 2009-2011 Brian Pellin.
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

import java.text.DateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.method.PasswordTransformationMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.keepass.KeePass;
import com.android.keepass.R;
import com.keepassdroid.app.App;
import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.PwEntryV4;
import com.keepassdroid.intents.Intents;
import com.keepassdroid.utils.Types;
import com.keepassdroid.utils.Util;

public class EntryActivity extends LockCloseActivity {
	public static final String KEY_ENTRY = "entry";
	public static final String KEY_REFRESH_POS = "refresh_pos";

	private static final int MENU_DONATE = Menu.FIRST;
	private static final int MENU_PASS = Menu.FIRST + 1;
	private static final int MENU_GOTO_URL = Menu.FIRST + 2;
	private static final int MENU_COPY_USER = Menu.FIRST + 3;
	private static final int MENU_COPY_PASS = Menu.FIRST + 4;
	private static final int MENU_LOCK = Menu.FIRST + 5; 
	
	public static final int NOTIFY_USERNAME = 1;
	public static final int NOTIFY_PASSWORD = 2;
	
	public static void Launch(Activity act, PwEntry pw, int pos) {
		Intent i;
		
		if ( pw instanceof PwEntryV4 ) {
			i = new Intent(act, EntryActivityV4.class);
		} else {
			i = new Intent(act, EntryActivity.class);
		}
		
		i.putExtra(KEY_ENTRY, Types.UUIDtoBytes(pw.getUUID()));
		i.putExtra(KEY_REFRESH_POS, pos);
		
		act.startActivityForResult(i,0);
	}
	
	protected PwEntry mEntry;
	private Timer mTimer = new Timer();
	private boolean mShowPassword;
	private int mPos;
	private NotificationManager mNM;
	private BroadcastReceiver mIntentReceiver;
	
	private DateFormat dateFormat;
	private DateFormat timeFormat;
	
	protected void setEntryView() {
		setContentView(R.layout.entry_view);
	}
	
	protected void setupEditButtons() {
		Button edit = (Button) findViewById(R.id.entry_edit);
		edit.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				EntryEditActivity.Launch(EntryActivity.this, mEntry);
			}
			
		});
		
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setEntryView();
		
		Context appCtx = getApplicationContext();
		dateFormat = android.text.format.DateFormat.getDateFormat(appCtx);
		timeFormat = android.text.format.DateFormat.getTimeFormat(appCtx);

		Database db = App.getDB();
		// Likely the app has been killed exit the activity 
		if ( ! db.Loaded() ) {
			finish();
			return;
		}

		setResult(KeePass.EXIT_NORMAL);

		Intent i = getIntent();
		UUID uuid = Types.bytestoUUID(i.getByteArrayExtra(KEY_ENTRY));
		mPos = i.getIntExtra(KEY_REFRESH_POS, -1);
		assert(uuid != null);
		
		mEntry = db.entries.get(uuid);
		
		// Update last access time.
		mEntry.stampLastAccess();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mShowPassword = ! prefs.getBoolean(getString(R.string.maskpass_key), getResources().getBoolean(R.bool.maskpass_default));
		fillData();

		setupEditButtons();
		
		// Notification Manager
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		if ( mEntry.getPassword().length() > 0 ) {
			// only show notification if password is available
			Notification password = getNotification(Intents.COPY_PASSWORD, R.string.copy_password);
			mNM.notify(NOTIFY_PASSWORD, password);
		}
		
		if ( mEntry.getUsername().length() > 0 ) {
			// only show notification if username is available
			Notification username = getNotification(Intents.COPY_USERNAME, R.string.copy_username);
			mNM.notify(NOTIFY_USERNAME, username);
		}
			
		mIntentReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();

				if ( action.equals(Intents.COPY_USERNAME) ) {
					String username = mEntry.getUsername();
					if ( username.length() > 0 ) {
						timeoutCopyToClipboard(username);
					}
				} else if ( action.equals(Intents.COPY_PASSWORD) ) {
					String password = new String(mEntry.getPassword());
					if ( password.length() > 0 ) {
						timeoutCopyToClipboard(new String(mEntry.getPassword()));
					}
				}
			}
		};
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intents.COPY_USERNAME);
		filter.addAction(Intents.COPY_PASSWORD);
		registerReceiver(mIntentReceiver, filter);
	}
	
	@Override
	protected void onDestroy() {
		// These members might never get initialized if the app timed out
		if ( mIntentReceiver != null ) {
			unregisterReceiver(mIntentReceiver);
		}
		
		if ( mNM != null ) {
			mNM.cancelAll();
		}
		
		super.onDestroy();
	}

	private Notification getNotification(String intentText, int descResId) {
		String desc = getString(descResId);
		Notification notify = new Notification(R.drawable.notify, desc, System.currentTimeMillis());
		
		Intent intent = new Intent(intentText);
		PendingIntent pending = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		
		notify.setLatestEventInfo(this, getString(R.string.app_name), desc, pending);
		
		return notify;
	}
	
	private String getDateTime(Date dt) {
		return dateFormat.format(dt) + " " + timeFormat.format(dt);
		
	}
	
	protected void fillData() {
		ImageView iv = (ImageView) findViewById(R.id.entry_icon);
		App.getDB().drawFactory.assignDrawableTo(iv, getResources(), mEntry.getIcon());

		populateText(R.id.entry_title, mEntry.getTitle());
		populateText(R.id.entry_user_name, mEntry.getUsername());
		
		populateText(R.id.entry_url, mEntry.getUrl());
		populateText(R.id.entry_password, mEntry.getPassword());
		setPasswordStyle();
		
		populateText(R.id.entry_created, getDateTime(mEntry.getCreate()));
		populateText(R.id.entry_modified, getDateTime(mEntry.getMod()));
		populateText(R.id.entry_accessed, getDateTime(mEntry.getAccess()));
		
		Date expires = mEntry.getExpire();
		if ( mEntry.expires() ) {
			populateText(R.id.entry_expires, getDateTime(expires));
		} else {
			populateText(R.id.entry_expires, R.string.never);
		}
		populateText(R.id.entry_comment, mEntry.getNotes());

	}
	
	private void populateText(int viewId, int resId) {
		TextView tv = (TextView) findViewById(viewId);
		tv.setText(resId);
	}

	private void populateText(int viewId, String text) {
		TextView tv = (TextView) findViewById(viewId);
		tv.setText(text);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if ( resultCode == KeePass.EXIT_REFRESH || resultCode == KeePass.EXIT_REFRESH_TITLE ) {
			fillData();
			if ( resultCode == KeePass.EXIT_REFRESH_TITLE ) {
				Intent ret = new Intent();
				ret.putExtra(KEY_REFRESH_POS, mPos);
				setResult(KeePass.EXIT_REFRESH, ret);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, MENU_DONATE, 0, R.string.menu_donate);
		menu.findItem(MENU_DONATE).setIcon(android.R.drawable.ic_menu_share);
		
		if ( mShowPassword ) {
			menu.add(0, MENU_PASS, 0, R.string.menu_hide_password);
		} else {
			menu.add(0, MENU_PASS, 0, R.string.show_password);
		}
		menu.findItem(MENU_PASS).setIcon(android.R.drawable.ic_menu_view);
		menu.add(0, MENU_GOTO_URL, 0, R.string.menu_url);
		menu.findItem(MENU_GOTO_URL).setIcon(android.R.drawable.ic_menu_upload);
		
		if ( mEntry.getUrl().length() == 0 ) {
			// disable button if url is not available
			menu.findItem(MENU_GOTO_URL).setEnabled(false);
		}
		menu.add(0, MENU_COPY_USER, 0, R.string.menu_copy_user);
		menu.findItem(MENU_COPY_USER).setIcon(android.R.drawable.ic_menu_set_as);
		if ( mEntry.getUsername().length() == 0 ) {
			// disable button if username is not available
			menu.findItem(MENU_COPY_USER).setEnabled(false);
		}
		menu.add(0, MENU_COPY_PASS, 0, R.string.menu_copy_pass);
		menu.findItem(MENU_COPY_PASS).setIcon(android.R.drawable.ic_menu_agenda);
		if ( mEntry.getPassword().length() == 0 ) {
			// disable button if password is not available
			menu.findItem(MENU_COPY_PASS).setEnabled(false);
		}
		menu.add(0, MENU_LOCK, 0, R.string.menu_lock);
		menu.findItem(MENU_LOCK).setIcon(android.R.drawable.ic_lock_lock);
		
		return true;
	}
	
	private void setPasswordStyle() {
		TextView password = (TextView) findViewById(R.id.entry_password);

		if ( mShowPassword ) {
			password.setTransformationMethod(null);
		} else {
			password.setTransformationMethod(PasswordTransformationMethod.getInstance());
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch ( item.getItemId() ) {
		case MENU_DONATE:
			try {
				Util.gotoUrl(this, R.string.donate_url);
			} catch (ActivityNotFoundException e) {
				Toast.makeText(this, R.string.error_failed_to_launch_link, Toast.LENGTH_LONG).show();
				return false;
			}
			
			return true;
		case MENU_PASS:
			if ( mShowPassword ) {
				item.setTitle(R.string.show_password);
				mShowPassword = false;
			} else {
				item.setTitle(R.string.menu_hide_password);
				mShowPassword = true;
			}
			setPasswordStyle();

			return true;
			
		case MENU_GOTO_URL:
			String url;
			url = mEntry.getUrl();
			
			// Default http:// if no protocol specified
			if ( ! url.contains("://") ) {
				url = "http://" + url;
			}
			
			try {
				Util.gotoUrl(this, url);
			} catch (ActivityNotFoundException e) {
				Toast.makeText(this, R.string.no_url_handler, Toast.LENGTH_LONG).show();
			}
			return true;
			
		case MENU_COPY_USER:
			timeoutCopyToClipboard(mEntry.getUsername());
			return true;
			
		case MENU_COPY_PASS:
			timeoutCopyToClipboard(new String(mEntry.getPassword()));
			return true;
			
		case MENU_LOCK:
			App.setShutdown();
			setResult(KeePass.EXIT_LOCK);
			finish();
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	private void timeoutCopyToClipboard(String text) {
		Util.copyToClipboard(this, text);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String sClipClear = prefs.getString(getString(R.string.clipboard_timeout_key), getString(R.string.clipboard_timeout_default));
		
		long clipClearTime = Long.parseLong(sClipClear);
		
		if ( clipClearTime > 0 ) {
			mTimer.schedule(new ClearClipboardTask(this, text), clipClearTime);
		}
	}
	

	// Setup to allow the toast to happen in the foreground
	final Handler uiThreadCallback = new Handler();

	// Task which clears the clipboard, and sends a toast to the foreground.
	private class ClearClipboardTask extends TimerTask {
		
		private final String mClearText;
		private final Context mCtx;
		
		ClearClipboardTask(Context ctx, String clearText) {
			mClearText = clearText;
			mCtx = ctx;
		}
		
		@Override
		public void run() {
			String currentClip = Util.getClipboard(mCtx);
			
			if ( currentClip.equals(mClearText) ) {
				Util.copyToClipboard(mCtx, "");
				uiThreadCallback.post(new UIToastTask(mCtx, R.string.ClearClipboard));
			}
		}
	}
}
