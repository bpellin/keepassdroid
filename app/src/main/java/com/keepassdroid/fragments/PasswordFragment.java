/*
 * Copyright 2020-2022 Brian Pellin
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
package com.keepassdroid.fragments;

import android.app.Activity;
import android.app.backup.BackupManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.android.keepass.KeePass;
import com.android.keepass.R;
import com.keepassdroid.AboutDialog;
import com.keepassdroid.Database;
import com.keepassdroid.GroupActivity;
import com.keepassdroid.PasswordActivity;
import com.keepassdroid.ProgressTask;
import com.keepassdroid.app.App;
import com.keepassdroid.compat.ClipDataCompat;
import com.keepassdroid.compat.StorageAF;
import com.keepassdroid.database.edit.LoadDB;
import com.keepassdroid.database.edit.OnFinish;
import com.keepassdroid.dialog.PasswordEncodingDialogHelper;
import com.keepassdroid.fileselect.BrowserDialog;
import com.keepassdroid.biometric.BiometricHelper;
import com.keepassdroid.intents.Intents;
import com.keepassdroid.settings.AppSettingsActivity;
import com.keepassdroid.utils.EmptyUtils;
import com.keepassdroid.utils.Interaction;
import com.keepassdroid.utils.PermissionUtil;
import com.keepassdroid.utils.UriUtil;
import com.keepassdroid.utils.Util;

import java.io.File;
import java.util.concurrent.Executor;

import javax.crypto.Cipher;

public class PasswordFragment extends Fragment implements BiometricHelper.BiometricCallback {
    private static final int FILE_BROWSE = 256;
    public static final int GET_CONTENT = 257;
    private static final int OPEN_DOC = 258;

    private static final String KEY_PASSWORD = "password";
    private static final String KEY_LAUNCH_IMMEDIATELY = "launchImmediately";
    private static final String VIEW_INTENT = "android.intent.action.VIEW";

    private static final int PERMISSION_REQUEST_ID = 1;
    private static final int BIOMETRIC_SAVE = 1;
    private static final int BIOMETRIC_LOAD = 2;

    private Uri mDbUri = null;
    private Uri mKeyUri = null;
    private boolean mRememberKeyfile;
    SharedPreferences prefs;
    SharedPreferences prefsNoBackup;

    private Uri storedKeyUri = null;
    private String storedPassword = null;
    private int mode;
    private static final String PREF_KEY_VALUE_PREFIX = "valueFor_"; // key is a combination of db file name and this prefix
    private static final String PREF_KEY_IV_PREFIX = "ivFor_"; // key is a combination of db file name and this prefix
    private View mView;

    private CheckBox biometricCheck;
    private EditText passwordView;
    private Button biometricOpen;
    private Button biometricClear;
    private View divider3;
    private Button confirmButton;
    private boolean biometricsAvailable = false;
    private BiometricPrompt biometricSavePrompt;
    private BiometricPrompt biometricOpenPrompt;
    private BiometricPrompt.PromptInfo savePrompt;
    private BiometricPrompt.PromptInfo loadPrompt;
    private BiometricHelper biometricHelper;
    private int biometricMode = 0;

    private AppCompatActivity mActivity;

    private boolean afterOnCreateBeforeEndOfOnResume = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        setRetainInstance(true);
        afterOnCreateBeforeEndOfOnResume = true;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mView = inflater.inflate(R.layout.password, container, false);
        return mView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Context context = getContext();

        prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
        prefsNoBackup = mActivity.getSharedPreferences("nobackup", Context.MODE_PRIVATE);

        mRememberKeyfile = prefs.getBoolean(getString(R.string.keyfile_key), getResources().getBoolean(R.bool.keyfile_default));
        confirmButton = (Button) view.findViewById(R.id.pass_ok);
        passwordView = (EditText) view.findViewById(R.id.password);
        biometricOpen = (Button) view.findViewById(R.id.open_biometric);
        biometricClear = (Button) view.findViewById(R.id.clear_biometric);
        divider3 = view.findViewById(R.id.divider3);
        biometricCheck = (CheckBox) view.findViewById(R.id.save_password);

        biometricOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                biometricLogin();
            }
        });

        biometricClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearStoredCredentials();
            }
        });
    }

    private void biometricLogin() {
        if (!initDecryptData()) {
            return;
        }

        biometricCheck.setChecked(false);

        Cipher cipher = biometricHelper.getCipher();
        biometricMode = BIOMETRIC_LOAD;
        biometricOpenPrompt.authenticate(loadPrompt, new BiometricPrompt.CryptoObject(cipher));
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        MenuInflater inflate = mActivity.getMenuInflater();
        inflate.inflate(R.menu.password, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Context context = getContext();
        switch (item.getItemId()) {
            case R.id.menu_about:
                AboutDialog dialog = new AboutDialog(context);
                dialog.show();
                return true;

            case R.id.menu_app_settings:
                AppSettingsActivity.Launch(context);
                return true;
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof AppCompatActivity) {
            mActivity = (AppCompatActivity) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mActivity = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mActivity = null;
    }

    private void setFingerPrintVisibilty() {
        if (biometricsAvailable) {
            biometricCheck.setVisibility(View.VISIBLE);
        } else {
            biometricCheck.setVisibility(View.GONE);
        }

        biometricOpenUpdateVisibility();
    }

    private void biometricOpenUpdateVisibility() {
        int visibility;
        boolean autoOpen = false;
        BiometricManager biometricManager = BiometricManager.from(mActivity);
        int auth = biometricManager.canAuthenticate();
        if (biometricsAvailable && auth != BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            String encryptedValue = prefsNoBackup.getString(getPreferenceKeyValue(), null);
            String ivSpecValue = prefsNoBackup.getString(getPreferenceKeyIvSpec(), null);

            boolean hasStoredKey = encryptedValue != null && ivSpecValue != null;
            if (hasStoredKey) {
                // Check key value
                visibility = View.VISIBLE;
                autoOpen = prefs.getBoolean(getString(R.string.biometric_autoscan_key),
                        getResources().getBoolean(R.bool.biometric_autoscan))
                        && afterOnCreateBeforeEndOfOnResume;
            } else {
                visibility = View.GONE;
            }

        } else {
            visibility = View.GONE;
        }

        biometricOpen.setVisibility(visibility);
        biometricClear.setVisibility(visibility);
        divider3.setVisibility(visibility);

        if (autoOpen) {
            biometricLogin();
        }
    }

    private void initBiometrics() {
        final Context context = getContext();
        biometricsAvailable = true;

        biometricHelper = new BiometricHelper(context, this);

        Executor executor = ContextCompat.getMainExecutor(context);

        BiometricPrompt.AuthenticationCallback biometricCallback =
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();

                        if (biometricMode == BIOMETRIC_SAVE) {
                            Toast.makeText(context, R.string.biometric_auth_failed_store, Toast.LENGTH_LONG).show();
                            GroupActivity.Launch(mActivity);
                        } else if (biometricMode == BIOMETRIC_LOAD) {
                            Toast.makeText(context, R.string.biometric_auth_failed, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);

                        if (biometricMode == BIOMETRIC_SAVE) {
                            // newly store the entered password in encrypted way
                            final String password = passwordView.getText().toString();

                            biometricHelper.encryptData(password);
                            GroupActivity.Launch(mActivity);
                            passwordView.setText("");
                        } else if (biometricMode == BIOMETRIC_LOAD) {
                            // retrieve the encrypted value from preferences
                            final String encryptedValue = prefsNoBackup.getString(getPreferenceKeyValue(), null);
                            if (encryptedValue != null) {
                                biometricHelper.decryptData(encryptedValue);
                            }
                        }

                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);

                        if (!canceledBiometricAuth(errorCode)) {
                            Toast.makeText(context, R.string.biometric_auth_error, Toast.LENGTH_LONG).show();
                        }

                        if (biometricMode == BIOMETRIC_SAVE) {
                            GroupActivity.Launch(mActivity);
                        }
                    }
                };

        biometricSavePrompt = new BiometricPrompt(this, executor, biometricCallback);
        BiometricPrompt.PromptInfo.Builder saveBuilder = new BiometricPrompt.PromptInfo.Builder();
        savePrompt = saveBuilder.setDescription(getString(R.string.biometric_auth_to_store))
                .setConfirmationRequired(false)
                .setTitle(getString(R.string.biometric_save_password))
                .setNegativeButtonText(getString(android.R.string.cancel))
                .build();

        biometricOpenPrompt = new BiometricPrompt(this, executor, biometricCallback);
        BiometricPrompt.PromptInfo.Builder openBuilder = new BiometricPrompt.PromptInfo.Builder();
        loadPrompt = openBuilder.setDescription(getString(R.string.biometric_auth_to_open))
                .setConfirmationRequired(false)
                .setTitle(getString(R.string.biometric_open_db))
                .setNegativeButtonText(getString(android.R.string.cancel))
                .build();

        setFingerPrintVisibilty();
    }

    private boolean canceledBiometricAuth(int errorCode) {
        switch (errorCode) {
            case BiometricPrompt.ERROR_CANCELED:
            case BiometricPrompt.ERROR_USER_CANCELED:
            case BiometricPrompt.ERROR_NEGATIVE_BUTTON:
                return true;
            default:
                return false;
        }
    }


    private boolean initDecryptData() {
        final String ivSpecValue = prefsNoBackup.getString(getPreferenceKeyIvSpec(), null);

        return biometricHelper.initDecryptData(ivSpecValue);
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Intent i = mActivity.getIntent();
        InitTask task = new InitTask();
        task.onPostExecute(task.doInBackground(i));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Activity activity = mActivity;

        switch (requestCode) {

            case KeePass.EXIT_NORMAL:
                setEditText(R.id.password, "");
                App.getDB().clear(activity.getApplicationContext());
                break;

            case KeePass.EXIT_LOCK:
                activity.setResult(KeePass.EXIT_LOCK);
                setEditText(R.id.password, "");
                activity.finish();
                App.getDB().clear(activity.getApplicationContext());
                break;
            case FILE_BROWSE:
                if (resultCode == Activity.RESULT_OK) {
                    String filename = data.getDataString();
                    if (filename != null) {
                        EditText fn = (EditText) mView.findViewById(R.id.pass_keyfile);
                        fn.setText(filename);
                        mKeyUri = UriUtil.parseDefaultFile(filename);
                    }
                }
                break;
            case GET_CONTENT:
            case OPEN_DOC:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        Uri uri = data.getData();
                        if (uri != null) {
                            if (requestCode == GET_CONTENT) {
                                uri = UriUtil.translate(activity, uri);
                            }
                            String path = uri.toString();
                            if (path != null) {
                                EditText fn = (EditText) mView.findViewById(R.id.pass_keyfile);
                                fn.setText(path);

                            }
                            mKeyUri = uri;
                        }
                    }
                }
                break;
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        // If the application was shutdown make sure to clear the password field, if it
        // was saved in the instance state
        if (App.isShutdown()) {
            TextView password = (TextView) mView.findViewById(R.id.password);
            password.setText("");
        }

        // Clear the shutdown flag
        App.clearShutdown();

        BiometricManager biometricManager = BiometricManager.from(mActivity);
        int auth = biometricManager.canAuthenticate();
        if (auth == BiometricManager.BIOMETRIC_SUCCESS){
            initBiometrics();
        } else {
            biometricsAvailable = false;
            setFingerPrintVisibilty();
        }

        afterOnCreateBeforeEndOfOnResume = false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_ID &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED){

            loadDatabaseWithPermission();
        } else {
            errorMessage(R.string.no_external_permissions);
        }
    }

    private String getPreferenceKeyValue() {
        // makes it possible to store passwords uniqly per database
        return PREF_KEY_VALUE_PREFIX + (mDbUri != null ? mDbUri.getPath() : "");
    }

    private String getPreferenceKeyIvSpec() {
        return PREF_KEY_IV_PREFIX + (mDbUri != null ? mDbUri.getPath() : "");
    }

    // Moved this to the foreground TOOD: Move this to a more typical pattern
    private class InitTask {

        String password = "";
        boolean launch_immediately = false;

        public Integer doInBackground(Intent... args) {
            Intent i = args[0];
            String action = i.getAction();
            ;
            if (action != null && action.equals(VIEW_INTENT)) {
                Uri incoming = i.getData();
                mDbUri = incoming;

                mKeyUri = ClipDataCompat.getUriFromIntent(i, PasswordActivity.KEY_KEYFILE);

                if (incoming == null) {
                    return R.string.error_can_not_handle_uri;
                } else if (incoming.getScheme().equals("file")) {
                    String fileName = incoming.getPath();

                    if (fileName.length() == 0) {
                        // No file name
                        return R.string.FileNotFound;
                    }

                    File dbFile = new File(fileName);
                    if (!dbFile.exists()) {
                        // File does not exist
                        return R.string.FileNotFound;
                    }

                    if (mKeyUri == null) {
                        mKeyUri = getKeyFile(mDbUri);
                    }
                } else if (incoming.getScheme().equals("content")) {
                    if (mKeyUri == null) {
                        mKeyUri = getKeyFile(mDbUri);
                    }
                } else {
                    return R.string.error_can_not_handle_uri;
                }
                password = i.getStringExtra(KEY_PASSWORD);
                launch_immediately = i.getBooleanExtra(KEY_LAUNCH_IMMEDIATELY, false);

            } else {
                mDbUri = UriUtil.parseDefaultFile(i.getStringExtra(PasswordActivity.KEY_FILENAME));
                mKeyUri = UriUtil.parseDefaultFile(i.getStringExtra(PasswordActivity.KEY_KEYFILE));
                password = i.getStringExtra(KEY_PASSWORD);
                launch_immediately = i.getBooleanExtra(KEY_LAUNCH_IMMEDIATELY, false);

                if (mKeyUri == null || mKeyUri.toString().length() == 0) {
                    mKeyUri = getKeyFile(mDbUri);
                }
            }

            biometricOpenUpdateVisibility();

            return null;
        }

        public void onPostExecute(Integer result) {
            if (result != null) {
                Toast.makeText(mActivity, result, Toast.LENGTH_LONG).show();
                mActivity.finish();
                return;
            }

            populateView();

            confirmButton.setOnClickListener(new OkClickHandler());

            CheckBox checkBox = (CheckBox) mView.findViewById(R.id.show_password);
            // Show or hide password
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                public void onCheckedChanged(
                        CompoundButton buttonView,
                        boolean isChecked) {
                    TextView password = (TextView) mView.findViewById(R.id.password);

                    if (isChecked) {
                        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    } else {
                        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    }
                }

            });

            if (password != null) {
                TextView tv_password = (TextView) mView.findViewById(R.id.password);
                tv_password.setText(password);
            }

            CheckBox defaultCheck = (CheckBox) mView.findViewById(R.id.default_database);
            defaultCheck.setOnCheckedChangeListener(new DefaultCheckChange());

            ImageButton browse = (ImageButton) mView.findViewById(R.id.browse_button);
            browse.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                    if (StorageAF.useStorageFramework(mActivity)) {
                        Intent i = new Intent(StorageAF.ACTION_OPEN_DOCUMENT);
                        i.addCategory(Intent.CATEGORY_OPENABLE);
                        i.setType("*/*");
                        startActivityForResult(i, OPEN_DOC);
                    } else {
                        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                        i.addCategory(Intent.CATEGORY_OPENABLE);
                        i.setType("*/*");

                        try {
                            startActivityForResult(i, GET_CONTENT);
                        } catch (ActivityNotFoundException e) {
                            lookForOpenIntentsFilePicker();
                        }
                    }
                }

                private void lookForOpenIntentsFilePicker() {
                    if (Interaction.isIntentAvailable(mActivity, Intents.OPEN_INTENTS_FILE_BROWSE)) {
                        Intent i = new Intent(Intents.OPEN_INTENTS_FILE_BROWSE);

                        // Get file path parent if possible
                        try {
                            if (mDbUri != null && mDbUri.toString().length() > 0) {
                                if (mDbUri.getScheme().equals("file")) {
                                    File keyfile = new File(mDbUri.getPath());
                                    File parent = keyfile.getParentFile();
                                    if (parent != null) {
                                        i.setData(Uri.parse("file://" + parent.getAbsolutePath()));
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Ignore
                        }

                        try {
                            startActivityForResult(i, FILE_BROWSE);
                        } catch (ActivityNotFoundException e) {
                            showBrowserDialog();
                        }
                    } else {
                        showBrowserDialog();
                    }
                }

                private void showBrowserDialog() {
                    BrowserDialog diag = new BrowserDialog(mActivity);
                    diag.show();
                }
            });

            retrieveSettings();

            if (launch_immediately) {
                loadDatabase(password, mKeyUri);
            }
        }
    }

    private class DefaultCheckChange implements CompoundButton.OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(
                CompoundButton buttonView,
                boolean isChecked) {

            String newDefaultFileName;

            if (isChecked) {
                newDefaultFileName = mDbUri.toString();
            } else {
                newDefaultFileName = "";
            }

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PasswordActivity.KEY_DEFAULT_FILENAME, newDefaultFileName);
            editor.apply();

            BackupManager backupManager = new BackupManager(getContext());
            backupManager.dataChanged();

        }

    }

    private void retrieveSettings() {
        String defaultFilename = prefs.getString(PasswordActivity.KEY_DEFAULT_FILENAME, "");
        if (!EmptyUtils.isNullOrEmpty(mDbUri.getPath()) && UriUtil.equalsDefaultfile(mDbUri, defaultFilename)) {
            CheckBox checkbox = (CheckBox) mView.findViewById(R.id.default_database);
            checkbox.setChecked(true);
        }
    }

    private Uri getKeyFile(Uri dbUri) {
        if (mRememberKeyfile) {

            return App.getFileHistory().getFileByName(dbUri);
        } else {
            return null;
        }
    }

    private void populateView() {
        String db = (mDbUri == null) ? "" : mDbUri.toString();
        setEditText(R.id.filename, db);

        String displayName = UriUtil.getFileName(mDbUri, getContext());
        TextView displayNameView = mView.findViewById(R.id.filename_display);
        if (displayNameView != null) {
            if (EmptyUtils.isNullOrEmpty(displayName)) {
                displayNameView.setVisibility(View.GONE);
            } else {
                displayNameView.setText(UriUtil.getFileName(mDbUri, getContext()));
            }
        }

        String key = (mKeyUri == null) ? "" : mKeyUri.toString();
        setEditText(R.id.pass_keyfile, key);
    }

    private void errorMessage(int resId) {
        Toast.makeText(mActivity, resId, Toast.LENGTH_LONG).show();
    }

    private void setEditText(
            int resId,
            String str) {

        TextView te = (TextView) mView.findViewById(resId);
        assert (te != null);

        if (te != null) {
            te.setText(str);
        }
    }
    private void loadDatabase(
            String pass,
            Uri keyfile) {
        if (pass.length() == 0 && (keyfile == null || keyfile.toString().length() == 0)) {
            errorMessage(R.string.error_nopass);
            return;
        }

        storedPassword = pass;
        storedKeyUri = keyfile;

        if (checkFilePermissions(mDbUri, keyfile)) {
            loadDatabaseWithPermission();
        }
    }

    private void loadDatabaseWithPermission() {
        String pass = storedPassword;
        storedPassword = null;
        Uri keyfile = storedKeyUri;
        storedKeyUri = null;
        Activity activity = mActivity;


        // Clear before we load
        Database db = App.getDB();
        db.clear(activity.getApplicationContext());

        // Clear the shutdown flag
        App.clearShutdown();

        Handler handler = new Handler();
        LoadDB task = new LoadDB(db, activity, mDbUri, pass, keyfile, new PasswordFragment.AfterLoad(handler, db));
        ProgressTask pt = new ProgressTask(activity, task, R.string.loading_database);
        pt.run();
    }

    private String getEditText(int resId) {
        return Util.getEditText(mActivity, resId);
    }
    private final class AfterLoad extends OnFinish {

        private Database db;

        public AfterLoad(
                Handler handler,
                Database db) {
            super(handler);

            this.db = db;
        }

        @Override
        public void run() {
            if (db.passwordEncodingError) {
                PasswordEncodingDialogHelper dialog = new PasswordEncodingDialogHelper();
                dialog.show(mActivity, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(
                            DialogInterface dialog,
                            int which) {
                        GroupActivity.Launch(mActivity);
                    }

                });
            } else if (mSuccess) {
                if (biometricCheck.isChecked()) {
                    if (!biometricHelper.initEncryptData()) {
                        return;
                    }
                    Cipher cipher = biometricHelper.getCipher();

                    biometricMode = BIOMETRIC_SAVE;
                    biometricSavePrompt.authenticate(savePrompt, new BiometricPrompt.CryptoObject(cipher));

                }
                else {
                    passwordView.setText("");

                    // Check to see if the fragement detached before this finished
                    if (mActivity != null) {
                        GroupActivity.Launch(mActivity);
                    }
                }
            } else {
                displayMessage(mActivity);
            }
        }
    }

    private class OkClickHandler implements View.OnClickListener {

        public void onClick(View view) {
            String pass = getEditText(R.id.password);
            String key = getEditText(R.id.pass_keyfile);

            loadDatabase(pass, key);
        }
    }

    private void loadDatabase(
            String pass,
            String keyfile) {

        loadDatabase(pass, UriUtil.parseDefaultFile(keyfile));
    }

    private boolean hasFileUri(Uri uri) {
        try {
            if (uri == null) { return false; }

            return uri.getScheme().equalsIgnoreCase("file");
        } catch (Exception e) {
            return false;
        }

    }

    private void clearStoredCredentials() {
        prefsNoBackup.edit()
                .remove(getPreferenceKeyValue())
                .remove(getPreferenceKeyIvSpec())
                .commit();
        setFingerPrintVisibilty();
    }

    @Override
    public void handleEncryptedResult(String value, String ivSpec) {
        prefsNoBackup.edit()
                .putString(getPreferenceKeyValue(), value)
                .putString(getPreferenceKeyIvSpec(), ivSpec)
                .commit();
        // and remove visual input to reset UI
        Toast.makeText(getContext(), R.string.encrypted_value_stored, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void handleDecryptedResult(String value) {
        // on decrypt enter it for the purchase/login action
        passwordView.setText(value);
        confirmButton.performClick();
    }

    @Override
    public void onInvalidKeyException() {
        Toast.makeText(getContext(), R.string.fingerprint_invalid_key, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onException() {
        onException(true);
    }

    @Override
    public void onException(boolean showWarningMessage) {
        if (showWarningMessage) {
            onException(R.string.biometric_error);
        }
    }

    @Override
    public void onException(CharSequence message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onException(int resId) {
        Toast.makeText(getContext(), resId, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onKeyInvalidated() {
        clearStoredCredentials();
        Toast.makeText(getContext(), R.string.biometric_invalidated, Toast.LENGTH_LONG).show();
    }

    private boolean checkFilePermissions(Uri db, Uri keyfile) {
        boolean hasFileUri = hasFileUri(db) ||
                hasFileUri(keyfile);

        if (!hasFileUri) return true;

        return PermissionUtil.checkAndRequest(this.mActivity, PERMISSION_REQUEST_ID);
    }
}
