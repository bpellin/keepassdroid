/*
 * Copyright 2013-2018 Brian Pellin.
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
package com.keepassdroid.database.security;

import android.util.Log;

import com.keepassdroid.crypto.CipherFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class ProtectedBinary {

	private static final String TAG = ProtectedBinary.class.getName();
	public final static ProtectedBinary EMPTY = new ProtectedBinary();

	private byte[] data;
	private boolean protect;
	private File dataFile;
	private int size;
	private static final SecureRandom secureRandom = new SecureRandom();
	private FileParams fileParams;

	private class FileParams {

		private File dataFile;
		public CipherOutputStream cos;
		public SecretKeySpec keySpec;
		public IvParameterSpec ivSpec;


		public Cipher initCipher(int mode) {
		    Cipher cipher;
			try {
				cipher = CipherFactory.getInstance("AES/CBC/PKCS5Padding");
				cipher.init(mode, keySpec, ivSpec);
			} catch (NoSuchAlgorithmException e) {
				throw new IllegalStateException(e);
			} catch (NoSuchPaddingException e) {
				throw new IllegalStateException(e);
			} catch (InvalidKeyException e) {
				throw new IllegalStateException(e);
			} catch (InvalidAlgorithmParameterException e) {
				throw new IllegalStateException(e);
			}

			return cipher;
		}

		public void setupEnc(File file) {

			byte[] iv = new byte[16];
			byte[] key = new byte[32];
			secureRandom.nextBytes(key);
			secureRandom.nextBytes(iv);

			keySpec = new SecretKeySpec(key, "AES");
			ivSpec = new IvParameterSpec((iv));

			Cipher cipherOut = initCipher(Cipher.ENCRYPT_MODE);
			FileOutputStream fos;
			try {
				fos = new FileOutputStream(file);
			} catch (FileNotFoundException e) {
				throw new IllegalStateException(e);
			}

			cos = new CipherOutputStream(fos, cipherOut);
		}

		public FileParams(File dataFile) {
			this.dataFile = dataFile;
			setupEnc(dataFile);
		}
	}
	
	public boolean isProtected() {
		return protect;
	}
	
	public int length() {
		if (data != null)
			return data.length;
		if (dataFile != null)
			return size;
		return 0;
	}
	
	private ProtectedBinary() {
		this.protect = false;
		this.data = null;
		this.dataFile = null;
		this.size = 0;
	}

	public ProtectedBinary(boolean enableProtection, byte[] data) {
		this.protect = enableProtection;
		this.data = data;
		this.dataFile = null;
		if (data != null)
		    this.size = data.length;
		else
		    this.size = 0;
	}

	public ProtectedBinary(boolean enableProtection, File dataFile, int size) {
		this.protect = enableProtection;
		this.data = null;
		this.dataFile = dataFile;
		this.size = size;

		fileParams = new FileParams(dataFile);
	}

	public OutputStream getOutputStream() {
		assert(fileParams != null);
	    return fileParams.cos;
	}

	public InputStream getData() throws IOException {
		if (data != null)
			return new ByteArrayInputStream(data);
		else if (dataFile != null)
			return new CipherInputStream(new FileInputStream(dataFile), fileParams.initCipher(Cipher.DECRYPT_MODE));
		else
			return null;
	}

	public void clear() {
		data = null;
		if (dataFile != null && !dataFile.delete())
			Log.e(TAG, "Unable to delete temp file " + dataFile.getAbsolutePath());
	}
	
	public boolean equals(ProtectedBinary o) {
        return (this == o) || ((o != null)
                && (getClass() == o.getClass())
                && (protect == o.protect)
                && (size == o.size)
                && (Arrays.equals(data, o.data))
                && (dataFile != null)
                && (dataFile.equals(o.dataFile)));
    }
}
