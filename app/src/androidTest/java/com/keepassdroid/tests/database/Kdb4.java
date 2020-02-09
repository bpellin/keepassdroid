/*
 * Copyright 2010-2020 Brian Pellin.
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
package com.keepassdroid.tests.database;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.res.AssetManager;

import androidx.test.platform.app.InstrumentationRegistry;

import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.database.exception.InvalidDBException;
import com.keepassdroid.database.exception.PwDbOutputException;
import com.keepassdroid.database.load.Importer;
import com.keepassdroid.database.load.ImporterFactory;
import com.keepassdroid.database.load.ImporterV4;
import com.keepassdroid.database.save.PwDbOutput;
import com.keepassdroid.database.save.PwDbV4Output;
import com.keepassdroid.stream.CopyInputStream;
import com.keepassdroid.tests.TestUtil;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;

public class Kdb4 {
    Context ctx;

    @Test
    public void testDetection() throws IOException, InvalidDBException {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();

        AssetManager am = ctx.getAssets();
        InputStream is = am.open("test.kdbx", AssetManager.ACCESS_STREAMING);

        Importer importer = ImporterFactory.createImporter(is, ctx.getFilesDir());

        assertTrue(importer instanceof ImporterV4);
        is.close();

    }

    @Test
    public void testParsing() throws IOException, InvalidDBException {
        AssetManager am = ctx.getAssets();
        InputStream is = am.open("test.kdbx", AssetManager.ACCESS_STREAMING);

        ImporterV4 importer = new ImporterV4(ctx.getFilesDir());
        importer.openDatabase(is, "12345", null);

        is.close();


    }

    @Test
    public void testSavingKDBXV3() throws IOException, InvalidDBException, PwDbOutputException {
       testSaving("test.kdbx", "12345", "test-out.kdbx");
    }

    @Test
    public void testSavingKDBXV4() throws IOException, InvalidDBException, PwDbOutputException {
        testSaving("test-kdbxv4.kdbx", "1", "test-kdbxv4-out.kdbx");
    }

    private void testSaving(String inputFile, String password, String outputFile) throws IOException, InvalidDBException, PwDbOutputException {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();

        AssetManager am = ctx.getAssets();
        InputStream is = am.open(inputFile, AssetManager.ACCESS_STREAMING);

        ImporterV4 importer = new ImporterV4(ctx.getFilesDir());
        PwDatabaseV4 db = importer.openDatabase(is, password, null);
        is.close();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        PwDbV4Output output =  (PwDbV4Output) PwDbOutput.getInstance(db, bos);
        output.output();

        byte[] data = bos.toByteArray();

        FileOutputStream fos = new FileOutputStream(TestUtil.getAppPath(ctx, outputFile), false);

        InputStream bis = new ByteArrayInputStream(data);
        bis = new CopyInputStream(bis, fos);
        importer = new ImporterV4(ctx.getFilesDir());
        db = importer.openDatabase(bis, password, null);
        bis.close();

        fos.close();

    }

    @Before
    public void setUp() throws Exception {
        ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();

        TestUtil.extractKey(ctx, "keyfile.key", TestUtil.getAppPath(ctx,"key"));
        TestUtil.extractKey(ctx, "binary.key", TestUtil.getAppPath(ctx,"key-binary"));

    }

    @Test
    public void testComposite() throws IOException, InvalidDBException {
        AssetManager am = ctx.getAssets();
        InputStream is = am.open("keyfile.kdbx", AssetManager.ACCESS_STREAMING);

        ImporterV4 importer = new ImporterV4(ctx.getFilesDir());
        importer.openDatabase(is, "12345", TestUtil.getKeyFileInputStream(ctx, TestUtil.getAppPath(ctx, "key")));

        is.close();

    }

    @Test
    public void testCompositeBinary() throws IOException, InvalidDBException {
        AssetManager am = ctx.getAssets();
        InputStream is = am.open("keyfile-binary.kdbx", AssetManager.ACCESS_STREAMING);

        ImporterV4 importer = new ImporterV4(ctx.getFilesDir());
        importer.openDatabase(is, "12345", TestUtil.getKeyFileInputStream(ctx,TestUtil.getAppPath(ctx,"key-binary")));

        is.close();

    }

    @Test
    public void testKeyfile() throws IOException, InvalidDBException {
        AssetManager am = ctx.getAssets();
        InputStream is = am.open("key-only.kdbx", AssetManager.ACCESS_STREAMING);

        ImporterV4 importer = new ImporterV4(ctx.getFilesDir());
        importer.openDatabase(is, "", TestUtil.getKeyFileInputStream(ctx, TestUtil.getAppPath(ctx, "key")));

        is.close();


    }

    @Test
    public void testNoGzip() throws IOException, InvalidDBException {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();

        AssetManager am = ctx.getAssets();
        InputStream is = am.open("no-encrypt.kdbx", AssetManager.ACCESS_STREAMING);

        ImporterV4 importer = new ImporterV4(ctx.getFilesDir());
        importer.openDatabase(is, "12345", null);

        is.close();


    }

}
