/*
 * Copyright 2014-2020 Brian Pellin.
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
package com.keepassdroid.tests.utils;

import com.keepassdroid.utils.StrUtil;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class StrUtilTest {
    private final String text = "AbCdEfGhIj";
    private final String search = "BcDe";
    private final String badSearch = "Ed";

    @Test
	public void testIndexOfIgnoreCase1() {
		assertEquals(1, StrUtil.indexOfIgnoreCase(text, search, Locale.ENGLISH));
	}

	@Test
	public void testIndexOfIgnoreCase2() {
		assertEquals(-1, StrUtil.indexOfIgnoreCase(text, search, Locale.ENGLISH), 2);
	}

	@Test
	public void testIndexOfIgnoreCase3() {
		assertEquals(-1, StrUtil.indexOfIgnoreCase(text, badSearch, Locale.ENGLISH));
	}
	
	private final String repText = "AbCtestingaBc";
	private final String repSearch = "ABc";
	private final String repSearchBad = "CCCCCC";
	private final String repNew = "12345";
	private final String repResult = "12345testing12345";

	@Test
	public void testReplaceAllIgnoresCase1() {
		assertEquals(repResult, StrUtil.replaceAllIgnoresCase(repText, repSearch, repNew, Locale.ENGLISH));
	}

	@Test
	public void testReplaceAllIgnoresCase2() {
		assertEquals(repText, StrUtil.replaceAllIgnoresCase(repText, repSearchBad, repNew, Locale.ENGLISH));
	}
}
