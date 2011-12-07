package net.temerity.davsync;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class DAVProfile {
	private String filename, username, password, hostname, resource;
	
	public DAVProfile(String filename, String hostname, String resource, String username, String password) {
		this.filename = new String(filename);
		this.username = new String(username);
		this.password = new String(password);
		this.hostname = new String(hostname);
		this.resource = new String(resource);
	}

	public String getFilename() {
		return new String(filename);
	}
	
	public String getHostname() {
		return new String(hostname);
	}

	public String getResource() {
		return new String(resource);
	}

	public String getUsername() {
		return new String(username);
	}

	public String getPassword() {
		return new String(password);
	}
}

