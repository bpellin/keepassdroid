/*
 * Copyright 2026 Brian Pellin.
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
package com.keepassdroid.tests.output;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.keepassdroid.Database;
import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.database.PwEntryV4;
import com.keepassdroid.database.edit.OnFinish;
import com.keepassdroid.database.edit.UpdateEntry;
import com.keepassdroid.database.edit.SaveDB;
import com.keepassdroid.database.exception.PwDbOutputException;
import com.keepassdroid.database.exception.PwDbOutputException.Reason;
import com.keepassdroid.database.save.PwDbOutput;
import com.keepassdroid.tests.database.TestData;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import static org.junit.Assert.*;

public class SaveFailureDiagnosticsTest {
    @Test
    public void saveFailureReportsCategoryOnceWithoutThrowing() {
        IOException original = new IOException("private provider path");
        PwDbOutputException failure = new PwDbOutputException(
                Reason.IO, "Failed to output header.", original);
        RecordingFinish finish = new RecordingFinish();

        new SaveDB(null, failingDatabase(failure), finish).run();

        assertEquals(1, finish.calls);
        assertFalse(finish.success);
        assertEquals("Database save failed [IO] (java.io.IOException)", finish.message);
        assertSame(original, failure.getCause());
    }

    @Test
    public void failedEntryUpdateRollsBackBeforeNotifyingCaller() {
        PwDatabaseV4 model = new PwDatabaseV4();
        final PwEntryV4 entry = new PwEntryV4();
        entry.setTitle("Original title", model);
        entry.setPassword("Original password", model);
        PwEntryV4 edited = (PwEntryV4) entry.clone();
        edited.setTitle("Edited title", model);
        edited.setPassword("Edited password", model);
        final java.util.Date modified = entry.getLastModificationTime();
        RecordingFinish finish = new RecordingFinish() {
            @Override
            public void run() {
                assertEquals("Original title", entry.getTitle());
                assertEquals("Original password", entry.getPassword());
                assertEquals(modified, entry.getLastModificationTime());
                super.run();
            }
        };
        PwDbOutputException failure = new PwDbOutputException(
                Reason.SERIALIZATION, "Invalid serializer state.");
        Database db = failingDatabase(failure);

        new UpdateEntry(null, db, entry, edited, finish).run();

        assertEquals(1, finish.calls);
        assertFalse(finish.success);
        assertEquals("Database save failed [SERIALIZATION]", finish.message);
        assertTrue(db.dirty.isEmpty());
    }

    private Database failingDatabase(final PwDbOutputException failure) {
        return new Database() {
            @Override
            public void SaveData(Context context) throws PwDbOutputException {
                throw failure;
            }
        };
    }

    private static class RecordingFinish extends OnFinish {
        int calls;
        boolean success;
        String message;

        @Override
        public void run() {
            calls++;
            success = mSuccess;
            message = mMessage;
        }
    }

    @Test
    public void kdbWriteFailurePreservesIOException() throws Exception {
        assertWriteFailure("test1.kdb", "12345");
    }

    @Test
    public void kdbx3WriteFailurePreservesIOException() throws Exception {
        assertWriteFailure("test.kdbx", "12345");
    }

    @Test
    public void kdbx4WriteFailurePreservesIOException() throws Exception {
        assertWriteFailure("test-kdbxv4.kdbx", "1");
    }

    private void assertWriteFailure(String asset, String password) throws Exception {
        Database db = load(asset, password);
        final IOException original = new IOException("Injected write failure");
        OutputStream failing = new OutputStream() {
            @Override
            public void write(int value) throws IOException {
                throw original;
            }
        };

        try {
            PwDbOutput.getInstance(db.pm, failing).output();
            fail("Expected write failure");
        } catch (PwDbOutputException e) {
            assertEquals(Reason.IO, e.getReason());
            assertSame(original, e.getCause());
        }
    }

    @Test
    public void unsupportedCipherIsDistinguishedFromWriteFailure() throws Exception {
        Database db = load("test.kdbx", "12345");
        ((PwDatabaseV4) db.pm).dataCipher = new UUID(0, 0);
        try {
            PwDbOutput.getInstance(db.pm, new ByteArrayOutputStream()).output();
            fail("Expected cipher failure");
        } catch (PwDbOutputException e) {
            assertEquals(Reason.CRYPTO, e.getReason());
            assertTrue(e.getCause() instanceof NoSuchAlgorithmException);
        }
    }

    private Database load(String asset, String password) throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        return TestData.GetDb(context, asset, password, "", "/unused");
    }
}
