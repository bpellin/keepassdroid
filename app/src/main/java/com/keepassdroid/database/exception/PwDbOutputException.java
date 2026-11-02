/*
 * Copyright 2009-2016 Brian Pellin.
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
package com.keepassdroid.database.exception;

public class PwDbOutputException extends Exception {
    public enum Reason {
        IO,
        SERIALIZATION,
        CRYPTO,
        KEY_DERIVATION,
        RANDOM,
        UNKNOWN
    }

    private final Reason reason;

    public PwDbOutputException(String message) {
        this(Reason.UNKNOWN, message, null);
    }

    public PwDbOutputException(String message, Exception cause) {
        this(Reason.UNKNOWN, message, cause);
    }

    public PwDbOutputException(Exception cause) {
        super(cause);
        this.reason = Reason.UNKNOWN;
    }

    public PwDbOutputException(Reason reason, String message) {
        this(reason, message, null);
    }

    public PwDbOutputException(Reason reason, String message, Exception cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    // Keep the summary free of database contents and provider exception messages.
    public String getDiagnosticSummary() {
        Throwable cause = getCause();
        return "Database save failed [" + reason + "]"
                + (cause == null ? "" : " (" + cause.getClass().getName() + ")");
    }

	/**
	 * 
	 */
	private static final long serialVersionUID = 3321212743159473368L;
}
