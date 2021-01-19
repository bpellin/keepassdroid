/*
 * Copyright 2017-2021 Brian Pellin.
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
package com.keepassdroid.crypto.keyDerivation;

import com.keepassdroid.utils.Types;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.UUID;

public class Argon2Kdf extends KdfEngine {
    public enum Argon2Type {
        D(0), ID(2);

        private int type;

        Argon2Type(int type) {
            this.type = type;
        }

        int value() {
            return type;
        }
    }

    public static final UUID CIPHER_UUID_D = Types.bytestoUUID(
            new byte[]{(byte) 0xEF, (byte) 0x63, (byte) 0x6D, (byte) 0xDF, (byte) 0x8C, (byte) 0x29, (byte) 0x44, (byte) 0x4B,
                    (byte) 0x91, (byte) 0xF7, (byte) 0xA9, (byte) 0xA4, (byte)0x03, (byte) 0xE3, (byte) 0x0A, (byte) 0x0C
            });

    public static final UUID CIPHER_UUID_ID = Types.bytestoUUID(
            new byte[]{(byte) 0x9E, (byte) 0x29, (byte) 0x8B, (byte) 0x19, (byte) 0x56, (byte) 0xDB, (byte) 0x47, (byte) 0x73,
                    (byte) 0xB2, (byte) 0x3D, (byte) 0xFC, (byte) 0x3E, (byte)0xC6, (byte) 0xF0, (byte) 0xA1, (byte) 0xE6
            });
    public static final String ParamSalt = "S"; // byte[]
    public static final String ParamParallelism = "P"; // UInt32
    public static final String ParamMemory = "M"; // UInt64
    public static final String ParamIterations = "I"; // UInt64
    public static final String ParamVersion = "V"; // UInt32
    public static final String ParamSecretKey = "K"; // byte[]
    public static final String ParamAssocData = "A"; // byte[]

    public static final long MinVersion = 0x10;
    public static final long MaxVersion = 0x13;

    private static final int MinSalt = 8;
    private static final int MaxSalt = Integer.MAX_VALUE;

    private static final long MinIterations = 1;
    private static final long MaxIterations = 4294967295L;

    private static final long MinMemory = 1024 * 8;
    private static final long MaxMemory = Integer.MAX_VALUE;

    private static final int MinParallelism = 1;
    private static final int MaxParallelism = (1 << 24) - 1;

    private static final long DefaultIterations = 2;
    private static final long DefaultMemory = 1024 * 1024;
    private static final long DefaultParallelism = 2;

    private Argon2Type type;

    public Argon2Kdf(Argon2Type type) {
        if (type == Argon2Type.D) {
            uuid = CIPHER_UUID_D;
        } else {
            uuid = CIPHER_UUID_ID;
        }

        this.type = type;
    }

    @Override
    public KdfParameters getDefaultParameters() {
        KdfParameters p = super.getDefaultParameters();

        p.setUInt32(ParamVersion, MaxVersion);
        p.setUInt64(ParamMemory, DefaultMemory);
        p.setUInt32(ParamParallelism, DefaultParallelism);

        return p;
    }

    @Override
    public byte[] transform(byte[] masterKey, KdfParameters p) throws IOException {

        byte[] salt = p.getByteArray(ParamSalt);
        int parallelism = (int)p.getUInt32(ParamParallelism);
        long memory = p.getUInt64(ParamMemory);
        long iterations = p.getUInt64(ParamIterations);
        long version = p.getUInt32(ParamVersion);
        byte[] secretKey = p.getByteArray(ParamSecretKey);
        byte[] assocData = p.getByteArray(ParamAssocData);

        return Argon2Native.transformKey(masterKey, salt, parallelism, memory, iterations,
                secretKey, assocData, version, type.value());
    }

    @Override
    public void randomize(KdfParameters p) {
        SecureRandom random = new SecureRandom();

        byte[] salt = new byte[32];
        random.nextBytes(salt);

        p.setByteArray(ParamSalt, salt);
    }
}
