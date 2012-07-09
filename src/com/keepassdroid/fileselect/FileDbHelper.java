/*
 * Copyright 2009 Brian Pellin.
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
package com.keepassdroid.fileselect;

import net.temerity.davsync.DAVException;
import net.temerity.davsync.DAVProfile;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class FileDbHelper {
	
	public static final String LAST_FILENAME = "lastFile";
	public static final String LAST_KEYFILE = "lastKey";
	
	private static final String DATABASE_NAME = "keepassdroid";
	private static final String FILE_TABLE = "files";
	private static final int DATABASE_VERSION = 2;
	
	private static final int MAX_FILES = 5;
	
	public static final String KEY_FILE_ID = "_id";
	public static final String KEY_FILE_FILENAME = "fileName";
	public static final String KEY_FILE_KEYFILE = "keyFile";
	public static final String KEY_FILE_UPDATED = "updated";

	private static final String DATABASE_CREATE = 
		"create table " + FILE_TABLE + " ( " + KEY_FILE_ID + " integer primary key autoincrement, " 
			+ KEY_FILE_FILENAME + " text not null, " + KEY_FILE_KEYFILE + " text, "
			+ KEY_FILE_UPDATED + " integer not null);";
	private static final String DATABASE_CREATE_DAV =
		"CREATE TABLE IF NOT EXISTS dav_profiles ( filename TEXT PRIMARY KEY, hostname TEXT, resource TEXT, username TEXT, password TEXT );";
	
	private final Context mCtx;
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		private final Context mCtx;
		
		DatabaseHelper(Context ctx) {
			super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
			mCtx = ctx;
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
			db.execSQL(DATABASE_CREATE_DAV);
			
			// Migrate preference to database if it is set.
			SharedPreferences settings = mCtx.getSharedPreferences("PasswordActivity", Context.MODE_PRIVATE); 
			String lastFile = settings.getString(LAST_FILENAME, "");
			String lastKey = settings.getString(LAST_KEYFILE,"");
						
			if ( lastFile.length() > 0 ) {
				ContentValues vals = new ContentValues();
				vals.put(KEY_FILE_FILENAME, lastFile);
				vals.put(KEY_FILE_UPDATED, System.currentTimeMillis());
				
				if ( lastKey.length() > 0 ) {
					vals.put(KEY_FILE_KEYFILE, lastKey);
				}
				
				db.insert(FILE_TABLE, null, vals);
				
				// Clear old preferences
				deletePrefs(settings);
				
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// this new database will also contain DAV profiles
			if( oldVersion == 1 && newVersion == 2 ) {
				db.execSQL(DATABASE_CREATE_DAV);
			}
		}
		
		private void deletePrefs(SharedPreferences prefs) {
			// We won't worry too much if this fails
			try {
				SharedPreferences.Editor editor = prefs.edit();
				editor.remove(LAST_FILENAME);
				editor.remove(LAST_KEYFILE);
				editor.commit();
			} catch (Exception e) {
				assert(true);
			}
		}
	}
	
	public void addDavProfile(DAVProfile p) {
		if( ! isOpen() ) {
			open();
		}
		String q = "INSERT OR REPLACE INTO dav_profiles VALUES('"
			   + p.getFilename() + "','"
			   + p.getHostname() + "','"
			   + p.getResource() + "','"
			   + p.getUsername() + "','"
			   + p.getPassword() + "');";
		mDb.execSQL(q);
	}
	
	public void delDavProfile(String filename) {
		mDb.execSQL("DELETE FROM dav_profiles WHERE filename LIKE '" + filename + "';");
	}
	
	public DAVProfile getDavProfile(String filename) throws DAVException  {
		if( ! isOpen() ) {
			open();
		}
		Cursor c = mDb.rawQuery("SELECT * FROM dav_profiles WHERE filename = '" + filename + "';", null);
		if( c.getCount() == 0 ) {
			throw new DAVException();
		} else {
			c.moveToFirst();
			return new DAVProfile(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4));
		}
	}
	
	public FileDbHelper(Context ctx) {
		mCtx = ctx;
	}
	
	public FileDbHelper open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}
	
	public boolean isOpen() {
		return mDb.isOpen();
	}
	
	public void close() {
		mDb.close();
	}
	
	public long createFile(String fileName, String keyFile) {
		
		// Check to see if this filename is already used
		Cursor cursor;
		try {
			cursor = mDb.query(true, FILE_TABLE, new String[] {KEY_FILE_ID}, 
					KEY_FILE_FILENAME + "=?", new String[] {fileName}, null, null, null, null);
		} catch (Exception e ) {
			assert(true);
			return -1;
		}
		
		long result;
		// If there is an existing entry update it with the new key file
		if ( cursor.getCount() > 0 ) {
			cursor.moveToFirst();
			long id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_FILE_ID));
			
			ContentValues vals = new ContentValues();
			vals.put(KEY_FILE_KEYFILE, keyFile);
			vals.put(KEY_FILE_UPDATED, System.currentTimeMillis());
			
			result = mDb.update(FILE_TABLE, vals, KEY_FILE_ID + " = " + id, null);
		
		// Otherwise add the new entry
		} else {
			ContentValues vals = new ContentValues();
			vals.put(KEY_FILE_FILENAME, fileName);
			vals.put(KEY_FILE_KEYFILE, keyFile);
			vals.put(KEY_FILE_UPDATED, System.currentTimeMillis());
			
			result = mDb.insert(FILE_TABLE, null, vals);
			
		}
		// Delete all but the last five records
		try {
			deleteAllBut(MAX_FILES);
		} catch (Exception e) {
			e.printStackTrace();
			assert(true);
		}
		
		cursor.close();
		
		return result;
		
	}
	
	private void deleteAllBut(int limit) {
		Cursor cursor = mDb.query(FILE_TABLE, new String[] {KEY_FILE_UPDATED}, null, null, null, null, KEY_FILE_UPDATED);
		
		if ( cursor.getCount() > limit ) {
			cursor.moveToFirst();
			long time = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_FILE_UPDATED));
			
			mDb.execSQL("DELETE FROM " + FILE_TABLE + " WHERE " + KEY_FILE_UPDATED + "<" + time + ";");
		}
		
		cursor.close();
		
	}
	
	public void deleteAllKeys() {
		ContentValues vals = new ContentValues();
		vals.put(KEY_FILE_KEYFILE, "");
		
		mDb.update(FILE_TABLE, vals, null, null);
	}
	
	public void deleteFile(String filename) {
		mDb.delete(FILE_TABLE, KEY_FILE_FILENAME + " = ?", new String[] {filename});
	}
	
	
	public Cursor fetchAllFiles() {
		Cursor ret;
		ret = mDb.query(FILE_TABLE, new String[] {KEY_FILE_ID, KEY_FILE_FILENAME, KEY_FILE_KEYFILE }, null, null, null, null, KEY_FILE_UPDATED + " DESC", Integer.toString(MAX_FILES));
		return ret;
	}
	
	public Cursor fetchFile(long fileId) throws SQLException {
		Cursor cursor = mDb.query(true, FILE_TABLE, new String[] {KEY_FILE_FILENAME, KEY_FILE_KEYFILE}, 
				KEY_FILE_ID + "=" + fileId, null, null, null, null, null);
		
		if ( cursor != null ) {
			cursor.moveToFirst();
		}
		
		return cursor;
		
	}
	
	public String getFileByName(String name) {
		Cursor cursor = mDb.query(true, FILE_TABLE, new String[] {KEY_FILE_KEYFILE}, 
				KEY_FILE_FILENAME + "= ?", new String[] {name}, null, null, null, null);
		
		if ( cursor == null ) {
			return "";
		}
		
		String filename;
		
		if ( cursor.moveToFirst() ) {
			filename = cursor.getString(0);
		} else {
			// Cursor is empty
			filename = "";
		}
		cursor.close();
		return filename;
	}
	
	public boolean hasRecentFiles() {
		Cursor cursor = fetchAllFiles();
		
		boolean hasRecent = cursor.getCount() > 0;
		cursor.close();
		
		return hasRecent; 
	}
}
