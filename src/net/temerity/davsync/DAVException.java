package net.temerity.davsync;

import java.lang.Exception;
import java.lang.Throwable;

public class DAVException extends Exception {

	private static final long serialVersionUID = -4879636486257695816L;
	private String error = "unknown error";

	public DAVException() {
		super();
	}

	public DAVException(String msg) {
		super(msg);
		error = msg;
	}

	public DAVException(String msg, Throwable cause) {
		super(msg, cause);
		error = msg;
	}

	public DAVException(Throwable cause) {
		super(cause);
	}

	public String toString() {
		return new String(error);
	}
}
