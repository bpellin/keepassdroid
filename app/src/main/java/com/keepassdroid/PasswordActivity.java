/*
 * Copyright 2009-2016 Brian Pellin.
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
package com.keepassdroid;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.keepass.KeePass;
import com.android.keepass.R;
import com.keepassdroid.app.App;
import com.keepassdroid.compat.BackupManagerCompat;
import com.keepassdroid.compat.ClipDataCompat;
import com.keepassdroid.compat.EditorCompat;
import com.keepassdroid.compat.StorageAF;
import com.keepassdroid.database.edit.LoadDB;
import com.keepassdroid.database.edit.OnFinish;
import com.keepassdroid.dialog.PasswordEncodingDialogHelper;
import com.keepassdroid.fileselect.BrowserDialog;
import com.keepassdroid.intents.Intents;
import com.keepassdroid.settings.AppSettingsActivity;
import com.keepassdroid.utils.EmptyUtils;
import com.keepassdroid.utils.Interaction;
import com.keepassdroid.utils.UriUtil;
import com.keepassdroid.utils.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidParameterSpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.inject.Inject;

public class PasswordActivity extends LockingActivity implements FingerprintUiHelper.Callback {
    private static final String TAG = PasswordActivity.class.getSimpleName();

    public static final String KEY_DEFAULT_FILENAME = "defaultFileName";
    private static final String KEY_FILENAME = "fileName";
    private static final String KEY_KEYFILE = "keyFile";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_LAUNCH_IMMEDIATELY = "launchImmediately";
    private static final String VIEW_INTENT = "android.intent.action.VIEW";

    private static final String FINGERPRINT_KEY_NAME = "keepass_key";
    private static final String FINGERPRINT_KEY_PASSWORD = "EncryptedPassword";
    private static final String FINGERPRINT_KEY_PASSWORD_IV = "EncryptedPasswordIV";
    private static final String FINGERPRINT_DIALOG_FRAGMENT_TAG = "fingerfrint_Fragment";
    private static final String FINGERPRINT_DB_KEY = "fingerfrint_db_key";

    private static final int FILE_BROWSE = 256;
    public static final int GET_CONTENT = 257;
    private static final int OPEN_DOC = 258;

    private Uri mDbUri = null;
    private Uri mKeyUri = null;
    private boolean mRememberKeyfile;
    SharedPreferences prefs;
    private FingerprintUiHelper mFingerprintUiHelper;

    private ImageView mFingerprintIcon;


    @Inject
    KeyguardManager mKeyguardManager;
    @Inject
    FingerprintManager mFingerprintManager;
    @Inject
    FingerprintAuthenticationDialogFragment mFragment;
    @Inject
    KeyStore mKeyStore;
    @Inject
    KeyGenerator mKeyGenerator;
    @Inject
    Cipher mEncryptCipher;
    @Inject
    Cipher mDecryptCipher;

    @Inject
    FingerprintUiHelper.FingerprintUiHelperBuilder mFingerprintUiHelperBuilder;


    public static void Launch(Activity act, String fileName) throws FileNotFoundException {
        Launch(act, fileName, "");
    }

    public static void Launch(Activity act, String fileName, String keyFile) throws FileNotFoundException {
        if (EmptyUtils.isNullOrEmpty(fileName)) {
            throw new FileNotFoundException();
        }

        Uri uri = UriUtil.parseDefaultFile(fileName);
        String scheme = uri.getScheme();

        if (!EmptyUtils.isNullOrEmpty(scheme) && scheme.equalsIgnoreCase("file")) {
            File dbFile = new File(uri.getPath());
            if (!dbFile.exists()) {
                throw new FileNotFoundException();
            }
        }

        Intent i = new Intent(act, PasswordActivity.class);
        i.putExtra(KEY_FILENAME, fileName);
        i.putExtra(KEY_KEYFILE, keyFile);

        act.startActivityForResult(i, 0);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

        case KeePass.EXIT_NORMAL:
            setEditText(R.id.password, "");
            App.getDB().clear();
            break;

        case KeePass.EXIT_LOCK:
            setResult(KeePass.EXIT_LOCK);
            setEditText(R.id.password, "");
            finish();
            App.getDB().clear();
            break;
        case FILE_BROWSE:
            if (resultCode == RESULT_OK) {
                String filename = data.getDataString();
                if (filename != null) {
                    EditText fn = (EditText) findViewById(R.id.pass_keyfile);
                    fn.setText(filename);
                    mKeyUri = UriUtil.parseDefaultFile(filename);
                }
            }
            break;
        case GET_CONTENT:
        case OPEN_DOC:
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    Uri uri = data.getData();
                    if (uri != null) {
                        if (requestCode==GET_CONTENT) {
                            uri = UriUtil.translate(this, uri);
                        }
                        String path = uri.toString();
                        if (path != null) {
                            EditText fn = (EditText) findViewById(R.id.pass_keyfile);
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ((InjectedApplication) getApplication()).inject(this);
        }

        Intent i = getIntent();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mRememberKeyfile = prefs.getBoolean(getString(R.string.keyfile_key), getResources().getBoolean(R.bool.keyfile_default));
        setContentView(R.layout.password);

        mFingerprintIcon = (ImageView) findViewById(R.id.fingerprint_icon_decrypt);

        new InitTask().execute(i);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // If the application was shutdown make sure to clear the password field, if it
        // was saved in the instance state
        if (App.isShutdown()) {
            TextView password = (TextView) findViewById(R.id.password);
            password.setText("");
        }

        // Clear the shutdown flag
        App.clearShutdown();
        if (mDbUri != null && prefs.getString(FINGERPRINT_KEY_PASSWORD, "") != "" && !prefs.getString(FINGERPRINT_DB_KEY, "").equals(mDbUri.getPath())) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(FINGERPRINT_KEY_PASSWORD, "");
            editor.commit();
        }

        if (Build.VERSION.SDK_INT >= 23) {
            mFingerprintUiHelper = mFingerprintUiHelperBuilder.build(
                    mFingerprintIcon,
                    (TextView) findViewById(R.id.fingerprint_status_decrypt), this);
        }

        if (Build.VERSION.SDK_INT >= 23 && prefs.getString(FINGERPRINT_KEY_PASSWORD, "") != "") {
            if (initDecryptCipher()) {
                mFingerprintUiHelper.stopListening();
                mFingerprintUiHelper.startListening(new FingerprintManager.CryptoObject(mDecryptCipher));
                mFingerprintIcon.setVisibility(View.VISIBLE);
            }
        } else {
            if (mFingerprintIcon != null) {
                mFingerprintIcon.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT >= 23 && prefs.getString(FINGERPRINT_KEY_PASSWORD, "") != "") {
            if (mFingerprintUiHelper != null) {
                mFingerprintUiHelper.stopListening();
                mFingerprintIcon.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void retrieveSettings() {
        String defaultFilename = prefs.getString(KEY_DEFAULT_FILENAME, "");
        if (!EmptyUtils.isNullOrEmpty(mDbUri.getPath()) && UriUtil.equalsDefaultfile(mDbUri, defaultFilename)) {
            CheckBox checkbox = (CheckBox) findViewById(R.id.default_database);
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

        String key = (mKeyUri == null) ? "" : mKeyUri.toString();
        setEditText(R.id.pass_keyfile, key);
    }

    /*
    private void errorMessage(CharSequence text)
    {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }
    */

    private void errorMessage(int resId)
    {
        Toast.makeText(this, resId, Toast.LENGTH_LONG).show();
    }

    private class DefaultCheckChange implements CompoundButton.OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                boolean isChecked) {

            String newDefaultFileName;

            if (isChecked) {
                newDefaultFileName = mDbUri.toString();
            } else {
                newDefaultFileName = "";
            }

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_DEFAULT_FILENAME, newDefaultFileName);
            EditorCompat.apply(editor);

            BackupManagerCompat backupManager = new BackupManagerCompat(PasswordActivity.this);
            backupManager.dataChanged();

        }

    }

    private class OkClickHandler implements View.OnClickListener {

        public void onClick(View view) {
            String pass = getEditText(R.id.password);
            String key = getEditText(R.id.pass_keyfile);
            loadDatabase(pass, key);
        }
    }

    private void loadDatabase(String pass, String keyfile) {
        loadDatabase(pass, UriUtil.parseDefaultFile(keyfile));
    }

    private void loadDatabase(String pass, Uri keyfile)
    {
        if ( pass.length() == 0 && (keyfile == null || keyfile.toString().length() == 0)) {
            errorMessage(R.string.error_nopass);
            return;
        }

        // Clear before we load
        Database db = App.getDB();
        db.clear();

        // Clear the shutdown flag
        App.clearShutdown();

        Handler handler = new Handler();
        LoadDB task = new LoadDB(db, PasswordActivity.this, mDbUri, pass, keyfile, new AfterLoad(handler, db));
        ProgressTask pt = new ProgressTask(PasswordActivity.this, task, R.string.loading_database);
        pt.run();
    }

    private String getEditText(int resId) {
        return Util.getEditText(this, resId);
    }

    private void setEditText(int resId, String str) {
        TextView te =  (TextView) findViewById(resId);
        assert(te == null);

        if (te != null) {
            te.setText(str);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflate = getMenuInflater();
        inflate.inflate(R.menu.password, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch ( item.getItemId() ) {
        case R.id.menu_about:
            AboutDialog dialog = new AboutDialog(this);
            dialog.show();
            return true;

        case R.id.menu_app_settings:
            AppSettingsActivity.Launch(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private final class AfterLoad extends OnFinish {
        private Database db;

        public AfterLoad(Handler handler, Database db) {
            super(handler);

            this.db = db;
        }

        @TargetApi(Build.VERSION_CODES.M)
        @Override
        public void run() {
            if (db.passwordEncodingError) {
                PasswordEncodingDialogHelper dialog = new PasswordEncodingDialogHelper();
                dialog.show(PasswordActivity.this, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        GroupActivity.Launch(PasswordActivity.this);
                    }

                });
            } else if (mSuccess) {
                if (Build.VERSION.SDK_INT >= 23 && prefs.getString(FINGERPRINT_KEY_PASSWORD, "") == "" && mFingerprintUiHelper.isFingerprintAuthAvailable() && initEncryptCipher()) {
                    mFragment.setEncryptCipher(mEncryptCipher);
                    mFragment.show(getFragmentManager(), FINGERPRINT_DIALOG_FRAGMENT_TAG);
                } else {
                    GroupActivity.Launch(PasswordActivity.this);
                }
            } else {
                displayMessage(PasswordActivity.this);
            }

            if (mFingerprintIcon != null && prefs.getString(FINGERPRINT_KEY_PASSWORD, "") != "") {
                mFingerprintIcon.setImageResource(R.drawable.ic_fp_40px);
                if (mSuccess == false) {
                    mFingerprintIcon.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    private class InitTask extends AsyncTask<Intent, Void, Integer> {
        String password = "";
        boolean launch_immediately = false;

        @Override
        protected Integer doInBackground(Intent... args) {
            Intent i = args[0];
            String action = i.getAction();;
            if ( action != null && action.equals(VIEW_INTENT) ) {
                Uri incoming = i.getData();
                mDbUri = incoming;

				mKeyUri = ClipDataCompat.getUriFromIntent(i, KEY_KEYFILE);

                if (incoming == null) {
                    return R.string.error_can_not_handle_uri;
                }
                else if (incoming.getScheme().equals("file")) {
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

					if(mKeyUri == null)
						mKeyUri = getKeyFile(mDbUri);
                }
                else if (incoming.getScheme().equals("content")) {
					if(mKeyUri == null)
						mKeyUri = getKeyFile(mDbUri);
                }
                else {
                    return R.string.error_can_not_handle_uri;
                }
                password = i.getStringExtra(KEY_PASSWORD);
                launch_immediately = i.getBooleanExtra(KEY_LAUNCH_IMMEDIATELY, false);

            } else {
                mDbUri = UriUtil.parseDefaultFile(i.getStringExtra(KEY_FILENAME));
                mKeyUri = UriUtil.parseDefaultFile(i.getStringExtra(KEY_KEYFILE));
                password = i.getStringExtra(KEY_PASSWORD);
                launch_immediately = i.getBooleanExtra(KEY_LAUNCH_IMMEDIATELY, false);

                if (mKeyUri == null || mKeyUri.toString().length() == 0) {
                    mKeyUri = getKeyFile(mDbUri);
                }
            }
            return null;
        }

        public void onPostExecute(Integer result) {
            if(result != null) {
                Toast.makeText(PasswordActivity.this, result, Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            populateView();

            Button confirmButton = (Button) findViewById(R.id.pass_ok);
            confirmButton.setOnClickListener(new OkClickHandler());

            CheckBox checkBox = (CheckBox) findViewById(R.id.show_password);
            // Show or hide password
            checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                public void onCheckedChanged(CompoundButton buttonView,
                        boolean isChecked) {
                    TextView password = (TextView) findViewById(R.id.password);

                    if (isChecked) {
                        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    } else {
                        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    }
                }

            });

            if (password != null) {
                TextView tv_password = (TextView) findViewById(R.id.password);
                tv_password.setText(password);
            }

            CheckBox defaultCheck = (CheckBox) findViewById(R.id.default_database);
            defaultCheck.setOnCheckedChangeListener(new DefaultCheckChange());

            ImageButton browse = (ImageButton) findViewById(R.id.browse_button);
            browse.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                    if (StorageAF.useStorageFramework(PasswordActivity.this)) {
                        Intent i = new Intent(StorageAF.ACTION_OPEN_DOCUMENT);
                        i.addCategory(Intent.CATEGORY_OPENABLE);
                        i.setType("*/*");
                        startActivityForResult(i, OPEN_DOC);
                    } else {
                        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                        i.addCategory(Intent.CATEGORY_OPENABLE);
                        i.setType("*/*");

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                            i.addCategory(Intent.CATEGORY_OPENABLE);
                            i.setType("*/*");
                        }

                        try {
                            startActivityForResult(i, GET_CONTENT);
                        } catch (ActivityNotFoundException e) {
                            lookForOpenIntentsFilePicker();
                        }
                    }
                }

                private void lookForOpenIntentsFilePicker() {
                    if (Interaction.isIntentAvailable(PasswordActivity.this, Intents.OPEN_INTENTS_FILE_BROWSE)) {
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
                    BrowserDialog diag = new BrowserDialog(PasswordActivity.this);
                    diag.show();
                }
            });

            retrieveSettings();

            if (launch_immediately)
                loadDatabase(password, mKeyUri);
        }
    }

    private boolean initEncryptCipher() {

        mEncryptCipher = getCipher(Cipher.ENCRYPT_MODE);
        if (mEncryptCipher == null) {
            // try again after recreating the keystore
            createKey();
            mEncryptCipher = getCipher(Cipher.ENCRYPT_MODE);
        }
        return (mEncryptCipher != null);

    }

    public boolean initDecryptCipher() {
        mDecryptCipher = getCipher(Cipher.DECRYPT_MODE);
        return (mDecryptCipher != null);
    }

    /**
     * Tries to encrypt some data with the generated key in {@link #createKey} which is
     * only works if the user has just authenticated via fingerprint.
     */
    @TargetApi(23)
    public boolean tryEncrypt() {
        try {
            String secret = getEditText(R.id.password);
            byte[] encrypted = mEncryptCipher.doFinal(secret.getBytes());

            IvParameterSpec ivParams = mEncryptCipher.getParameters().getParameterSpec(IvParameterSpec.class);
            String iv = Base64.encodeToString(ivParams.getIV(), Base64.DEFAULT);

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(FINGERPRINT_KEY_PASSWORD, Base64.encodeToString(encrypted, Base64.DEFAULT));
            editor.putString(FINGERPRINT_KEY_PASSWORD_IV, iv);
            editor.putString(FINGERPRINT_DB_KEY, mDbUri.getPath());
            editor.commit();
            GroupActivity.Launch(PasswordActivity.this);
            return true;


        } catch (BadPaddingException | IllegalBlockSizeException e) {
            Toast.makeText(this, "Failed to encrypt the data with the generated key. "
                    + "Retry the purchase", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Failed to encrypt the data with the generated key." + e.getMessage());
        } catch (InvalidParameterSpecException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Tries to decrypt some data with the generated key in {@link #createKey} which is
     * only works if the user has just authenticated via fingerprint.
     */
    @TargetApi(23)
    public String tryDecrypt() {
        try {

            byte[] encodedData = Base64.decode(prefs.getString(FINGERPRINT_KEY_PASSWORD, ""), Base64.DEFAULT);
            byte[] decodedData = mDecryptCipher.doFinal(encodedData);
            return new String(decodedData);

        } catch (BadPaddingException | IllegalBlockSizeException e) {
            Toast.makeText(this, "Failed to decrypt the data with the generated key. "
                    + "Retry the purchase", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Failed to decrypt the data with the generated key." + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }


    private SecretKey getKey() {
        try {
            mKeyStore.load(null);
            SecretKey key = (SecretKey) mKeyStore.getKey(FINGERPRINT_KEY_NAME, null);
            if (key != null) return key;
            return createKey();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        return null;
    }

    @TargetApi(23)
    private SecretKey createKey() {
        try {

            // Set the alias of the entry in Android KeyStore where the key will appear
            // and the constrains (purposes) in the constructor of the Builder
            mKeyGenerator.init(new KeyGenParameterSpec.Builder(FINGERPRINT_KEY_NAME,
                    KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_ENCRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    // Require the user to authenticate with a fingerprint to authorize every use
                    // of the key
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            return mKeyGenerator.generateKey();

        } catch (Exception e) {

        }
        return null;
    }

    @TargetApi(23)
    public Cipher getCipher(int mode) {
        Cipher cipher;

        try {
            mKeyStore.load(null);
            byte[] iv;
            cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            IvParameterSpec ivParams;
            if (mode == Cipher.ENCRYPT_MODE) {
                cipher.init(mode, getKey());

            } else {
                SecretKey key = (SecretKey) mKeyStore.getKey(FINGERPRINT_KEY_NAME, null);
                iv = Base64.decode(prefs.getString(FINGERPRINT_KEY_PASSWORD_IV, ""), Base64.DEFAULT);
                ivParams = new IvParameterSpec(iv);
                cipher.init(mode, key, ivParams);
            }
            return cipher;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onAuthenticated(Cipher cipher) {
        String pass = tryDecrypt();
        String key = getEditText(R.id.pass_keyfile);
        loadDatabase(pass, key);
    }

    @Override
    public void onError() {

    }
}
