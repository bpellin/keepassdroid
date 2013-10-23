/*
 * Copyright 2009-2013 Brian Pellin.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public abstract class PwGroup {
	public List<PwGroup> childGroups = new ArrayList<PwGroup>();
	public List<PwEntry> childEntries = new ArrayList<PwEntry>();
	public String name = "";
	public PwIconStandard icon;

	public abstract PwGroup getParent();
	public abstract void setParent(PwGroup parent);
	
	public abstract PwGroupId getId();
	public abstract void setId(PwGroupId id);

	public abstract String getName();
	
	public abstract Date getLastMod();
	
	public PwIcon getIcon() {
		return icon;
	}

	public void sortGroupsByName() {
		Collections.sort(childGroups, new GroupNameComparator());
	}

	public static class GroupNameComparator implements Comparator<PwGroup> {

		public int compare(PwGroup object1, PwGroup object2) {
			return object1.getName().compareToIgnoreCase(object2.getName());
		}
		
	}
	
	public abstract void setLastAccessTime(Date date);

	public abstract void setLastModificationTime(Date date);
	
	public void sortEntriesByName() {
		Collections.sort(childEntries, new PwEntry.EntryNameComparator());
	}
	
	public void initNewGroup(String nm, PwGroupId newId) {
		setId(newId);
		name = nm;
	}
	
	public boolean isContainedIn(PwGroup container) {
		PwGroup cur = this;
		while (cur != null) {
			if (cur == container) {
				return true;
			}
			
			cur = cur.getParent();
		}
		
		return false;
	}
	
	public void touch(boolean modified, boolean touchParents) {
		Date now = new Date();
		
		setLastAccessTime(now);
		
		if (modified) {
			setLastModificationTime(now);
		}
		
		PwGroup parent = getParent();
		if (touchParents && parent != null) {
			parent.touch(modified, true);
		}
	}

}
