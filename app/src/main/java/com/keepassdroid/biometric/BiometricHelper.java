/*
 * Copyright 2017-2020 Hans Cappelle, Brian Pellin
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
package com.keepassdroid.biometric;

import android.app.KeyguardManager;
import android.content.Context;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.os.CancellationSignal;

import com.keepassdroid.compat.KeyGenParameterSpecCompat;
import com.keepassdroid.compat.KeyguardManagerCompat;

import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class BiometricHelper {

    private static final String ALIAS_KEY = "example-key";

    private BiometricManager biometricManager;
    private KeyStore keyStore = null;
    private KeyGenerator keyGenerator = null;
    private Cipher cipher = null;
    private KeyguardManager keyguardManager = null;
    private BiometricPrompt.CryptoObject cryptoObject = null;

    private boolean initOk = false;
    private boolean cryptoInitOk = false;
    private BiometricCallback biometricCallback;
    private CancellationSignal cancellationSignal;

    public interface BiometricCallback {

        void handleEncryptedResult(String value, String ivSpec);

        void handleDecryptedResult(String value);

        void onInvalidKeyException();

        void onException();

        void onException(boolean showWarningMessage);

        void onException(CharSequence message);

        void onException(int resId);

        void onKeyInvalidated();
    }

    public BiometricHelper(
            final Context context,
            final BiometricCallback biometricCallback) {

        this.biometricManager = BiometricManager.from(context);
        this.keyguardManager = (KeyguardManager)context.getSystemService(Context.KEYGUARD_SERVICE);

        if (!isBiometricSupported()) {
            // really not much to do when no fingerprint support found
            setInitOk(false);
            return;
        }
        this.biometricCallback = biometricCallback;

        try {
            this.keyStore = KeyStore.getInstance("AndroidKeyStore");
            this.keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    "AndroidKeyStore");
            this.cipher = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/"
                            + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            this.cryptoObject = new BiometricPrompt.CryptoObject(cipher);
            setInitOk(true);
        } catch (final Exception e) {
            setInitOk(false);
            biometricCallback.onException();
        }
    }

    private boolean isBiometricSupported() {
        int auth = biometricManager.canAuthenticate();
        return (auth == BiometricManager.BIOMETRIC_SUCCESS || auth == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED)
                && KeyguardManagerCompat.isKeyguardSecure(keyguardManager);
    }

    public boolean isFingerprintInitialized() {
        return initOk;
    }

    public boolean initEncryptData() {
        cryptoInitOk = false;

        if (!isFingerprintInitialized()) {
            if (biometricCallback != null) {
                biometricCallback.onException();
            }
            return false;
        }
        try {
            initEncryptKey(false);
            return true;
        } catch (final InvalidKeyException invalidKeyException) {
            try {
                biometricCallback.onKeyInvalidated();
                initEncryptKey(true);
            } catch (InvalidKeyException e) {
                biometricCallback.onInvalidKeyException();
            } catch (Exception e) {
                biometricCallback.onException();;
            }
        } catch (final Exception e) {
            biometricCallback.onException();
        }

        return false;
    }

    private void initEncryptKey(
            final boolean deleteExistingKey) throws Exception {

        createNewKeyIfNeeded(deleteExistingKey);
        keyStore.load(null);
        final SecretKey key = (SecretKey) keyStore.getKey(ALIAS_KEY, null);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        cryptoInitOk = true;

    }

    public void encryptData(final String value) {

        if (!isFingerprintInitialized()) {
            if (biometricCallback != null) {
                biometricCallback.onException();
            }
            return;
        }
        try {
            // actual do encryption here
            byte[] encrypted = cipher.doFinal(value.getBytes());
            final String encryptedValue = new String(Base64.encodeToString(encrypted, Base64.NO_WRAP));

            // passes updated iv spec on to callback so this can be stored for decryption
            final IvParameterSpec spec = cipher.getParameters().getParameterSpec(IvParameterSpec.class);
            final String ivSpecValue = new String(Base64.encode(spec.getIV(), Base64.NO_WRAP));
            biometricCallback.handleEncryptedResult(encryptedValue, ivSpecValue);

        } catch (final Exception e) {
            biometricCallback.onException();
        }

    }

    public Cipher getCipher() {
        return cipher;
    }

    public boolean initDecryptData(
            final String ivSpecValue) {

        cryptoInitOk = false;
        try {
            initDecryptKey(ivSpecValue,false);
            return true;
        } catch (final InvalidKeyException invalidKeyException) {
            // Key was invalidated (maybe all registered fingerprints were changed)
            // Retry with new key
            try {
                biometricCallback.onKeyInvalidated();
                initDecryptKey(ivSpecValue, true);
            } catch (InvalidKeyException e) {
                biometricCallback.onInvalidKeyException();
            } catch (Exception e) {
                biometricCallback.onException();
            }
        } catch (final Exception e) {
            biometricCallback.onException();
        }

        return false;
    }

    private void initDecryptKey(
            final String ivSpecValue,
            final boolean deleteExistingKey) throws  Exception {

        createNewKeyIfNeeded(deleteExistingKey);
        keyStore.load(null);
        final SecretKey key = (SecretKey) keyStore.getKey(ALIAS_KEY, null);

        // important to restore spec here that was used for decryption
        final byte[] iv = Base64.decode(ivSpecValue, Base64.NO_WRAP);
        final IvParameterSpec spec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        cryptoInitOk = true;

    }

    public void decryptData(final String encryptedValue) {

        if (!isFingerprintInitialized()) {
            if (biometricCallback != null) {
                biometricCallback.onException();
            }
            return;
        }
        try {
            // actual decryption here
            final byte[] encrypted = Base64.decode(encryptedValue, Base64.NO_WRAP);
            byte[] decrypted = cipher.doFinal(encrypted);
            final String decryptedString = new String(decrypted);

            //final String encryptedString = Base64.encodeToString(encrypted, 0 /* flags */);
            biometricCallback.handleDecryptedResult(decryptedString);

        } catch (BadPaddingException | IllegalBlockSizeException e) {
            biometricCallback.onKeyInvalidated();
        } catch (final Exception e) {
            biometricCallback.onException();
        }
    }

    private void createNewKeyIfNeeded(final boolean allowDeleteExisting) {
        try {
            keyStore.load(null);
            if (allowDeleteExisting
                    && keyStore.containsAlias(ALIAS_KEY)) {

                keyStore.deleteEntry(ALIAS_KEY);
            }

            // Create new key if needed
            if (!keyStore.containsAlias(ALIAS_KEY)) {
                // Set the alias of the entry in Android KeyStore where the key will appear
                // and the constrains (purposes) in the constructor of the Builder
                AlgorithmParameterSpec algSpec = KeyGenParameterSpecCompat.build(ALIAS_KEY,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT,
                        KeyProperties.BLOCK_MODE_CBC, true,
                        KeyProperties.ENCRYPTION_PADDING_PKCS7);


                keyGenerator.init(algSpec);
                keyGenerator.generateKey();
            }
        } catch (final Exception e) {
            biometricCallback.onException();
        }
    }

    void setInitOk(final boolean initOk) {
        this.initOk = initOk;
    }

}
