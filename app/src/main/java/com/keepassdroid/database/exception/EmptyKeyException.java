package com.keepassdroid.database.exception;

/** Raised when no password or key file is supplied for the master key. */
public class EmptyKeyException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public EmptyKeyException() {
        super("Key cannot be empty.");
    }
}
