/*
 * Copyright 2018-2025 Brian Pellin.
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
package com.keepassdroid.utils;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.keepass.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class NotificationUtil {
    public static final String COPY_CHANNEL_ID = "copy";
    public static final String COPY_CHANNEL_NAME = "Copy username and password";

    public static void createChannels(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(COPY_CHANNEL_ID,
                    COPY_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);

            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

            NotificationManager manager =
                    (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return;

            manager.createNotificationChannel(channel);
        }
    }

    public static boolean requestPermission(Activity act, ActivityResultLauncher<String> requestLauncher) {
        if (ContextCompat.checkSelfPermission(act, "android.permission.POST_NOTIFICATIONS")
            != PackageManager.PERMISSION_GRANTED) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(act,
                        Manifest.permission.POST_NOTIFICATIONS)) {

                    showNotificationPermissionRationale(act, requestLauncher);
                    return false;
                } else {
                    // Request permissions
                    requestLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    return false;
                }
            }
        }

        return true;
    }

    private static void showNotificationPermissionRationale(Activity act, ActivityResultLauncher<String> requestLauncher) {
        AlertDialog.Builder builder = new AlertDialog.Builder(act);
        builder.setTitle(R.string.notification_permission_title)
                .setMessage(R.string.notification_permission_text)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            requestLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
