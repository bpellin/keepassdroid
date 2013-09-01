package com.keepassdroid.provider;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.text.SpannableStringBuilder;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author pfn
 */
public class Authz {
    Authz(int uid) {
        isNew = true;
        this.uid = uid;
    }

    Authz(int uid, String signatures, long lastAuthz, boolean authz,
          boolean remember) {
        this.uid = uid;
        this.signatures = signatures;
        this.lastAuthz = lastAuthz;
        this.authz = authz;
        this.remember = remember;
        isNew = false;
    }
    public final boolean isNew;

    public int uid;
    public String signatures;
    public long lastAuthz;
    public boolean remember;
    public boolean authz;

    public CharSequence appNames;

    public ContentValues values() {
        ContentValues values = new ContentValues();
        values.put(AuthzDatabaseHelper.FIELD_UID,        uid);
        values.put(AuthzDatabaseHelper.FIELD_SIGNATURES, signatures);
        values.put(AuthzDatabaseHelper.FIELD_LAST_AUTHZ, lastAuthz);
        values.put(AuthzDatabaseHelper.FIELD_AUTHZ,      authz);
        values.put(AuthzDatabaseHelper.FIELD_REMEMBER,   remember);
        return values;
    }
    public boolean checkSignatures(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        String[] packages = pm.getPackagesForUid(uid);
        Arrays.sort(packages);
        SpannableStringBuilder b = new SpannableStringBuilder();
        StringBuilder allSignatures = new StringBuilder();
        for (String pkg : packages) {
            try {
                PackageInfo pInfo = pm.getPackageInfo(
                        pkg, PackageManager.GET_SIGNATURES);
                ApplicationInfo appInfo = pm.getApplicationInfo(pkg, 0);
                CharSequence label = pm.getApplicationLabel(appInfo);
                for (Signature sig : pInfo.signatures) {
                    allSignatures.append(sig.toCharsString());
                    allSignatures.append(":");
                }
                b.append(label);
                b.append("\n");
            } catch (PackageManager.NameNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
        appNames = b;
        String old = signatures;
        signatures = allSignatures.toString();
        return signatures.equals(old);
    }
}
