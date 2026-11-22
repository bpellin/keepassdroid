/*
 * Copyright 2026 Brian Pellin.
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

import com.keepassdroid.tests.crypto.*;
import com.keepassdroid.tests.database.*;
import com.keepassdroid.tests.output.*;
import com.keepassdroid.tests.search.*;
import com.keepassdroid.tests.stream.*;
import com.keepassdroid.tests.utils.*;
import com.keepassdroid.tests.*;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;

//Run all unit tests
@RunWith(Suite.class)
@Suite.SuiteClasses({
        AESTest.class,
        CipherTest.class,
        FinalKeyTest.class,
        DeleteEntry.class,
        EntryV4.class,
        Kdb3.class,
        Kdb3Twofish.class,
        Kdb4.class,
        Kdb42.class,
        Kdb4Header.class,
        ProtectedBinaryTest.class,
        SprEngineTest.class,
        PwManagerOutputTest.class,
        SaveFailureDiagnosticsTest.class,
        SearchTest.class,
        HashedBlock.class,
        StrUtilTest.class,
        AccentTest.class,
        PwDateTest.class,
        PwEntryTestV3.class,
        PwEntryTestV4.class,
        TypesTest.class
})
public class UnitTestSuite {
}
