package com.iscas.crashtracker.model.component;

public class Data {
	private String scheme = "";
	private String authority = "";
	private String path = "";
	private String mime_type = "";

	@Override
	public String toString() {
		return scheme + authority + path + mime_type;
	}

	/**
	 * @return the scheme
	 */
	public String getScheme() {
		return scheme;
	}

	/**
	 * @param scheme the scheme to set
	 */
	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	/**
	 * @return the port
	 */
	public String getAuthority() {
		return authority;
	}

	/**
	 * @param port the port to set
	 */
	public void setAuthority(String authority) {
		this.authority = authority;
	}

	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @param path the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * @return the mime_type
	 */
	public String getMime_type() {
		return mime_type;
	}

	/**
	 * @param mime_type the mime_type to set
	 */
	public void setMime_type(String mime_type) {
		this.mime_type = mime_type;
	}
}
