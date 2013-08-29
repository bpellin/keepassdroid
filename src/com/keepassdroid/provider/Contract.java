package com.keepassdroid.provider;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * @author pfn
 */
public class Contract {
    // long
    public final static String _ID = BaseColumns._ID;

    // string fields
    public final static String TITLE = "title";
    public final static String USERNAME = "username";
    public final static String PASSWORD = "password";
    public final static String URL = "url";
    public final static String NOTES = "notes";

    // long fields
    public final static String CREATION_TIME = "creationTime";
    public final static String LAST_MODIFIED = "lastModified";
    public final static String LAST_ACCESSED = "lastAccessed";
    public final static String EXPIRY = "expiry";

    public final static String[] COLUMNS = {
            _ID,
            TITLE,
            USERNAME,
            PASSWORD,
            URL,
            NOTES,
            CREATION_TIME,
            LAST_MODIFIED,
            LAST_ACCESSED,
            EXPIRY
    };
    public final static Uri URI = new Uri.Builder()
            .scheme("content")
            .authority("com.keepassdroid.provider")
            .appendPath("entries")
            .build();
    public static final String METHOD_OPEN = "open";
    public static final String EXTRA_DATABASE =
            "com.keepassdroid.extra.DATABASE";
    public static final String EXTRA_PASSWORD =
            "com.keepassdroid.extra.PASSWORD";
    public static final String EXTRA_KEYFILE =
            "com.keepassdroid.extra.KEYFILE";
    public static final String EXTRA_ERROR = "com.keepassdroid.extra.ERROR";

    public static Uri uri(long id) {
        return URI.buildUpon().appendPath(String.valueOf(id)).build();
    }
}
