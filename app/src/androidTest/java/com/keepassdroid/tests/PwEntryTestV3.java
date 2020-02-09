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

import com.keepassdroid.database.PwEntryV3;
import com.keepassdroid.tests.database.TestData;

import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class PwEntryTestV3 {
	PwEntryV3 mPE;

	@Before
	public void setUp() throws Exception {
		Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();

		mPE = (PwEntryV3) TestData.GetTest1(ctx).entries.get(0);
		
	}

	@Test
	public void testName() {
		assertTrue("Name was " + mPE.title, mPE.title.equals("Amazon"));
	}

	@Test
	public void testPassword() throws UnsupportedEncodingException {
		String sPass = "12345";
		byte[] password = sPass.getBytes("UTF-8");
		
		assertArrayEquals(password, mPE.getPasswordBytes());
	}

	@Test
	public void testCreation() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(mPE.tCreation.getJDate());
		
		assertEquals("Incorrect year.", cal.get(Calendar.YEAR), 2009);
		assertEquals("Incorrect month.", cal.get(Calendar.MONTH), 3);
		assertEquals("Incorrect day.", cal.get(Calendar.DAY_OF_MONTH), 23);
	}
}
