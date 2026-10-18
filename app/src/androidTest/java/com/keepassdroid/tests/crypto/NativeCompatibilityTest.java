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
package com.keepassdroid.tests.crypto;

import com.keepassdroid.crypto.NativeLib;
import com.keepassdroid.crypto.keyDerivation.Argon2Native;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class NativeCompatibilityTest {
    @Test
    public void nativeArgon2MatchesIndependentImplementation() throws Exception {
        assertTrue("Both native crypto libraries must load without Java fallback", NativeLib.init());
        byte[] password = "page-size-regression".getBytes("UTF-8");
        byte[] salt = "keepassdroid-salt".getBytes("UTF-8");
        for (int type : new int[]{Argon2Parameters.ARGON2_d, Argon2Parameters.ARGON2_id}) {
            Argon2BytesGenerator reference = new Argon2BytesGenerator();
            reference.init(new Argon2Parameters.Builder(type)
                    .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                    .withSalt(salt).withParallelism(1).withMemoryAsKB(32).withIterations(2)
                    .build());
            byte[] expected = new byte[32];
            reference.generateBytes(password, expected);
            assertArrayEquals(expected, Argon2Native.transformKey(password, salt,
                    1, 32 * 1024, 2, null, null, 0x13, type));
        }
    }
}
