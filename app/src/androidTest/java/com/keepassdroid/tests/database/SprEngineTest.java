/*
 * Copyright 2014-2016 Brian Pellin.
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
package com.keepassdroid.tests.database;

import java.io.InputStream;
import java.util.UUID;

import android.content.Context;
import android.content.res.AssetManager;
import android.test.AndroidTestCase;
import biz.source_code.base64Coder.Base64Coder;

import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.database.PwEntryV4;
import com.keepassdroid.database.load.ImporterV4;
import com.keepassdroid.utils.SprEngine;
import com.keepassdroid.utils.Types;

public class SprEngineTest extends AndroidTestCase {
	private PwDatabaseV4 db;
	private SprEngine spr;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		Context ctx = getContext();
		
		AssetManager am = ctx.getAssets();
		InputStream is = am.open("test.kdbx", AssetManager.ACCESS_STREAMING);
		
		ImporterV4 importer = new ImporterV4();
		db = importer.openDatabase(is, "12345", null);
		
		is.close();
		
		spr = SprEngine.getInstance(db);
	}
	
	private final String REF = "{REF:P@I:2B1D56590D961F48A8CE8C392CE6CD35}";
	private final String ENCODE_UUID = "IN7RkON49Ui1UZ2ddqmLcw==";
	private final String RESULT = "Password";
	public void testRefReplace() {
		UUID entryUUID = decodeUUID(ENCODE_UUID);
		
		PwEntryV4 entry = (PwEntryV4) db.entries.get(entryUUID);

		
		assertEquals(RESULT, spr.compile(REF, entry, db));
		
	}
	
	private UUID decodeUUID(String encoded) {
		if (encoded == null || encoded.length() == 0 ) {
			return PwDatabaseV4.UUID_ZERO;
		}
		
		byte[] buf = Base64Coder.decode(encoded);
		return Types.bytestoUUID(buf);
	}

}
