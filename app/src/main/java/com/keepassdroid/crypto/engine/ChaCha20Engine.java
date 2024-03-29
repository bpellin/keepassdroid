/*
 * Copyright 2017-2022 Brian Pellin.
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
package com.keepassdroid.crypto.engine;

import com.keepassdroid.utils.Types;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class ChaCha20Engine extends CipherEngine {
    public static final UUID CIPHER_UUID = Types.bytestoUUID(
        new byte[]{(byte)0xD6, (byte)0x03, (byte)0x8A, (byte)0x2B, (byte)0x8B, (byte)0x6F,
                (byte)0x4C, (byte)0xB5, (byte)0xA5, (byte)0x24, (byte)0x33, (byte)0x9A, (byte)0x31,
                (byte)0xDB, (byte)0xB5, (byte)0x9A});

    @Override
    public int ivLength() {
        return 12;
    }

    @Override
    public Cipher getCipher(int opmode, byte[] key, byte[] IV, boolean androidOverride) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance("Chacha7539", new BouncyCastleProvider());
        cipher.init(opmode, new SecretKeySpec(key, "ChaCha7539"), new IvParameterSpec(IV));
        return cipher;
    }
}
