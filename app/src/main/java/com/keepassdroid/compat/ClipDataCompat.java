/*
 * Copyright 2016-2022 Brian Pellin.
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

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.net.Uri;

import java.lang.reflect.Method;

public class ClipDataCompat {
    private static Method getClipDataFromIntent;
    private static Class persistableBundle;
    private static Method putBoolean;
    private static Method setExtras;

    private static boolean initSucceded;

    static {
        try {
            getClipDataFromIntent = Intent.class.getMethod("getClipData", (Class[])null);

            persistableBundle = Class.forName("android.os.PersistableBundle");
            putBoolean = persistableBundle.getMethod("putBoolean",
                    new Class[] {String.class, boolean.class});

            setExtras = ClipDescription.class.getMethod("setExtras",
                    new Class[]{persistableBundle});

            initSucceded = true;
        } catch (Exception e) {
            initSucceded = false;
        }
    }

    public static Uri getUriFromIntent(Intent i, String key) {
        boolean clipDataSucceeded = false;
        if (initSucceded) {
            try {
                ClipData clip = (ClipData) getClipDataFromIntent.invoke(i, null);

                if (clip != null) {
                    ClipDescription clipDescription = clip.getDescription();
                    CharSequence label = clipDescription.getLabel();
                    if (label.equals(key)) {
                        int itemCount = clip.getItemCount();
                        if (itemCount == 1) {
                            ClipData.Item clipItem = clip.getItemAt(0);
                            if (clipItem != null) {
                                return clipItem.getUri();
                            }
                        }
                    }
                }
                return null;

            } catch (Exception e) {
                // Fall through below to backup method if reflection fails
            }
        }

        return i.getParcelableExtra(key);
    }

    public static void markSensitive(ClipData clipData) {

        try {
            Object extras = persistableBundle.newInstance();
            putBoolean.invoke(extras, "android.content.extra.IS_SENSITIVE", true);

            setExtras.invoke(clipData.getDescription(), extras);
        } catch (Exception e) {
            // Do nothing if this fails
        }
    }
}
