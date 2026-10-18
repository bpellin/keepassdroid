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

import android.os.Build;
import android.view.View;
import android.widget.FrameLayout;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class WindowInsetsUtilTest {
    @Test
    public void insetsPreservePaddingAndDoNotAccumulate() {
        assumeTrue(Build.VERSION.SDK_INT >= 21);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            View content = new FrameLayout(
                    InstrumentationRegistry.getInstrumentation().getTargetContext());
            content.setPadding(2, 3, 5, 7);
            WindowInsetsUtil.applyToContent(content);

            WindowInsetsCompat bars = new WindowInsetsCompat.Builder()
                    .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 24, 0, 48))
                    .setInsets(WindowInsetsCompat.Type.displayCutout(), Insets.of(80, 0, 0, 0))
                    .build();
            assertTrue(ViewCompat.dispatchApplyWindowInsets(content, bars).isConsumed());
            ViewCompat.dispatchApplyWindowInsets(content, bars);
            assertPadding(content, 82, 27, 5, 55);

            WindowInsetsCompat keyboard = new WindowInsetsCompat.Builder(bars)
                    .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, 300))
                    .build();
            ViewCompat.dispatchApplyWindowInsets(content, keyboard);
            assertPadding(content, 82, 27, 5, 307);

            // Keyboard dismissal and rotation must remove the old padding.
            ViewCompat.dispatchApplyWindowInsets(content, new WindowInsetsCompat.Builder()
                    .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 24, 48, 0))
                    .build());
            assertPadding(content, 2, 27, 53, 7);
        });
    }

    private static void assertPadding(View view, int left, int top, int right, int bottom) {
        assertEquals(left, view.getPaddingLeft());
        assertEquals(top, view.getPaddingTop());
        assertEquals(right, view.getPaddingRight());
        assertEquals(bottom, view.getPaddingBottom());
    }
}
