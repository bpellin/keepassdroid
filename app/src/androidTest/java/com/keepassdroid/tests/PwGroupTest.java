/*
 * Copyright 2009-2020 Brian Pellin.
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


import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.keepassdroid.database.PwGroupV3;
import com.keepassdroid.tests.database.TestData;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;

public class PwGroupTest {

	PwGroupV3 mPG;

	@Before
	public void setUp() throws Exception {

		Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
		mPG = (PwGroupV3) TestData.GetTest1(ctx).getGroups().get(0);
		
	}

	@Test
	public void testGroupName() {
		 assertTrue("Name was " + mPG.name, mPG.name.equals("Internet"));
	}
}

