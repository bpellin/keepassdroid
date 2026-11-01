/*
 * Copyright 2010-2012 Brian Pellin.
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
package com.keepassdroid.icons;

import com.android.keepass.R;

public class Icons {
    // KeePass icon IDs are part of the database format. Keep this order stable.
    // Direct references also let R8 retain the resources without reflection.
    private static final int[] ICONS = {
        R.drawable.ic00, R.drawable.ic01, R.drawable.ic02, R.drawable.ic03, R.drawable.ic04, R.drawable.ic05, R.drawable.ic06, R.drawable.ic07,
        R.drawable.ic08, R.drawable.ic09, R.drawable.ic10, R.drawable.ic11, R.drawable.ic12, R.drawable.ic13, R.drawable.ic14, R.drawable.ic15,
        R.drawable.ic16, R.drawable.ic17, R.drawable.ic18, R.drawable.ic19, R.drawable.ic20, R.drawable.ic21, R.drawable.ic22, R.drawable.ic23,
        R.drawable.ic24, R.drawable.ic25, R.drawable.ic26, R.drawable.ic27, R.drawable.ic28, R.drawable.ic29, R.drawable.ic30, R.drawable.ic31,
        R.drawable.ic32, R.drawable.ic33, R.drawable.ic34, R.drawable.ic35, R.drawable.ic36, R.drawable.ic37, R.drawable.ic38, R.drawable.ic39,
        R.drawable.ic40, R.drawable.ic41, R.drawable.ic42, R.drawable.ic43, R.drawable.ic44, R.drawable.ic45, R.drawable.ic46, R.drawable.ic47,
        R.drawable.ic48, R.drawable.ic49, R.drawable.ic50, R.drawable.ic51, R.drawable.ic52, R.drawable.ic53, R.drawable.ic54, R.drawable.ic55,
        R.drawable.ic56, R.drawable.ic57, R.drawable.ic58, R.drawable.ic59, R.drawable.ic60, R.drawable.ic61, R.drawable.ic62, R.drawable.ic63,
        R.drawable.ic64, R.drawable.ic65, R.drawable.ic66, R.drawable.ic67, R.drawable.ic68
    };

    public static int iconToResId(int iconId) {
        return iconId >= 0 && iconId < ICONS.length ? ICONS[iconId] : R.drawable.ic99_blank;
    }

    public static int count() {
        return ICONS.length;
    }
}
