/*
 * Copyright (C) 2017 Brian Pellin
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.keepassdroid;

//import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.view.inputmethod.InputMethodManager;

import com.keepassdroid.compat.KeyPropertiesCompat;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;

import dagger.Module;
import dagger.Provides;

/**
 * Dagger module for Fingerprint APIs.
 */
@Module(
        library = true,
        injects = {PasswordActivity.class}
)
//@TargetApi(23)
public class FingerprintModule {

    private final Context mContext;
    private final String FINGERPRINT_SERVICE = "fingerprint";

    public FingerprintModule(Context context) {
        mContext = context;
    }

    @Provides
    public Context providesContext() {
        return mContext;
    }

    @Provides
    public FingerprintManagerCompat providesFingerprintManager(Context context) {
        return FingerprintManagerCompat.from(context);
    }

    @Provides
    public KeyguardManager providesKeyguardManager(Context context) {
        return (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
    }

    @Provides
    public KeyStore providesKeystore() {
        try {
            return KeyStore.getInstance("AndroidKeyStore");
        } catch (KeyStoreException e) {
            throw new RuntimeException("Failed to get an instance of KeyStore", e);
        }
    }

    @Provides
    public KeyGenerator providesKeyGenerator() {
        try {
            return KeyGenerator.getInstance(KeyPropertiesCompat.KEY_ALGORITHM_AES, "AndroidKeyStore");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException("Failed to get an instance of KeyGenerator", e);
        }
    }

    @Provides
    public Cipher providesCipher(KeyStore keyStore) {
        try {
            return Cipher.getInstance(KeyPropertiesCompat.KEY_ALGORITHM_AES + "/"
                    + KeyPropertiesCompat.BLOCK_MODE_CBC + "/"
                    + KeyPropertiesCompat.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get an instance of Cipher", e);
        }
    }

    @Provides
    public InputMethodManager providesInputMethodManager(Context context) {
        return (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Provides
    public SharedPreferences providesSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
