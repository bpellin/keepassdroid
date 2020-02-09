/*
 * Copyright 2018 Brian Pellin.
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

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import static org.junit.Assert.assertArrayEquals;

import com.keepassdroid.database.security.ProtectedBinary;
import com.keepassdroid.utils.Util;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

public class ProtectedBinaryTest {
    @Test
    public void testEncryption() throws Exception {
        byte[] input = new byte[4096];
        Random random = new Random();
        random.nextBytes(input);

        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File dir = ctx.getFilesDir();
        File temp = new File(dir, "1");

        ProtectedBinary pb = new ProtectedBinary(true, temp, input.length);
        OutputStream os = pb.getOutputStream();
        ByteArrayInputStream bais = new ByteArrayInputStream(input);

        Util.copyStream(bais, os);
        os.close();

        InputStream is = pb.getData();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Util.copyStream(is, baos);
        byte[] output = baos.toByteArray();

        assertArrayEquals(input, output);

    }
}
