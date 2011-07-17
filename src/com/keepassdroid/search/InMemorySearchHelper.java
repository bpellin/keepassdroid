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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import android.util.Log;

import com.keepassdroid.Database;
import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.PwDatabaseV3;
import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.PwEntryV4;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.PwGroupV3;
import com.keepassdroid.database.PwGroupV4;

public class InMemorySearchHelper implements SearchHelper {
    private static final String TAG = InMemorySearchHelper.class.getSimpleName();
    private Map<UUID, Collection<String>> stringMap = new HashMap<UUID, Collection<String>>();

    @Override
    public SearchHelper open(){
        return this;
    }

    @Override
    public void close() {
    }

    @Override
    public void clear() {
    }

    @Override
    public void insertEntry(PwDatabase db, PwEntry entry) {
        UUID uuid = entry.getUUID();
        Collection<String> strings = new ArrayList<String>();
        strings.add( entry.getTitle().toLowerCase() );
        strings.add( entry.getUrl().toLowerCase() );
        strings.add( entry.getUsername().toLowerCase() );
        strings.add( entry.getNotes().toLowerCase() );
        
        if( entry instanceof PwEntryV4 ) {
            // Add Advanced keys&values https://code.google.com/p/keepassdroid/issues/detail?id=162
            PwEntryV4 v4 = (PwEntryV4) entry;
            for( String key : v4.strings.keySet() ) {
                strings.add(key.toLowerCase());
                strings.add(v4.strings.get(key).toLowerCase());
            }
        }
        
        stringMap.put(uuid, strings);
    }

    @Override
    public void insertEntry(PwDatabase db, List<? extends PwEntry> entries) {
        for( PwEntry entry : entries ) {
            insertEntry( db, entry );
        }
    }

    @Override
    public void updateEntry(PwDatabase db, PwEntry entry) {
        insertEntry(db, entry);
    }

    @Override
    public void deleteEntry(PwEntry entry) {
        stringMap.remove( entry.getUUID() );
    }

    @Override
    public PwGroup search(Database db, String qStr) {
        long start = System.currentTimeMillis();
        
        final String searchStr = qStr.toLowerCase();
        
        PwGroup group;
        if ( db.pm instanceof PwDatabaseV3 ) {
            group = new PwGroupV3();
        } else if ( db.pm instanceof PwDatabaseV4 ) {
            group = new PwGroupV4();
        } else {
            Log.d(TAG, "Tried to search with unknown db");
            return null;
        }
        group.name = "Search results";
        group.childEntries = new ArrayList<PwEntry>();
        
        int entries = 0;
        int total = 0;
        
        for( UUID uuid : stringMap.keySet() ) {
            for( String str : stringMap.get(uuid) ) {
                if( str.contains( searchStr ) ) {
                    PwEntry entry = (PwEntry) db.entries.get(uuid);
                    group.childEntries.add(entry);
                }
                
                total++;
            }
            entries++;
        }
        Log.d(TAG, String.format("Searched %d entries, %d strings. Search took %dms to complete", entries, total, System.currentTimeMillis()-start) );
        
        return group;
    }

}
