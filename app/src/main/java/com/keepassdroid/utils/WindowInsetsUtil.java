/*
 * Copyright 2026 Brian Pellin.
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

import android.app.Activity;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Keeps the legacy activity layouts clear of system UI under enforced edge-to-edge. */
public final class WindowInsetsUtil {
    private WindowInsetsUtil() {
    }

    public static void apply(Activity activity) {
        if (Build.VERSION.SDK_INT < 35) {
            return;
        }

        TypedValue floating = new TypedValue();
        if (activity.getTheme().resolveAttribute(android.R.attr.windowIsFloating, floating, true)
                && floating.data != 0) {
            // Floating dialogs retain the platform's own window fitting behavior.
            return;
        }

        // Request IME insets rather than allowing the window to pan a focused field.
        activity.getWindow().setSoftInputMode(
                (activity.getWindow().getAttributes().softInputMode
                        & ~WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST)
                        | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        applyToContent(activity.findViewById(android.R.id.content));
    }

    static void applyToContent(View content) {
        final int left = content.getPaddingLeft();
        final int top = content.getPaddingTop();
        final int right = content.getPaddingRight();
        final int bottom = content.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(content, (view, windowInsets) -> {
            Insets safe = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(left + safe.left, top + safe.top,
                    right + safe.right, bottom + safe.bottom);
            // This container owns all insets; descendants must not apply them again.
            return WindowInsetsCompat.CONSUMED;
        });
        // Called during onCreate, before the first attachment/inset dispatch. The content
        // container survives subsequent setContentView calls, including programmatic screens.
        ViewCompat.requestApplyInsets(content);
    }
}
