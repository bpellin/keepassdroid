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
        Kdb4Header.class,
        ProtectedBinaryTest.class,
        SprEngineTest.class,
        PwManagerOutputTest.class,
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
