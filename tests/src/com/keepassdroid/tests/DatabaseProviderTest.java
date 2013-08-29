package com.keepassdroid.tests;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.test.AndroidTestCase;

import com.keepassdroid.provider.Contract;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileOutputStream;

import static org.junit.Assert.*;

public class DatabaseProviderTest extends AndroidTestCase {
    public void testProvider() {
        ContentResolver r = getContext().getContentResolver();
        Cursor c = r.query(Contract.URI, null, "yo", null, null);
        assertNull("cursor must be initially null", c);
        Bundle b = new Bundle();

        InputStream in = null;
        File temp = null;
        try {
            temp = File.createTempFile(
                "test1", "kdb", Environment.getExternalStorageDirectory());
            in = getContext().getAssets().open("test1.kdb");
            FileOutputStream out = new FileOutputStream(temp);
            byte[] buf = new byte[32768];
            int read;
            while ((read = in.read(buf, 0, 32768)) != -1) {
                out.write(buf, 0, read);
            }
            out.close();

            b.putString(Contract.EXTRA_DATABASE,
                    temp.getAbsolutePath());
            b.putString(Contract.EXTRA_PASSWORD, "12345");
            b = r.call(Contract.URI, Contract.METHOD_OPEN, null, b);
            c = r.query(Contract.URI, null, "Amazon", null, null);
            assertTrue("no search results for amazon", c.moveToNext());
            long id = c.getLong(c.getColumnIndex(Contract._ID));
            assertEquals("Amazon", c.getString(
                    c.getColumnIndex(Contract.TITLE)));
            assertEquals("frank", c.getString(
                    c.getColumnIndex(Contract.USERNAME)));
            assertEquals("12345", c.getString(
                    c.getColumnIndex(Contract.PASSWORD)));
            assertEquals("http://www.amazon.com", c.getString(
                    c.getColumnIndex(Contract.URL)));
            assertEquals("Comment", c.getString(
                    c.getColumnIndex(Contract.NOTES)));
            c.close();
            c = r.query(Contract.uri(id), null, null, null, null);
            assertTrue("no search results for: " + id, c.moveToNext());
            assertEquals(id, c.getLong(0));
            assertEquals("Amazon", c.getString(
                    c.getColumnIndex(Contract.TITLE)));
            assertFalse("more than 1 result?!", c.moveToNext());
            assertTrue(b.getString(Contract.EXTRA_ERROR),
                    !b.containsKey(Contract.EXTRA_ERROR));
            c.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            temp.delete();
            try {
                in.close();
            } catch (IOException ex) { }
        }
    }
}