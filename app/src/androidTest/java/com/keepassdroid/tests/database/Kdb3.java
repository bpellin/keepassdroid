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
import android.os.Environment;

import androidx.test.platform.app.InstrumentationRegistry;

import com.keepassdroid.database.load.ImporterV3;
import com.keepassdroid.tests.TestUtil;

import org.junit.Test;

import java.io.File;
import java.io.InputStream;

public class Kdb3 {

	private void testKeyfile(String dbAsset, String keyAsset, String password) throws Exception {
		Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();

		File sdcard = Environment.getExternalStorageDirectory();
		String keyPath = ctx.getFilesDir().getAbsolutePath() + "/key";
		
		TestUtil.extractKey(ctx, keyAsset, keyPath);
		
		AssetManager am = ctx.getAssets();
		InputStream is = am.open(dbAsset, AssetManager.ACCESS_STREAMING);
		
		ImporterV3 importer = new ImporterV3();
		importer.openDatabase(is, password, TestUtil.getKeyFileInputStream(ctx, keyPath));
		
		is.close();
	}

	@Test
	public void testXMLKeyFile() throws Exception {
		testKeyfile("kdb_with_xml_keyfile.kdb", "keyfile.key", "12345");
	}

	@Test
	public void testBinary64KeyFile() throws Exception {
		testKeyfile("binary-key.kdb", "binary.key", "12345");
	}

}
