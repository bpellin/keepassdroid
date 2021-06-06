/*
 * Copyright 2021 Brian Pellin.
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
package com.keepassdroid.database;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PwCustomData extends HashMap<String, String> {
    public Map<String, Date> lastMod = new HashMap<String, Date>();

    public String put(String key, String value, Date last) {
        lastMod.put(key, last);

        return put(key, value);
    }

    public Date getLastMod(String key) {
        return lastMod.get(key);
    }
}
