/*
 * Copyright 2010-2020 Brian Pellin.
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

import com.keepassdroid.database.PwDate;

import org.junit.Test;

import static junit.framework.TestCase.assertTrue;

public class PwDateTest {
    @Test
	public void testDate() {
		PwDate jDate = new PwDate(System.currentTimeMillis());
		
		PwDate intermediate = (PwDate) jDate.clone();
		
		PwDate cDate = new PwDate(intermediate.getCDate(), 0);
		
		assertTrue("jDate and intermediate not equal", jDate.equals(intermediate));
		assertTrue("jDate and cDate not equal", cDate.equals(jDate));
		
	}
}
