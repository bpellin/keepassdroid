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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLDecoder;

import net.temerity.davsync.DAVException;
import net.temerity.davsync.DAVNetwork;
import net.temerity.davsync.DAVProfile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.method.PasswordTransformationMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.android.keepass.KeePass;
import com.android.keepass.R;
import com.keepassdroid.app.App;
import com.keepassdroid.compat.BackupManagerCompat;
import com.keepassdroid.database.edit.LoadDB;
import com.keepassdroid.database.edit.OnFinish;
import com.keepassdroid.fileselect.BrowserDialog;
import com.keepassdroid.fileselect.FileDbHelper;
import com.keepassdroid.intents.Intents;
import com.keepassdroid.settings.AppSettingsActivity;
import com.keepassdroid.utils.Interaction;
import com.keepassdroid.utils.Util;


public class PasswordActivity extends LockingActivity {

	private static final int MENU_ABOUT = Menu.FIRST;
	private static final int MENU_APP_SETTINGS = Menu.FIRST + 1;
	
	public static final String KEY_DEFAULT_FILENAME = "defaultFileName";
	private static final String KEY_FILENAME = "fileName";
	private static final String KEY_KEYFILE = "keyFile";
	private static final String VIEW_INTENT = "android.intent.action.VIEW";
	
	private static final int FILE_BROWSE = 256;

	private String mFileName;
	private String mKeyFile;
	private boolean mRememberKeyfile;
	SharedPreferences prefs;
	BroadcastReceiver mIntentReceiver;
	
	public static void Launch(Activity act, String fileName) throws FileNotFoundException {
		Launch(act,fileName,"");
	}
	
	public static void Launch(Activity act, String fileName, String keyFile) throws FileNotFoundException {
		File dbFile = new File(fileName);
		if ( ! dbFile.exists() ) {
			throw new FileNotFoundException();
		}
		
		Intent i = new Intent(act, PasswordActivity.class);
		i.putExtra(KEY_FILENAME, fileName);
		i.putExtra(KEY_KEYFILE, keyFile);
		
		act.startActivityForResult(i, 0);
		
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		switch (requestCode) {
		
		case KeePass.EXIT_NORMAL:
			setEditText(R.id.password, "");
			App.getDB().clear();
			break;
		
		case KeePass.EXIT_LOCK:
			setResult(KeePass.EXIT_LOCK);
			setEditText(R.id.password, "");
			finish();
			App.getDB().clear(); 
			break;
		case FILE_BROWSE:
			if (resultCode == RESULT_OK) {
				String filename = data.getDataString();
				if (filename != null) {
					if (filename.startsWith("file://")) {
						filename = filename.substring(7);
					}
					
					filename = URLDecoder.decode(filename);
					
					EditText fn = (EditText) findViewById(R.id.pass_keyfile);
					fn.setText(filename);
				}
			}
			break;
		}
		
	}
	
