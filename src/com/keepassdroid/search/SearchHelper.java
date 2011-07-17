/*
 * Copyright 2009-2011 Brian Pellin.
 * Copyright 2011 riku salkia
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
package com.keepassdroid.search;

import java.util.List;

import com.keepassdroid.Database;
import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.PwGroup;

public interface SearchHelper {

    /**
     * Called before insert/update/delete for opening a backing resource (database/file/whatever)
     * @return this
     */
    public abstract SearchHelper open();

    /**
     * Called after insert/update/delete for closing backing resource (database/file/whatever)
     * @return this
     */
    public abstract void close();

    public abstract void clear();

    public abstract void insertEntry(PwDatabase db, PwEntry entry);

    public abstract void insertEntry(PwDatabase db, List<? extends PwEntry> entries);

    public abstract void updateEntry(PwDatabase db, PwEntry entry);

    public abstract void deleteEntry(PwEntry entry);

    public abstract PwGroup search(Database db, String qStr);

}