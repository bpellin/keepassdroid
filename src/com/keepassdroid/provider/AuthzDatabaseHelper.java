package com.keepassdroid.provider;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * @author pfn
 */
public class AuthzDatabaseHelper {
    final static String TABLE_AUTHZ = "authz";

    final static String FIELD_UID        = "uid";
    final static String FIELD_SIGNATURES = "signatures";
    final static String FIELD_LAST_AUTHZ = "last_authz";
    final static String FIELD_AUTHZ      = "authz";
    final static String FIELD_REMEMBER   = "remember";

    final static String TABLE_AUTHZ_DDL = "CREATE TABLE " + TABLE_AUTHZ +
            " ( " +
            FIELD_UID        + " INTEGER PRIMARY KEY, " +
            FIELD_SIGNATURES + " TEXT NOT NULL, " +
            FIELD_LAST_AUTHZ + " INTEGER NOT NULL, " +
            FIELD_AUTHZ      + " INTEGER NOT NULL, " +
            FIELD_REMEMBER   + " INTEGER NOT NULL " +
            ");";
    final static String DATABASE_NAME = "provider_authz";
    final static int DATABASE_VERSION = 1;
    AuthzDatabaseHelper(Context c) {
        helper = new SQLiteOpenHelper(
                c, DATABASE_NAME, null, DATABASE_VERSION) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL(TABLE_AUTHZ_DDL);
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int i, int i2) { }
        };
    }

    private final SQLiteOpenHelper helper;

    Authz getByUid(int uid) {
        SQLiteDatabase db = helper.getReadableDatabase();

        try {
            Cursor c = db.query(TABLE_AUTHZ,
                    new String[] {
                            FIELD_UID,
                            FIELD_SIGNATURES,
                            FIELD_LAST_AUTHZ,
                            FIELD_AUTHZ,
                            FIELD_REMEMBER
                    },
                    FIELD_UID + " = ?",
                    new String[] { String.valueOf(uid) },
                    null, null, null);
            if (c.moveToNext()) {
                int id = c.getInt(0);
                String sigs = c.getString(1);
                long lastAuthz = c.getLong(2);
                boolean authz = c.getInt(3) != 0;
                boolean remember = c.getInt(4) != 0;
                return new Authz(id, sigs, lastAuthz, authz, remember);
            } else {
                return new Authz(uid);
            }
        } finally {
            db.close();
        }
    }

    void clear() {
        SQLiteDatabase db = helper.getWritableDatabase();
        try {
            db.delete(TABLE_AUTHZ, null, null);
        } finally {
            db.close();
        }
    }

    void save(Authz a) {
        SQLiteDatabase db = helper.getWritableDatabase();
        try {
            if (a.isNew) {
                db.insertOrThrow(TABLE_AUTHZ, null, a.values());
            } else {
                int rows = db.update(TABLE_AUTHZ, a.values(),
                        FIELD_UID + " = ?",
                        new String[]{ String.valueOf(a.uid) });
                if (rows != 1)
                    throw new IllegalStateException(
                            "Wrong number of rows updated: " + rows);
            }
        } finally {
            db.close();
        }
    }
}
