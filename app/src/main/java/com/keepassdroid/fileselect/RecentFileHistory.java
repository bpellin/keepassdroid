/*
 * Copyright 2013-2025 Brian Pellin.
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.android.keepass.R;
import com.keepassdroid.utils.UriUtil;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Looper;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class RecentFileHistory {

    private static String DB_KEY = "recent_databases";
    private static String KEYFILE_KEY = "recent_keyfiles";

    private final MutableLiveData<List<String>> databases =
            new MutableLiveData<List<String>>(new CopyOnWriteArrayList<String>());
    private List<String> keyfiles = new ArrayList<String>();
    private Context ctx;
    private SharedPreferences prefs;
    private OnSharedPreferenceChangeListener listner;
    private boolean enabled;
    private boolean init = false;

    public RecentFileHistory(Context c) {
        ctx = c.getApplicationContext();

        prefs = PreferenceManager.getDefaultSharedPreferences(c);
        enabled = prefs.getBoolean(ctx.getString(R.string.recentfile_key), ctx.getResources().getBoolean(R.bool.recentfile_default));
        listner = new OnSharedPreferenceChangeListener() {

            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                    String key) {
                if (key.equals(ctx.getString(R.string.recentfile_key))) {
                    enabled = sharedPreferences.getBoolean(ctx.getString(R.string.recentfile_key), ctx.getResources().getBoolean(R.bool.recentfile_default));
                }
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(listner);
    }

    private synchronized void init() {
        if (!init) {
            if (!upgradeFromSQL()) {
                loadPrefs();
            }

            init = true;
        }
    }

    private boolean upgradeFromSQL() {

        try {
            // Check for a database to upgrade from
            if (!sqlDatabaseExists()) {
                return false;
            }

            List<String> files = databases.getValue();
            files.clear();

            keyfiles.clear();

            FileDbHelper helper = new FileDbHelper(ctx);
            helper.open();
            Cursor cursor = helper.fetchAllFiles();

            int dbIndex = cursor.getColumnIndex(FileDbHelper.KEY_FILE_FILENAME);
            int keyIndex = cursor.getColumnIndex(FileDbHelper.KEY_FILE_KEYFILE);

            if(cursor.moveToFirst()) {
                while (cursor.moveToNext()) {
                    String filename = cursor.getString(dbIndex);
                    String keyfile = cursor.getString(keyIndex);

                    files.add(filename);
                    keyfiles.add(keyfile);
                }
            }

            updateDatabases(files);

            savePrefs();

            cursor.close();
            helper.close();

        } catch (Exception e) {
            // If upgrading fails, we'll just give up on it.
        }

        try {
            FileDbHelper.deleteDatabase(ctx);
        } catch (Exception e) {
            // If we fail to delete it, just move on
        }

        return true;
    }

    private boolean sqlDatabaseExists() {
        File db = ctx.getDatabasePath(FileDbHelper.DATABASE_NAME);
        return db.exists();
    }

    public void createFile(Uri uri, Uri keyUri) {
        if (!enabled || uri == null) return;

        init();

        // Remove any existing instance of the same filename
        deleteFile(uri, false);

        List<String> files = databases.getValue();
        files.add(0, uri.toString());
        updateDatabases(files);

        String key = (keyUri == null) ? "" : keyUri.toString();
        keyfiles.add(0, key);

        trimLists();
        savePrefs();
    }

    public boolean hasRecentFiles() {
        if (!enabled) return false;

        init();

        return databases.getValue().size() > 0;
    }

    public String getDatabaseAt(int i) {
        init();

        List<String> files = databases.getValue();
        if (i < files.size()) {
            return files.get(i);
        } else {
            return "";
        }
    }

    public String getKeyfileAt(int i) {
        init();
        if (i < keyfiles.size()) {
            return keyfiles.get(i);
        } else {
            return "";
        }
    }

    private void loadPrefs() {
        List<String> files = databases.getValue();
        loadList(files, DB_KEY);
        updateDatabases(files);

        loadList(keyfiles, KEYFILE_KEY);
    }

    private void savePrefs() {
        saveList(DB_KEY, databases.getValue());
        saveList(KEYFILE_KEY, keyfiles);
    }

    private void loadList(List<String> list, String keyprefix) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        int size = prefs.getInt(keyprefix, 0);

        list.clear();
        for (int i = 0; i < size; i++) {
            list.add(prefs.getString(keyprefix + "_" + i, ""));
        }
    }

    private void saveList(String keyprefix, List<String> list) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor edit = prefs.edit();
        int size = list.size();
        edit.putInt(keyprefix, size);

        for (int i = 0; i < size; i++) {
            edit.putString(keyprefix + "_" + i, list.get(i));
        }
        edit.apply();;
    }

    public void deleteFile(Uri uri) {
        deleteFile(uri, true);
    }

    public void deleteFile(Uri uri, boolean save) {
        init();

        String uriName = uri.toString();
        String fileName = uri.getPath();

        List<String> files = databases.getValue();
        for (int i = 0; i < files.size(); i++) {
            String entry = files.get(i);
            boolean delete;
            delete = (uriName != null && uriName.equals((entry)) ||
                    (fileName != null && fileName.equals(entry)));

            if (delete) {
                files.remove(i);
                updateDatabases(files);
                keyfiles.remove(i);
                break;
            }
        }

        if (save) {
            savePrefs();
        }
    }

    public @NonNull LiveData<List<String>> getDbList() {
        init();
        return databases;
    }

    public Uri getFileByName(Uri database) {
        if (!enabled) return null;

        init();

        List<String> files = databases.getValue();
        int size = files.size();
        for (int i = 0; i < size; i++) {
            if (UriUtil.equalsDefaultfile(database,files.get(i))) {
                return UriUtil.parseDefaultFile(keyfiles.get(i));
            }
        }

        return null;
    }

    public void deleteAll() {
        init();

        List<String> files = databases.getValue();
        files.clear();
        updateDatabases(files);

        keyfiles.clear();

        savePrefs();
    }

    public void deleteAllKeys() {
        init();

        keyfiles.clear();

        int size = databases.getValue().size();
        for (int i = 0; i < size; i++) {
            keyfiles.add("");
        }

        savePrefs();
    }

    private void trimLists() {
        List<String> files = databases.getValue();
        boolean updated = false;
        int size = files.size();
        for (int i = FileDbHelper.MAX_FILES; i < size; i++) {
            if (i < files.size()) {
                files.remove(i);
                updated = true;
            }

            if (i < keyfiles.size()) {
                keyfiles.remove(i);
            }
        }

        if (updated) {
            updateDatabases(files);
        }
    }

    private void updateDatabases(List<String> files) {
        // On the main thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
            databases.setValue(files);
        } else {
            databases.postValue(files);
        }
    }
}
