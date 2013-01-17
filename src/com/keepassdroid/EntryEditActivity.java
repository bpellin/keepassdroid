/*
 * Copyright 2009-2012 Brian Pellin.
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

import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.keepass.KeePass;
import com.android.keepass.R;
import com.keepassdroid.app.App;
import com.keepassdroid.database.PwDate;
import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.PwEntryV3;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.PwGroupV3;
import com.keepassdroid.database.PwGroupIdV3;
import com.keepassdroid.database.edit.AddEntry;
import com.keepassdroid.database.edit.OnFinish;
import com.keepassdroid.database.edit.RunnableOnFinish;
import com.keepassdroid.database.edit.UpdateEntry;
import com.keepassdroid.icons.Icons;
import com.keepassdroid.utils.Types;
import com.keepassdroid.utils.Util;

public class EntryEditActivity extends LockCloseActivity {
	public static final String KEY_ENTRY = "entry";
	public static final String KEY_PARENT = "parent";

	public static final int RESULT_OK_ICON_PICKER = 1000;
	public static final int RESULT_OK_PASSWORD_GENERATOR = RESULT_OK_ICON_PICKER + 1;

	private PwEntryV3 mEntry;
	private boolean mShowPassword = false;
	private boolean mIsNew;
	private int mSelectedIconID = -1;
	private ArrayAdapter<PwGroup> mGroupsAdapter;
	
	public static void Launch(Activity act, PwEntry pw) {
		if ( !(pw instanceof PwEntryV3) ) {
			throw new RuntimeException("Not yet implemented.");
		}
		
		Intent i = new Intent(act, EntryEditActivity.class);
		
		i.putExtra(KEY_ENTRY, Types.UUIDtoBytes(pw.getUUID()));
		
		act.startActivityForResult(i, 0);
	}
	
	public static void Launch(Activity act, PwGroup pw) {
		if ( !(pw instanceof PwGroupV3) ) {
			throw new RuntimeException("Not yet implemented.");
		}

		Intent i = new Intent(act, EntryEditActivity.class);
		
		PwGroupV3 parent = (PwGroupV3) pw;
		i.putExtra(KEY_PARENT, parent.groupId);
		
		act.startActivityForResult(i, 0);
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mShowPassword = ! prefs.getBoolean(getString(R.string.maskpass_key), getResources().getBoolean(R.bool.maskpass_default));
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.entry_edit);
		setResult(KeePass.EXIT_NORMAL);
		
		// Likely the app has been killed exit the activity
		Database db = App.getDB();
		if ( ! db.Loaded() ) {
			finish();
			return;
		}

		Intent i = getIntent();
		byte[] uuidBytes = i.getByteArrayExtra(KEY_ENTRY);

		if ( uuidBytes == null ) {
			int groupId = i.getIntExtra(KEY_PARENT, -1);

			mEntry = new PwEntryV3(db, groupId);
			mIsNew = true;
			fillGroupsAndSelect(groupId);
			
		} else {
			UUID uuid = Types.bytestoUUID(uuidBytes);
			assert(uuid != null);

			mEntry = (PwEntryV3) db.entries.get(uuid);
			mIsNew = false;
			
			fillData();
		} 
	
		View scrollView = findViewById(R.id.entry_scroll);
		scrollView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);

		ImageButton iconButton = (ImageButton) findViewById(R.id.icon_button);
		iconButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				IconPickerActivity.Launch(EntryEditActivity.this);
			}
		});

		// Generate password button
		Button generatePassword = (Button) findViewById(R.id.generate_button);
		generatePassword.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				//EntryEditActivity.Launch(EntryActivity.this, mEntry);
				GeneratePasswordActivity.Launch(EntryEditActivity.this);
			}
		});
		

		
		// Save button
		Button save = (Button) findViewById(R.id.entry_save);
		save.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				EntryEditActivity act = EntryEditActivity.this;
				
				// Require title
				String title = Util.getEditText(act, R.id.entry_title);
				if ( title.length() == 0 ) {
					Toast.makeText(act, R.string.error_title_required, Toast.LENGTH_LONG).show();
					return;
				}
				
				// Validate password
				String pass = Util.getEditText(act, R.id.entry_password);
				String conf = Util.getEditText(act, R.id.entry_confpassword);
				if ( ! pass.equals(conf) ) {
					Toast.makeText(act, R.string.error_pass_match, Toast.LENGTH_LONG).show();
					return;
				}
				
				PwEntryV3 newEntry = new PwEntryV3();
				
				newEntry.binaryDesc = mEntry.binaryDesc;
				//newEntry.groupId = mEntry.groupId;
				newEntry.groupId = getSelectedGroupId();
				newEntry.parent = (PwGroupV3)App.getDB().groups.get(new PwGroupIdV3(newEntry.groupId));

				if (mSelectedIconID == -1) {
					if (mIsNew) {
						newEntry.icon = App.getDB().pm.iconFactory.getIcon(0);
					} else {
						// Keep previous icon, if no new one was selected
						newEntry.icon = mEntry.icon;
					}
				}
				else {
					newEntry.icon = App.getDB().pm.iconFactory.getIcon(mSelectedIconID);
				}

				//newEntry.parent = mEntry.parent;
				newEntry.tCreation = mEntry.tCreation;
				newEntry.tExpire = mEntry.tExpire;
				newEntry.setUUID(mEntry.getUUID());
				
				Date now = Calendar.getInstance().getTime(); 
				newEntry.tLastAccess = new PwDate(now);
				newEntry.tLastMod = new PwDate(now);
				
				byte[] binaryData = mEntry.getBinaryData();
				if ( binaryData != null ) {
					newEntry.setBinaryData(binaryData, 0, binaryData.length);
				}

				newEntry.title = Util.getEditText(act, R.id.entry_title);
				newEntry.url = Util.getEditText(act, R.id.entry_url);
				newEntry.username = Util.getEditText(act, R.id.entry_user_name);
				newEntry.additional = Util.getEditText(act, R.id.entry_comment);
				byte[] password;
				try {
					password = pass.getBytes("UTF-8");
				} catch (UnsupportedEncodingException e) {
					assert false;
					password = pass.getBytes();
				}
				newEntry.setPassword(password, 0, password.length);

				if ( newEntry.title.equals(mEntry.title) ) {
					setResult(KeePass.EXIT_REFRESH);
				} else {
					setResult(KeePass.EXIT_REFRESH_TITLE);
				}
				
				RunnableOnFinish task;
				OnFinish onFinish = act.new AfterSave(new Handler());
				
				if ( mIsNew ) {
					task = AddEntry.getInstance(App.getDB(), newEntry, onFinish);
				} else {
					task = new UpdateEntry(App.getDB(), mEntry, newEntry, onFinish);
				}
				ProgressTask pt = new ProgressTask(act, task, R.string.saving_database);
				pt.run();
			}
			
		});
		
		// Cancel button
		Button cancel = (Button) findViewById(R.id.entry_cancel);
		cancel.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				finish();
				
			}
			
		});
		
		// Respect mask password setting
		if (mShowPassword) {
			EditText pass = (EditText) findViewById(R.id.entry_password);
			EditText conf = (EditText) findViewById(R.id.entry_confpassword);
			
			pass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
			conf.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
		}
	}
		
	private void fillGroupsAndSelect(int groupId) {
		mGroupsAdapter = new ArrayAdapter<PwGroup>(this, android.R.layout.simple_spinner_item);
		mGroupsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		Spinner spinner = (Spinner) findViewById(R.id.entry_parent_spinner);
		spinner.setAdapter(mGroupsAdapter);
		addGroups(App.getDB().groups.values());
		for(int i = 0; i < mGroupsAdapter.getCount(); ++i) {
			PwGroup item = mGroupsAdapter.getItem(i);
			if (item instanceof PwGroupV3 && ((PwGroupV3)item).groupId == groupId) {
				spinner.setSelection(i, false);
				return;
			}
		}
	}

	private void addGroups(Collection<PwGroup> groups) {
		if (groups.isEmpty()) {
			return;
		}
		for (PwGroup group : groups) {
			mGroupsAdapter.add(group);
//			addGroups(group.childGroups);
		}
	}

	protected int getSelectedGroupId() {
		Spinner spinner = (Spinner) findViewById(R.id.entry_parent_spinner);
		Object obj = spinner.getSelectedItem();
		if (obj instanceof PwGroupV3) {
			return ((PwGroupV3)obj).groupId;
		}
		return 0;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch (resultCode)
		{
			case RESULT_OK_ICON_PICKER:
				mSelectedIconID = data.getExtras().getInt(IconPickerActivity.KEY_ICON_ID);
				ImageButton currIconButton = (ImageButton) findViewById(R.id.icon_button);
				currIconButton.setImageResource(Icons.iconToResId(mSelectedIconID));
				break;
				
			case RESULT_OK_PASSWORD_GENERATOR:
				String generatedPassword = data.getStringExtra("com.keepassdroid.password.generated_password");
				EditText password = (EditText) findViewById(R.id.entry_password);
				EditText confPassword = (EditText) findViewById(R.id.entry_confpassword);
				
				password.setText(generatedPassword);
				confPassword.setText(generatedPassword);

				break;
			case Activity.RESULT_CANCELED:
			default:
				break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.entry_edit, menu);
		
		
		MenuItem togglePassword = menu.findItem(R.id.menu_toggle_pass);
		if ( mShowPassword ) {
			togglePassword.setTitle(R.string.menu_hide_password);
		} else {
			togglePassword.setTitle(R.string.show_password);
		}
		
		return true;
	}
	
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
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	private void setPasswordStyle() {
		TextView password = (TextView) findViewById(R.id.entry_password);
		TextView confpassword = (TextView) findViewById(R.id.entry_confpassword);

		if ( mShowPassword ) {
			password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
			confpassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

		} else {
			password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
			confpassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		}
	}

	private void fillData() {
		fillGroupsAndSelect(mEntry.groupId);
		ImageButton currIconButton = (ImageButton) findViewById(R.id.icon_button);
		App.getDB().drawFactory.assignDrawableTo(currIconButton, getResources(), mEntry.getIcon());
		
		populateText(R.id.entry_title, mEntry.title);
		populateText(R.id.entry_user_name, mEntry.getUsername());
		populateText(R.id.entry_url, mEntry.url);
		
		String password = new String(mEntry.getPassword());
		populateText(R.id.entry_password, password);
		populateText(R.id.entry_confpassword, password);
		setPasswordStyle();

		populateText(R.id.entry_comment, mEntry.additional);
	}

	private void populateText(int viewId, String text) {
		TextView tv = (TextView) findViewById(viewId);
		tv.setText(text);
	}
	
	private final class AfterSave extends OnFinish {

		public AfterSave(Handler handler) {
			super(handler);
		}

		@Override
		public void run() {
			if ( mSuccess ) {
				finish();
			} else {
				displayMessage(EntryEditActivity.this);
			}
		}
		
	}

}