	/* added by mcrawford */
	protected void processSyncButtons()
	{
		FileDbHelper dbHelp = App.fileDbHelper; // database for recent files
		Button syncbutton = (Button) findViewById(R.id.sync_button);

		try {
			final AlertDialog worked = new AlertDialog.Builder(this).setTitle("Success!").setMessage("File was synchronized successfully!").create();
			final AlertDialog failed = new AlertDialog.Builder(this).setTitle("Failure!").setMessage("Unable to synchronize file!").create();
			
			final DAVProfile prof = dbHelp.getDavProfile(mFileName);
			syncbutton.setOnClickListener(new View.OnClickListener() {
				
				public void onClick(View v) {
					DAVNetwork net = new DAVNetwork(prof, new File(mFileName));
					try {
						if(net.sync())
							worked.show();
						else
							failed.show();
					} catch (Exception e) {
						// FIXME: do something here
					}
				}
			});
			syncbutton.setEnabled(true);
			syncbutton.setBackgroundDrawable(getResources().getDrawable(R.drawable.btn_green));
			syncbutton.setText("Sync");
			syncbutton.setTextColor(Color.WHITE);
			///TODO come back to this after webdav is configured.
		} catch( DAVException ce ) {
			syncbutton.setEnabled(false);
			syncbutton.setBackgroundDrawable(null);
			syncbutton.setText("No WebDAV Configuration Found");
			syncbutton.setTextColor(Color.RED);
		}
		
		Button wdcbutton = (Button) findViewById(R.id.webdav_configure_button);
		wdcbutton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				Intent x = new Intent(PasswordActivity.this, net.temerity.davsync.davsync.class);
				x.putExtra("kdbfile",mFileName); 
				startActivityForResult(x, 0);
			}
		});
	}
	
	
	@Override
	protected void onResume()
	{
		super.onResume();
		processSyncButtons();
	}
	
	/* end added by mcrawford */
	
	private boolean checkNetworkPermission() {
	    String permission = "android.permission.INTERNET";
	    int res = getBaseContext().checkCallingOrSelfPermission(permission);
	    return res == PackageManager.PERMISSION_GRANTED;        
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	
		Intent i = getIntent();
		String action = i.getAction();
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mRememberKeyfile = prefs.getBoolean(getString(R.string.keyfile_key), getResources().getBoolean(R.bool.keyfile_default));
		
		if ( action != null && action.equals(VIEW_INTENT) ) {
			mFileName = i.getDataString();
			
			if ( ! mFileName.substring(0, 7).equals("file://") ) {
				Toast.makeText(this, R.string.error_can_not_handle_uri, Toast.LENGTH_LONG).show();
				finish();
				return;
			}
			
			mFileName = URLDecoder.decode(mFileName.substring(7, mFileName.length()));
						
			if ( mFileName.length() == 0 ) {
				// No file name
				Toast.makeText(this, R.string.FileNotFound, Toast.LENGTH_LONG).show();
				finish();
				return;
			}
			
			File dbFile = new File(mFileName);
			if ( ! dbFile.exists() ) {
				// File does not exist
				Toast.makeText(this, R.string.FileNotFound, Toast.LENGTH_LONG).show();
				finish();
				return;
			}
			
			mKeyFile = getKeyFile(mFileName);
			
		} else {
			mFileName = i.getStringExtra(KEY_FILENAME);
			mKeyFile = i.getStringExtra(KEY_KEYFILE);
			if ( mKeyFile == null || mKeyFile.length() == 0) {
				mKeyFile = getKeyFile(mFileName);
			}
		}
		
		setContentView(R.layout.password);
		populateView();
		
		Button confirmButton = (Button) findViewById(R.id.pass_ok);
		confirmButton.setOnClickListener(new OkClickHandler());
		
		CheckBox checkBox = (CheckBox) findViewById(R.id.show_password);
		// Show or hide password
		checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				TextView password = (TextView) findViewById(R.id.password);

				if ( isChecked ) {
					password.setTransformationMethod(null);
				} else {
					password.setTransformationMethod(PasswordTransformationMethod.getInstance());
				}
			}
			
		});
		
		CheckBox defaultCheck = (CheckBox) findViewById(R.id.default_database);
		defaultCheck.setOnCheckedChangeListener(new DefaultCheckChange());
		
		ImageButton browse = (ImageButton) findViewById(R.id.browse_button);
		browse.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				if (Interaction.isIntentAvailable(PasswordActivity.this, Intents.FILE_BROWSE)) {
					Intent i = new Intent(Intents.FILE_BROWSE);
					
					if (mFileName.length() > 0) {
						File keyfile = new File(mFileName);
						File parent = keyfile.getParentFile();
						if (parent != null) {
							i.setData(Uri.parse("file://" + parent.getAbsolutePath()));
						}
					}
					
					startActivityForResult(i, FILE_BROWSE);
				} else {
					BrowserDialog diag = new BrowserDialog(PasswordActivity.this);
					diag.show();
				}
					
			}
		});
		/* added by mcrawford */
		processSyncButtons();
		/* end added by mcrawford */
		
		retrieveSettings();
		
		mIntentReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (action.equals(Intents.TIMEOUT)) {
					setEditText(R.id.password, "");
				}
				
			}
		};
		registerReceiver(mIntentReceiver, new IntentFilter());
	}
	
	@Override
	protected void onDestroy() {
		if (mIntentReceiver != null) {
			unregisterReceiver(mIntentReceiver);
		}
		
		super.onDestroy();
	}

	private void retrieveSettings() {
		String defaultFilename = prefs.getString(KEY_DEFAULT_FILENAME, "");
		if (mFileName.length() > 0 && mFileName.equals(defaultFilename)) {
			CheckBox checkbox = (CheckBox) findViewById(R.id.default_database);
			checkbox.setChecked(true);
		}
	}
	
	private String getKeyFile(String filename) {
		if ( mRememberKeyfile ) {
			FileDbHelper dbHelp = App.fileDbHelper;
			
			String keyfile = dbHelp.getFileByName(filename);
			
			return keyfile;
		} else {
			return "";
		}
	}
	
	private void populateView() {
		setEditText(R.id.filename, mFileName);
		
		setEditText(R.id.pass_keyfile, mKeyFile);
	}
	
	/*
	private void errorMessage(CharSequence text)
	{
		Toast.makeText(this, text, Toast.LENGTH_LONG).show();
	}
	*/
	
	private void errorMessage(int resId)
	{
		Toast.makeText(this, resId, Toast.LENGTH_LONG).show();
	}
	
	private class DefaultCheckChange implements CompoundButton.OnCheckedChangeListener {
		
		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			
			String newDefaultFileName;
			
			if (isChecked) {
				newDefaultFileName = mFileName;
			} else {
				newDefaultFileName = "";
			}
			
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString(KEY_DEFAULT_FILENAME, newDefaultFileName);
			editor.commit();
			
			BackupManagerCompat backupManager = new BackupManagerCompat(PasswordActivity.this);
			backupManager.dataChanged();
			
		}
		
	}
	
	private class OkClickHandler implements View.OnClickListener {
		
		public void onClick(View view) {
			String pass = getEditText(R.id.password);
			String key = getEditText(R.id.pass_keyfile);
			if ( pass.length() == 0 && key.length() == 0 ) {
				errorMessage(R.string.error_nopass);
				return;
			}
			
			String fileName = getEditText(R.id.filename);
			
			
			// Clear before we load
			Database db = App.getDB();
			db.clear();
			
			// Clear the shutdown flag
			App.clearShutdown();
			
			Handler handler = new Handler();
			LoadDB task = new LoadDB(db, PasswordActivity.this, fileName, pass, key, new AfterLoad(handler));
			ProgressTask pt = new ProgressTask(PasswordActivity.this, task, R.string.loading_database);
			pt.run();
		}			
	}
	
	private String getEditText(int resId) {
		return Util.getEditText(this, resId);
	}
	
	private void setEditText(int resId, String str) {
		TextView te =  (TextView) findViewById(resId);
		assert(te == null);
		
		if (te != null) {
			te.setText(str);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, MENU_APP_SETTINGS, 0, R.string.menu_app_settings);
		menu.findItem(MENU_APP_SETTINGS).setIcon(android.R.drawable.ic_menu_preferences);
		
		menu.add(0, MENU_ABOUT, 0, R.string.menu_about);
		menu.findItem(MENU_ABOUT).setIcon(android.R.drawable.ic_menu_help);
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch ( item.getItemId() ) {
		case MENU_ABOUT:
			AboutDialog dialog = new AboutDialog(this);
			dialog.show();
			return true;
			
		case MENU_APP_SETTINGS:
			AppSettingsActivity.Launch(this);
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}

	private final class AfterLoad extends OnFinish {
		
		public AfterLoad(Handler handler) {
			super(handler);
		}

		@Override
		public void run() {
			if ( mSuccess ) {
				GroupActivity.Launch(PasswordActivity.this);
			} else {
				displayMessage(PasswordActivity.this);
			}
		}
	}
	
}
