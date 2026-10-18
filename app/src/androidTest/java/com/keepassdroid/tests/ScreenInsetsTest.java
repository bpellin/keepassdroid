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
package com.keepassdroid.tests;

import android.os.Build;
import android.view.View;
import android.widget.FrameLayout;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import com.keepassdroid.GroupEditActivity;
import com.keepassdroid.fileselect.FileSelectActivity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assume.assumeTrue;

public class ScreenInsetsTest {
    @Test
    public void fileSelectionKeepsInsetsWhenContentIsReplaced() {
        assumeTrue(Build.VERSION.SDK_INT >= 35);
        ActivityTestRule<FileSelectActivity> rule =
                new ActivityTestRule<>(FileSelectActivity.class, false, false);
        FileSelectActivity activity = rule.launchActivity(null);
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                View content = activity.findViewById(android.R.id.content);
                WindowInsetsCompat insets = new WindowInsetsCompat.Builder()
                        .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 24, 0, 48))
                        .build();
                ViewCompat.dispatchApplyWindowInsets(content, insets);
                assertEquals(24, content.getPaddingTop());
                assertEquals(48, content.getPaddingBottom());
                activity.setContentView(new FrameLayout(activity));
                assertSame(content, activity.findViewById(android.R.id.content));
                ViewCompat.dispatchApplyWindowInsets(content, insets);
                assertEquals(24, content.getPaddingTop());
                assertEquals(48, content.getPaddingBottom());
            });
        } finally {
            rule.finishActivity();
        }
    }

    @Test
    public void floatingGroupEditorDoesNotApplyFullscreenInsets() {
        assumeTrue(Build.VERSION.SDK_INT >= 35);
        ActivityTestRule<GroupEditActivity> rule =
                new ActivityTestRule<>(GroupEditActivity.class, false, false);
        GroupEditActivity activity = rule.launchActivity(null);
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                View content = activity.findViewById(android.R.id.content);
                int top = content.getPaddingTop();
                int bottom = content.getPaddingBottom();
                ViewCompat.dispatchApplyWindowInsets(content, new WindowInsetsCompat.Builder()
                        .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 24, 0, 48))
                        .build());
                assertEquals(top, content.getPaddingTop());
                assertEquals(bottom, content.getPaddingBottom());
            });
        } finally {
            rule.finishActivity();
        }
    }
}
