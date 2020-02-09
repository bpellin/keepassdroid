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
package com.keepassdroid.tests.database;

import android.content.Context;
import android.content.res.AssetManager;

import androidx.test.platform.app.InstrumentationRegistry;

import com.keepassdroid.database.PwDatabaseV3;
import com.keepassdroid.database.PwEncryptionAlgorithm;
import com.keepassdroid.database.load.ImporterV3;

import org.junit.Test;

import java.io.InputStream;

import static junit.framework.TestCase.assertTrue;

public class Kdb3Twofish {
    @Test
	public void testReadTwofish() throws Exception {
		Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();

		AssetManager am = ctx.getAssets();
		InputStream is = am.open("twofish.kdb", AssetManager.ACCESS_STREAMING);
		
		ImporterV3 importer = new ImporterV3();

		PwDatabaseV3 db = importer.openDatabase(is, "12345", null);
		
		assertTrue(db.algorithm == PwEncryptionAlgorithm.Twofish);
		
		is.close();

	}

}
