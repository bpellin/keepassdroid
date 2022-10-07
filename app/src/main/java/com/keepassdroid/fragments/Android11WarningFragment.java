/*
 * Copyright 2022 Brian Pellin
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

import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.android.keepass.R;
import com.keepassdroid.utils.EmptyUtils;

public class Android11WarningFragment extends DialogFragment {
    int resId;

    public static boolean showAndroid11Warning(String filename) {
        if (EmptyUtils.isNullOrEmpty(filename)) { return false; }

        Uri fileUri = Uri.parse(filename);
        return showAndroid11Warning(fileUri);
    }

    public static boolean showAndroid11Warning(Uri fileUri) {
        if (fileUri == null) { return false; }

        String scheme = fileUri.getScheme();
        if (scheme == null) { return true; }
        
        return scheme.equals("file") && showAndroid11WarningOnThisVersion();
    }

    public static boolean showAndroid11WarningOnThisVersion() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R);
    }

    public Android11WarningFragment() {
        this.resId = R.string.Android11FileNotFound;
    }
    public Android11WarningFragment(int resId) {
       this.resId = resId;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(resId)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

        return builder.create();
    }
}
