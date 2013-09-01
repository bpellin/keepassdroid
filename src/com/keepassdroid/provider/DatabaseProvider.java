package com.keepassdroid.provider;

import android.content.*;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.util.Log;
import com.android.keepass.BuildConfig;
import com.keepassdroid.Database;
import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.exception.InvalidDBException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class DatabaseProvider extends ContentProvider {

    private final static String TAG = "DatabaseProvider";

    /**
     * 30 second authz dialog timeout
     */
    static final long AUTHZ_TIMEOUT = 30 * 1000;
    /**
     * 15 minute threshold for re-prompting access if allowed but not always.
     *
     * Use a shorter timeout in debug
     */
    static final long AUTHZ_THRESHOLD = !BuildConfig.DEBUG ?
            15 * 60 * 1000 : 60000;
    /**
     * 1 minute threshold for re-prompting access if denied
     */
    static final long FAIL_THRESHOLD = 60 * 1000;

    static final String ACTION_AUTHZ_SUCCESS =
            "com.keepassdroid.action.AUTHZ_SUCCESS";
    static final String ACTION_AUTHZ_FAIL =
            "com.keepassdroid.action.AUTHZ_FAIL";

    static final String EXTRA_TOKEN = "com.keepassdroid.extra.TOKEN";
    static final String EXTRA_UID   = "com.keepassdroid.extra.UID";

    private static final UriMatcher URI_MATCHER;

    private static final int BY_SEARCH = 1;
    private static final int BY_ID     = 2;

    static {
        URI_MATCHER = new UriMatcher(0);
        URI_MATCHER.addURI("com.keepassdroid.provider", "entries",   BY_SEARCH);
        URI_MATCHER.addURI("com.keepassdroid.provider", "entries/#", BY_ID);
    }
    private volatile int startingUid;
    private Database db;
    private AuthzDatabaseHelper dbHelper;

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (Contract.METHOD_OPEN.equals(method)) {
            startingUid = Binder.getCallingUid();
            Log.v(TAG, "content provider started by uid: " + startingUid);
            String database = extras.getString(Contract.EXTRA_DATABASE);
            String password = extras.getString(Contract.EXTRA_PASSWORD);
            String keyfile = extras.getString(Contract.EXTRA_KEYFILE);
            Database d = new Database();
            try {
                d.LoadData(getContext(), database,
                        password == null ? "" : password,
                        keyfile == null ? "" : keyfile);
                db = d;
            } catch (IOException e) {
                extras.putString(Contract.EXTRA_ERROR, e.toString());
            } catch (InvalidDBException e) {
                extras.putString(Contract.EXTRA_ERROR, e.toString());
            }
            return extras;
        } else
            return super.call(method, arg, extras);
    }

    @Override
    public boolean onCreate() {
        dbHelper = new AuthzDatabaseHelper(getContext());
        return true;
    }

    private boolean checkAuthz(int uid) {
        // they passed us the master creds, they automatically get access
        if (startingUid == uid) return true;

        Authz authz = dbHelper.getByUid(uid);
        authz.checkSignatures(getContext());
        final boolean[] allowed = { authz.authz };
        long now = System.currentTimeMillis();
        long elapsed = now - authz.lastAuthz;
        if ((!authz.authz && elapsed > FAIL_THRESHOLD) ||
                (!authz.remember && elapsed > AUTHZ_THRESHOLD) ||
                !authz.checkSignatures(getContext())) {
            final Object lock = new Object();
            final String token = UUID.randomUUID().toString();
            BroadcastReceiver r = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    String tok = intent.getStringExtra(EXTRA_TOKEN);
                    if (token.equals(tok)) {
                        if (ACTION_AUTHZ_FAIL.equals(action)) {
                            synchronized(lock) {
                                allowed[0] = false;
                                lock.notify();
                            }
                        } else if (
                                ACTION_AUTHZ_SUCCESS.equals(action)) {
                            synchronized(lock) {
                                allowed[0] = true;
                                lock.notify();
                            }
                        }
                    }
                }
            };
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_AUTHZ_FAIL);
            filter.addAction(ACTION_AUTHZ_SUCCESS);
            getContext().registerReceiver(r, filter);
            Intent intent = new Intent(
                    getContext(), AuthzActivity.class);
            intent.putExtra(EXTRA_TOKEN, token);
            intent.putExtra(EXTRA_UID, uid);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

            // start authz activity, wait until processed or timed out
            getContext().startActivity(intent);
            try {
                synchronized(lock) {
                    lock.wait(AUTHZ_TIMEOUT);
                }
            } catch (InterruptedException ignored) { }
            getContext().unregisterReceiver(r);
        }
        return allowed[0];
    }
    @Override
    public Cursor query(Uri uri, String[] projection, String query,
                        String[] params, String order) {
        if (db == null) {
            return null;
        } else {
            int uid = android.os.Binder.getCallingUid();
            if (!checkAuthz(uid)) return new PwCursor();

            switch (URI_MATCHER.match(uri)) {
                case BY_SEARCH:
                    if (query == null)
                        return new PwCursor();
                    return new PwCursor(db.Search(query));
                case BY_ID:
                    String sid = uri.getLastPathSegment();
                    long id = Long.parseLong(sid);
                    for (UUID uuid : db.pm.entries.keySet()) {
                        if (id == Math.abs(uuid.getLeastSignificantBits()))
                            return new PwCursor(db.pm.entries.get(uuid));
                    }
            }
            return new PwCursor();
        }
    }

    @Override
    public String getType(Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case BY_SEARCH: return "vnd.android.cursor.dir/PwGroup";
            case BY_ID:     return "vnd.android.cursor.item/PwEntry";
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        return null; // noop
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        return 0; // noop
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (db != null) {
            db.clear();
            db = null;
        }
    }

    @Override
    public int update(Uri u, ContentValues values, String s, String[] strings) {
        return 0; // noop
    }

    private static class PwCursor extends AbstractCursor {
        private final List<PwEntry> entries;

        PwCursor() {
            entries = Arrays.asList();
        }
        PwCursor(PwGroup group) {
            entries = group == null ? Arrays.<PwEntry>asList() : group.childEntries;
        }
        PwCursor(PwEntry entry) {
            entries = Arrays.asList(entry);
        }
        @Override
        public int getCount() {
            return entries.size();
        }

        @Override
        public String[] getColumnNames() {
            return Contract.COLUMNS;
        }

        @Override
        public String getString(int i) {
            switch (i) {
                case 1: return entry().getTitle();
                case 2: return entry().getUsername();
                case 3: return entry().getPassword();
                case 4: return entry().getUrl();
                case 5: return entry().getNotes();
            }
            return null;
        }

        @Override
        public int getType(int column) {
            switch (column) {
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                    return Cursor.FIELD_TYPE_STRING;
                case 0:
                case 6:
                case 7:
                case 8:
                case 9:
                    return Cursor.FIELD_TYPE_INTEGER;
            }
            return Cursor.FIELD_TYPE_NULL;
        }

        private PwEntry entry() {
            return entries.get(mPos);
        }

        @Override
        public short getShort(int i) {
            return (short) getLong(i);
        }

        @Override
        public int getInt(int i) {
            return (int) getLong(i);
        }

        @Override
        public long getLong(int i) {
            switch (i) {
                case 6: return entry().getCreationTime().getTime();
                case 7: return entry().getLastModificationTime().getTime();
                case 8: return entry().getLastAccessTime().getTime();
                case 9: return entry().getExpiryTime().getTime();
                case 0: return Math.abs(
                        entry().getUUID().getLeastSignificantBits());
            }
            return -1;
        }

        @Override
        public float getFloat(int i) {
            return getLong(i);
        }

        @Override
        public double getDouble(int i) {
            return getLong(i);
        }

        @Override
        public boolean isNull(int i) {
            return getLong(i) != -1 || getString(i) != null;
        }
    }

}
