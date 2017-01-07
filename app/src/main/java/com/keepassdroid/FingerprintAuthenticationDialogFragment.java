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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.keepass.R;

import javax.crypto.Cipher;
import javax.inject.Inject;

/**
 * A dialog which uses fingerprint APIs to authenticate the user, and falls back to password
 * authentication if fingerprint is not available.
 */
@TargetApi(23)
public class FingerprintAuthenticationDialogFragment extends DialogFragment
        implements FingerprintUiHelper.Callback {

    private View mView;
    private Button mCancelButton;
    private View mFingerprintEncrypt;

    private Cipher mEncryptCipher;
    private Cipher mDecryptCipher;
    private FingerprintUiHelper mFingerprintUiHelper;
    private PasswordActivity mActivity;

    @Inject FingerprintUiHelper.FingerprintUiHelperBuilder mFingerprintUiHelperBuilder;
    @Inject InputMethodManager mInputMethodManager;
    @Inject SharedPreferences mSharedPreferences;

    @Inject
    public FingerprintAuthenticationDialogFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Do not create a new Fragment when the Activity is re-created such as orientation changes.
        setRetainInstance(true);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        getDialog().setTitle(getString(R.string.sign_in));
        View v = mView = inflater.inflate(R.layout.fingerprint_dialog_container, container, false);
        mCancelButton = (Button) mView.findViewById(R.id.cancel_button);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        mFingerprintEncrypt = v.findViewById(R.id.fingerprint_encrypt_container);

        mFingerprintUiHelper = mFingerprintUiHelperBuilder.build(
                (ImageView) mView.findViewById(R.id.fingerprint_icon),
                (TextView) mView.findViewById(R.id.fingerprint_status), this);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        mFingerprintUiHelper.stopListening();
        FingerprintManagerCompat test;

        mFingerprintUiHelper.startListening(new FingerprintManagerCompat.CryptoObject(mEncryptCipher));
    }

    @Override
    public void onPause() {
        super.onPause();
        mFingerprintUiHelper.stopListening();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (PasswordActivity) activity;
    }

    /**
     * Sets the crypto object to be passed in when authenticating with fingerprint.
     */
    public void setDecryptCipher(Cipher cipher) {
        mDecryptCipher = cipher;
    }

    /**
     * Sets the crypto object to be passed in when authenticating with fingerprint.
     */
    public void setEncryptCipher(Cipher cipher) {
        mEncryptCipher = cipher;
    }

    @Override
    public void onAuthenticated(Cipher cipher) {
        mActivity.tryEncrypt();
        dismiss();
    }

    @Override
    public void onError() {

    }
}
