/*
 * Copyright 2009-2013 Brian Pellin.
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
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.keepass.KeePass;
import com.android.keepass.R;
import com.keepassdroid.app.App;
import com.keepassdroid.compat.ActivityCompat;
import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.PwEntryV4;
import com.keepassdroid.database.exception.SamsungClipboardException;
import com.keepassdroid.intents.Intents;
import com.keepassdroid.utils.EmptyUtils;
import com.keepassdroid.utils.Types;
import com.keepassdroid.utils.Util;

public class EntryActivity extends LockCloseActivity {
	public static final String KEY_ENTRY = "entry";
	public static final String KEY_REFRESH_POS = "refresh_pos";

	public static final int NOTIFY_USERNAME = 1;
	public static final int NOTIFY_PASSWORD = 2;
	
	final Context context = this;
	
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
		
		Button telepass = (Button) findViewById(R.id.entry_telepass);
		telepass.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setComponent(new ComponentName("com.a29apps.telepass", "com.a29apps.telepass.TelePassActivity"));
				intent.putExtra(Intent.EXTRA_TEXT, mEntry.getPassword());
				try {
					startActivity(intent);
				} catch (ActivityNotFoundException ex) {
					Toast.makeText(context, R.string.telepass_err, Toast.LENGTH_LONG).show();
				}
			}
		});
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mShowPassword = ! prefs.getBoolean(getString(R.string.maskpass_key), getResources().getBoolean(R.bool.maskpass_default));
		
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
		
		mEntry = db.pm.entries.get(uuid);
		if (mEntry == null) {
			Toast.makeText(this, R.string.entry_not_found, Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		
		// Refresh Menu contents in case onCreateMenuOptions was called before mEntry was set
		ActivityCompat.invalidateOptionsMenu(this);
		
		// Update last access time.
		mEntry.touch(false, false);
		
		fillData(false);

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
	
	protected void fillData(boolean trimList) {
		ImageView iv = (ImageView) findViewById(R.id.entry_icon);
		App.getDB().drawFactory.assignDrawableTo(iv, getResources(), mEntry.getIcon());

		populateText(R.id.entry_title, mEntry.getTitle());
		populateText(R.id.entry_user_name, mEntry.getUsername());
		
		populateText(R.id.entry_url, mEntry.getUrl());
		populateText(R.id.entry_password, mEntry.getPassword());
		setPasswordStyle();
		
		populateText(R.id.entry_created, getDateTime(mEntry.getCreationTime()));
		populateText(R.id.entry_modified, getDateTime(mEntry.getLastModificationTime()));
		populateText(R.id.entry_accessed, getDateTime(mEntry.getLastAccessTime()));
		
		Date expires = mEntry.getExpiryTime();
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
			fillData(true);
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
		
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.entry, menu);
		
		MenuItem togglePassword = menu.findItem(R.id.menu_toggle_pass);
		if ( mShowPassword ) {
			togglePassword.setTitle(R.string.menu_hide_password);
		} else {
			togglePassword.setTitle(R.string.show_password);
		}
		
		MenuItem gotoUrl = menu.findItem(R.id.menu_goto_url);
		MenuItem copyUser = menu.findItem(R.id.menu_copy_user);
		MenuItem copyPass = menu.findItem(R.id.menu_copy_pass);
		
		// In API >= 11 onCreateOptionsMenu may be called before onCreate completes
		// so mEntry may not be set
		if (mEntry == null) {
			gotoUrl.setVisible(false);
			copyUser.setVisible(false);
			copyPass.setVisible(false);
		}
		else {
			String url = mEntry.getUrl();
			if (EmptyUtils.isNullOrEmpty(url)) {
				// disable button if url is not available
				gotoUrl.setVisible(false);
			}
			if ( mEntry.getUsername().length() == 0 ) {
				// disable button if username is not available
				copyUser.setVisible(false);
			}
			if ( mEntry.getPassword().length() == 0 ) {
				// disable button if password is not available
				copyPass.setVisible(false);
			}
		}
		
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
		case R.id.menu_donate:
			try {
				Util.gotoUrl(this, R.string.donate_url);
			} catch (ActivityNotFoundException e) {
				Toast.makeText(this, R.string.error_failed_to_launch_link, Toast.LENGTH_LONG).show();
				return false;
			}
			
			return true;
		case R.id.menu_toggle_pass:
			if ( mShowPassword ) {
				item.setTitle(R.string.show_password);
				mShowPassword = false;
			} else {
				item.setTitle(R.string.menu_hide_password);
				mShowPassword = true;
			}
			setPasswordStyle();

			return true;
			
		case R.id.menu_goto_url:
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
			
		case R.id.menu_copy_user:
			timeoutCopyToClipboard(mEntry.getUsername());
			return true;
			
		case R.id.menu_copy_pass:
			timeoutCopyToClipboard(new String(mEntry.getPassword()));
			return true;
			
		case R.id.menu_lock:
			App.setShutdown();
			setResult(KeePass.EXIT_LOCK);
			finish();
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	private void timeoutCopyToClipboard(String text) {
		try {
			Util.copyToClipboard(this, text);
		} catch (SamsungClipboardException e) {
			showSamsungDialog();
			return;
		}
		
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
				try {
					Util.copyToClipboard(mCtx, "");
					uiThreadCallback.post(new UIToastTask(mCtx, R.string.ClearClipboard));
				} catch (SamsungClipboardException e) {
					uiThreadCallback.post(new UIToastTask(mCtx, R.string.clipboard_error_clear));
				}
			}
		}
	}
	
	private void showSamsungDialog() {
		String text = getString(R.string.clipboard_error).concat(System.getProperty("line.separator")).concat(getString(R.string.clipboard_error_url));
		SpannableString s = new SpannableString(text);
		TextView tv = new TextView(this);
		tv.setText(s);
		tv.setAutoLinkMask(RESULT_OK);
		tv.setMovementMethod(LinkMovementMethod.getInstance());
		Linkify.addLinks(s, Linkify.WEB_URLS);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.clipboard_error_title)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
			.setView(tv)
			.show();
		
	}
}
