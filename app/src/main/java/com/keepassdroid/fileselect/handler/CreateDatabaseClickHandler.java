package com.keepassdroid.fileselect.handler;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.widget.AppCompatEditText;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.keepass.R;
import com.keepassdroid.ProgressTask;
import com.keepassdroid.compat.StorageAF;
import com.keepassdroid.database.edit.CreateDB;
import com.keepassdroid.fileselect.BrowserDialog;
import com.keepassdroid.fileselect.CreateDatabaseActivity;
import com.keepassdroid.fileselect.FileSelectActivity;
import com.keepassdroid.utils.Util;

import java.io.File;
import java.io.IOException;

import static android.os.Build.VERSION_CODES.LOLLIPOP;

/**
 * ClickHandler to create a new Database. This should only be used with the new design.
 */
public class CreateDatabaseClickHandler implements View.OnClickListener {

    private FileSelectActivity activity;

    public CreateDatabaseClickHandler(FileSelectActivity activity) {
        this.activity = activity;
    }

    private void showMessage(int messageId) {
        showMessage(activity.getText(messageId));
    }

    private void showMessage(int messageId, CharSequence additionalMessage) {
        showMessage(activity.getText(messageId) + " " + additionalMessage);
    }

    private void showMessage(CharSequence message) {
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
    }

    private boolean handlePositiveButtonClick(View dialogView) {
        String path = ((AppCompatEditText) dialogView.findViewById(R.id.path)).getText().toString();
        if (path.length() == 0) {
            // Make sure path name exists
            showMessage(R.string.error_path_required);
            return false;
        }

        String fileName = ((AppCompatEditText) dialogView.findViewById(R.id.filename)).getText().toString();
        if (fileName.length() == 0) {
            // Make sure file name exists
            showMessage(R.string.error_filename_required);
            return false;
        }
        if (!fileName.contains(".")) {
            fileName += ".kdbx";
        }

        // Try to create the file
        File file = new File(path + "/" + fileName);
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
        } catch (IOException e) {
            showMessage(R.string.error_could_not_create_parent, e.getLocalizedMessage());
            return false;
        }
        return true;

        // Prep an object to collect a password once the database has
        // been created
        /* FileSelectActivity.CollectPassword password = new FileSelectActivity.CollectPassword(
                new FileSelectActivity.LaunchGroupActivity(filename));

        // Create the new database
        CreateDB create = new CreateDB(FileSelectActivity.this, filename, password, true);
        ProgressTask createTask = new ProgressTask(
                FileSelectActivity.this, create,
                R.string.progress_create);
        createTask.run();*/
    }

    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void onClick(View v) {
        Intent intent = activity.getIntent();
        intent.setClass(activity, CreateDatabaseActivity.class);
        activity.startActivity(intent);
        /*final View dialogView = activity.getLayoutInflater().inflate(R.layout.create_dialog, null);

        final AlertDialog createDialog = new AlertDialog.Builder(activity)
                .setCancelable(true)
                .setPositiveButton(R.string.entry_save, null)
                .setNegativeButton(android.R.string.cancel, null)
                .setTitle(R.string.create_database)
                .setView(dialogView)
                .show();

        createDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (handlePositiveButtonClick(dialogView)) {
                    createDialog.dismiss();
                }
            }
        });

        // Handle clicks on path-input
        dialogView.findViewById(R.id.path).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (StorageAF.useStorageFramework(activity)) {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    activity.startActivityForResult(i, 1);
                }
            }
        });

        createDialog.show();*/
    }
}
