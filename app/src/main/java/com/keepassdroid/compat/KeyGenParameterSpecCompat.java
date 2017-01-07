/*
 * Copyright 2017 Brian Pellin.
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
package com.keepassdroid.compat;

import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.KeyGenerator;

public class KeyGenParameterSpecCompat {
    private static Class kgpspBuilder;
    private static Constructor constructor;
    private static Method setBlockModes;
    private static Method setUserAuthenticationRequired;
    private static Method setEncryptionPaddings;
    private static Method build;
    private static boolean useKgps = false;

    static {
        try {
            kgpspBuilder = Class.forName("android.security.keystore.KeyGenParameterSpec$Builder");
            constructor = kgpspBuilder.getConstructor(String.class, int.class);
            setUserAuthenticationRequired = kgpspBuilder.getMethod("setUserAuthenticationRequired", boolean.class);
            setBlockModes = kgpspBuilder.getDeclaredMethod("setBlockModes", String[].class);
            setEncryptionPaddings = kgpspBuilder.getMethod("setEncryptionPaddings", String[].class);
            build = kgpspBuilder.getMethod("build");


            useKgps = true;
        } catch (ClassNotFoundException e) {
            Log.d("KeePassDroid", "KeyGenParameterSpecCompat", e);
        } catch (Exception e) {
            Log.d("KeePassDroid", "KeyGenParameterSpecCompat", e);
        }

    }

    public static void init(KeyGenerator keyGenerator, String keystoreName) {
        try {
            if (useKgps) {
                Object builder = constructor.newInstance(keystoreName, KeyPropertiesCompat.ENCRYPT | KeyPropertiesCompat.DECRYPT);
                builder = setBlockModes.invoke(builder, new Object[]{new String[]{KeyPropertiesCompat.BLOCK_MODE_CBC}});
                builder = setUserAuthenticationRequired.invoke(builder, true);
                builder = setEncryptionPaddings.invoke(builder, new Object[]{new String[]{KeyPropertiesCompat.ENCRYPTION_PADDING_PKCS7}});
                AlgorithmParameterSpec spec = (AlgorithmParameterSpec) build.invoke(builder);
                keyGenerator.init(spec);
            }
        } catch (Exception e) {
            Log.d("KeePassDroid", "KeyGenParameterSpecCompat", e);
            // Do nothing rely on fallback
        }
    }
}
