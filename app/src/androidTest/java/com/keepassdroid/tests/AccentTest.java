/*
* Copyright 2009-2019 Brian Pellin.
*
* This file is part of KeePassDroid.
*
* KeePassDroid is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* KeePassDroid is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with KeePassDroid. If not, see <http://www.gnu.org/licenses/>.
*
*/
package com.keepassdroid.tests;

import android.content.Context;

import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import com.keepassdroid.tests.database.TestData;

import org.junit.Test;

import static org.junit.Assert.assertTrue;


@SmallTest
public class AccentTest {
	
	private static final String KEYFILE = "";
	private static final String PASSWORD = "é";
	private static final String ASSET = "accent.kdb";
	private static final String FILENAME = "/sdcard/accent.kdb";

	@Test
	public void testOpen() {

		try {
			Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
			TestData.GetDb(ctx, ASSET, PASSWORD, KEYFILE, FILENAME);
		} catch (Exception e) {
			assertTrue("Failed to open database", false);

		}
	}

}
