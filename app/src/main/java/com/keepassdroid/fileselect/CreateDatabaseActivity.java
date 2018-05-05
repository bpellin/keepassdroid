package com.keepassdroid.fileselect;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.android.keepass.R;
import com.keepassdroid.ProgressTask;
import com.keepassdroid.compat.StorageAF;
import com.keepassdroid.database.edit.CreateDB;
import com.keepassdroid.database.edit.OnFinish;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import static android.os.Build.VERSION_CODES.LOLLIPOP;

@TargetApi(LOLLIPOP)
public class CreateDatabaseActivity extends AppCompatActivity {

    private static final String TAG = CreateDatabaseActivity.class.getSimpleName();

    private static final int RESULT_CHOOSE_FOLDER = 1;

    private AppCompatEditText path;

    private AppCompatEditText fileName;

    private AppCompatEditText password;

    private AppCompatEditText passwordConfirm;

    private AppCompatEditText keyFile;

    private Path selectedPath = new Path("", "");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.create_dialog);

        path = (AppCompatEditText) findViewById(R.id.path);
        fileName = (AppCompatEditText) findViewById(R.id.filename);
        password = (AppCompatEditText) findViewById(R.id.password);
        passwordConfirm = (AppCompatEditText) findViewById(R.id.password_confirm);
        keyFile = (AppCompatEditText) findViewById(R.id.key_file);

        // Handle clicks on path-input
        path.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (StorageAF.useStorageFramework(CreateDatabaseActivity.this)) {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    startActivityForResult(i, RESULT_CHOOSE_FOLDER);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.create_database, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_save:
                return saveDatabase();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RESULT_CHOOSE_FOLDER:
                if (resultCode == RESULT_OK) {
                    try {
                        selectedPath = Path.parseActivityResult(
                                convertContentToFilePath(
                                        Uri.parse(URLDecoder.decode(data.getDataString(), "UTF-8"))
                                ));
                    } catch (UnsupportedEncodingException e) {
                        Log.e(TAG, "onActivityResult: could not parse string to uri", e);
                    }
                    path.setText(selectedPath.displayName);
                }
                return;
            default:
                return;
        }
    }

    private String convertContentToFilePath(Uri contentPath) {
        String filePath = Environment.getExternalStorageDirectory() + "/" + contentPath.getPathSegments().get(1).split(":")[1];
        Log.d("","URI = " + contentPath);
        Log.d("","Chosen path = " + filePath);
        return filePath;
    }

    private boolean checkFields(Field... fields) {
        for (Field field : fields) {
            if (field.fieldContent.isEmpty()) {
                showMessage(field.messageId);
                return false;
            }
        }
        return true;
    }

    private boolean saveDatabase() {
        String sFileName = this.fileName.getText().toString();
        String sPassword = this.password.getText().toString();
        String sPasswordConfirm = this.passwordConfirm.getText().toString();
        String sKeyFilePath = this.keyFile.getText().toString();

        if (!checkFields(
                new Field(selectedPath.path, R.string.error_path_required),
                new Field(sFileName, R.string.error_filename_required)
            )) {
            // One field has an error...
            return false;
        }

        if (!sPassword.equals(sPasswordConfirm)) {
            showMessage(R.string.error_pass_match);
            return false;
        }

        if (sPassword.isEmpty() && sKeyFilePath.isEmpty()) {
            showMessage(R.string.error_nopass);
            return false;
        }

        // Enrich filename with filetype
        if (!sFileName.contains(".")) {
            sFileName += ".kdbx";
        }

        // Try to create the file
        File file = new File(selectedPath.path + "/" + sFileName);
        try {
            if (file.exists()) {
                showMessage(R.string.error_database_exists);
                return false;
            }
            File parent = file.getParentFile();

            if (parent == null || (parent.exists() && !parent.isDirectory())) {
                showMessage(R.string.error_invalid_path);
                return false;
            }

            if (!parent.exists()) {
                // Create parent dircetory
                if (!parent.mkdirs()) {
                    showMessage(R.string.error_could_not_create_parent);
                    return false;
                }
            }

            file.createNewFile();

            CreateDB createDB = new CreateDB(this, sFileName, new OnFinish(){

            }, true);

            ProgressTask createTask = new ProgressTask(this, createDB, R.string.progress_create);
            createTask.run();

        } catch (IOException e) {
            showMessage(R.string.error_could_not_create_parent, e.getLocalizedMessage());
            return false;
        }

        // Prep an object to collect a password once the database has
        // been created
        /*FileSelectActivity.CollectPassword password = new FileSelectActivity.CollectPassword(
                new FileSelectActivity.LaunchGroupActivity(filename));

        // Create the new database
        CreateDB create = new CreateDB(this, sFileName, password, true);
        ProgressTask createTask = new ProgressTask(
                FileSelectActivity.this, create,
                R.string.progress_create);
        createTask.run();*/

        return true;
    }

    private void showMessage(int messageId) {
        showMessage(getText(messageId));
    }

    private void showMessage(int messageId, CharSequence additionalMessage) {
        showMessage(getText(messageId) + " " + additionalMessage);
    }

    private void showMessage(CharSequence message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private class Field {
        String fieldContent;
        int messageId;
        Field(String fieldContent, int messageId) {
            this.fieldContent = fieldContent;
            this.messageId = messageId;
        }
    }

    private static class Path {
        String path;
        String displayName;
        Path(String path, String displayName) {
            this.path = path;
            this.displayName = displayName;
        }
        static Path parseActivityResult(String dataString) {
            return new Path (
                    dataString,
                    dataString
            );
        }
    }

}
