package com.keepassdroid.release.tests;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.Provider;
import java.security.Security;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.Assert.*;

/** No application compile dependency: these tests cannot add R8 keep roots. */
public class OptimizedReleaseTest {
    private Instrumentation instrumentation;
    private Context target;
    private volatile Activity resumed;
    private final Application.ActivityLifecycleCallbacks callbacks = new Application.ActivityLifecycleCallbacks() {
        public void onActivityResumed(Activity activity) { resumed = activity; }
        public void onActivityCreated(Activity activity, Bundle state) {}
        public void onActivityStarted(Activity activity) {}
        public void onActivityPaused(Activity activity) {}
        public void onActivityStopped(Activity activity) {}
        public void onActivitySaveInstanceState(Activity activity, Bundle state) {}
        public void onActivityDestroyed(Activity activity) {}
    };

    @Before public void setUp() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        target = instrumentation.getTargetContext();
        ((Application) target.getApplicationContext()).registerActivityLifecycleCallbacks(callbacks);
    }

    @After public void tearDown() {
        ((Application) target.getApplicationContext()).unregisterActivityLifecycleCallbacks(callbacks);
        if (resumed != null) instrumentation.runOnMainSync(() -> resumed.finish());
    }

    private int id(String name) {
        int id = target.getResources().getIdentifier(name, "id", target.getPackageName());
        assertTrue("Missing view " + name, id != 0);
        return id;
    }

    private Activity launch(String className, Intent intent) {
        intent.setClassName(target.getPackageName(), className);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return instrumentation.startActivitySync(intent);
    }

    private String extract(String asset) throws Exception {
        android.net.Uri uri = android.net.Uri.parse("content://com.keepassdroid.release.tests.fixtures/" + asset);
        target.getContentResolver().delete(uri, null, null);
        // Force initialization before the app tries to read it.
        try (InputStream input = target.getContentResolver().openInputStream(uri)) {
            assertNotNull(input);
        }
        return uri.toString();
    }

    private void unlock(String asset, String password, String keyAsset) throws Exception {
        unlockPath(extract(asset), password, keyAsset == null ? null : extract(keyAsset));
    }

    private void unlockPath(String uri, String password, String keyUri) throws Exception {
        Intent intent = new Intent().putExtra("fileName", uri);
        if (keyUri != null) intent.putExtra("keyFile", keyUri);
        Activity activity = launch("com.keepassdroid.PasswordActivity", intent);
        instrumentation.runOnMainSync(() -> {
            ((EditText) activity.findViewById(id("password"))).setText(password);
            activity.findViewById(id("pass_ok")).performClick();
        });
        long deadline = android.os.SystemClock.uptimeMillis() + 30000;
        while (android.os.SystemClock.uptimeMillis() < deadline) {
            instrumentation.waitForIdleSync();
            Activity current = resumed;
            if (current != null && current != activity && hasView(current, id("group_list"))) return;
            Thread.sleep(100);
        }
        fail("Optimized APK failed to unlock " + uri);
    }

    private boolean hasView(Activity activity, int viewId) {
        AtomicBoolean found = new AtomicBoolean();
        instrumentation.runOnMainSync(() -> found.set(activity.findViewById(viewId) != null));
        return found.get();
    }

    @Test public void unlockKdb() throws Exception { unlock("test1.kdb", "12345", null); }
    @Test public void unlockTwofish() throws Exception { unlock("twofish.kdb", "12345", null); }
    @Test public void unlockKdbx() throws Exception { unlock("test.kdbx", "12345", null); }
    @Test public void unlockKdbxV4() throws Exception { unlock("test-kdbxv4.kdbx", "1", null); }
    @Test public void unlockXmlKeyFile() throws Exception { unlock("kdb_with_xml_keyfile.kdb", "12345", "keyfile.key"); }
    @Test public void unlockBinaryKeyFile() throws Exception { unlock("binary-key.kdb", "12345", "binary.key"); }

    @Test public void saveAndReopenKdb() throws Exception { saveAndReopen("test1.kdb", "12345"); }
    @Test public void saveAndReopenKdbx() throws Exception { saveAndReopen("test.kdbx", "12345"); }
    @Test public void saveAndReopenKdbxV4() throws Exception { saveAndReopen("test-kdbxv4.kdbx", "1"); }

    private int groupCount(Activity activity) {
        AtomicInteger count = new AtomicInteger(-1);
        instrumentation.runOnMainSync(() -> {
            ListView list = activity.findViewById(id("group_list"));
            if (list != null) count.set(list.getCount());
        });
        return count.get();
    }

    private void saveAndReopen(String asset, String password) throws Exception {
        String uri = extract(asset);
        unlockPath(uri, password, null);
        Activity group = resumed;
        int expectedCount = groupCount(group) + 1;
        instrumentation.runOnMainSync(() -> group.findViewById(id("add_group")).performClick());
        long deadline = android.os.SystemClock.uptimeMillis() + 10000;
        while ((resumed == group || !hasView(resumed, id("ok")))
                && android.os.SystemClock.uptimeMillis() < deadline) Thread.sleep(100);
        Activity editor = resumed;
        assertNotSame("Group editor did not open", group, editor);
        instrumentation.runOnMainSync(() -> {
            ((EditText) editor.findViewById(id("group_name"))).setText("R8 saved group");
            editor.findViewById(id("ok")).performClick();
        });
        deadline = android.os.SystemClock.uptimeMillis() + 30000;
        while (groupCount(group) != expectedCount && android.os.SystemClock.uptimeMillis() < deadline) Thread.sleep(100);
        assertEquals("Group save failed", expectedCount, groupCount(group));
        unlockPath(uri, password, null); // Clears the in-memory DB and reads the saved file.
        assertEquals("Saved group missing after reopen", expectedCount, groupCount(resumed));
    }

    @Test public void providerAlgorithmsAndNativeBindings() throws Exception {
        unlock("test.kdbx", "12345", null); // Initializes the app's provider and native libraries.
        Provider bc = Security.getProvider("BC");
        assertNotNull(bc);
        for (String transformation : new String[]{"AES/CBC/PKCS7Padding", "Twofish/CBC/PKCS7Padding", "ChaCha7539"}) {
            roundTrip(transformation, bc, transformation.equals("ChaCha7539") ? 12 : 16);
        }
        Provider nativeProvider = new Provider("ReleaseTestNative", 1.0, "") {};
        nativeProvider.put("Cipher.AES", "com.keepassdroid.crypto.NativeAESCipherSpi");
        roundTrip("AES/CBC/PKCS5Padding", nativeProvider, 16); // No fallback path.
        Class<?> finalKey = target.getClassLoader().loadClass("com.keepassdroid.crypto.finalkey.NativeFinalKey");
        Method transform = finalKey.getDeclaredMethod("nTransformMasterKey", byte[].class, byte[].class, long.class);
        transform.setAccessible(true);
        byte[] seed = new byte[32], key = new byte[32];
        Cipher aes = Cipher.getInstance("AES/ECB/NoPadding");
        aes.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(seed, "AES"));
        byte[] expected = key;
        for (int i = 0; i < 10; i++) expected = aes.doFinal(expected);
        assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(expected),
                (byte[]) transform.invoke(null, seed, key, 10L));
        Class<?> argon = target.getClassLoader().loadClass("com.keepassdroid.crypto.keyDerivation.Argon2Native");
        Method derive = argon.getDeclaredMethod("nTransformMasterKey", byte[].class, byte[].class,
                int.class, long.class, long.class, byte[].class, byte[].class, long.class, int.class);
        derive.setAccessible(true);
        for (int type : new int[]{0, 2}) {
            byte[] actual = (byte[]) derive.invoke(null, "page-size-regression".getBytes("UTF-8"),
                    "keepassdroid-salt".getBytes("UTF-8"), 1, 32768L, 2L, null, null, 19L, type);
            String vector = type == 0 ? "ecfde27f57cbb359fa72e7251598530c3d7c0c9704c10577bc6924d666db9f6b" : "dce6ffd42817c9011a1910df2b56d911758039304f11c4b04d6684a655fdf4f0";
            assertEquals(vector, hex(actual));
        }
    }

    private void roundTrip(String transformation, Provider provider, int ivLength) throws Exception {
        String algorithm = transformation.split("/")[0];
        SecretKeySpec key = new SecretKeySpec(new byte[32], algorithm);
        IvParameterSpec iv = new IvParameterSpec(new byte[ivLength]);
        Cipher encrypt = Cipher.getInstance(transformation, provider);
        Cipher decrypt = Cipher.getInstance(transformation, provider);
        encrypt.init(Cipher.ENCRYPT_MODE, key, iv);
        decrypt.init(Cipher.DECRYPT_MODE, key, iv);
        byte[] plain = "optimized release crypto regression".getBytes("UTF-8");
        assertArrayEquals(plain, decrypt.doFinal(encrypt.doFinal(plain)));
    }

    private String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) result.append(String.format("%02x", b & 255));
        return result.toString();
    }

    @Test public void iconsSurviveShrinking() {
        Activity activity = launch("com.keepassdroid.IconPickerActivity", new Intent());
        instrumentation.runOnMainSync(() -> {
            GridView grid = activity.findViewById(id("IconGridView"));
            assertNotNull(grid);
            assertEquals(69, grid.getAdapter().getCount());
            for (int i = 0; i < 69; i++) assertNotNull(grid.getAdapter().getView(i, null, grid));
        });
    }

    @Test public void settingsFragmentsSurviveShrinking() {
        Activity activity = launch("com.keepassdroid.settings.AppSettingsActivity", new Intent());
        instrumentation.waitForIdleSync();
        assertFalse(activity.isFinishing());
        // Preference XML names must remain loadable with public no-arg constructors.
        for (String name : new String[]{"AppSettingsFragement", "DBSettingsFragement"}) {
            try {
                assertNotNull(target.getClassLoader().loadClass("com.keepassdroid.settings." + name).getConstructor().newInstance());
            } catch (Exception e) { throw new AssertionError(e); }
        }
    }
}
