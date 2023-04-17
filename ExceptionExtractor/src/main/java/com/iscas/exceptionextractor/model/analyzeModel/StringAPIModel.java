package com.iscas.exceptionextractor.model.analyzeModel;

import java.io.Serializable;

public class StringAPIModel implements Serializable {
	private static final long serialVersionUID = 8L;
	// obtain value
	public int toStr = 0;
	public int append = 0;
	public int contact = 0;
	public int valueOf = 0;
	public int copyValueOf = 0;
	public int substring = 0;
	public int toLowerCase = 0;
	public int toUpperCase = 0;
	public int trim = 0;

	// compare value
	public int contains = 0;
	public int equals = 0;
	public int equalsIgnoreCase = 0;
	public int contentEquals = 0;
	public int startsWith = 0;
	public int endsWith = 0;
	public int nullCompare = 0;
	public int isEmpty = 0;

	@Override
	public String toString() {
		String s = "";
		s += "toString = " + toStr + "\n";
		s += "append = " + append + "\n";
		s += "contact = " + contact + "\n";
		s += "valueOf = " + valueOf + "\n";
		s += "copyValueOf = " + copyValueOf + "\n";
		s += "substring = " + substring + "\n";
		s += "toLowerCase = " + toLowerCase + "\n";
		s += "toUpperCase = " + toUpperCase + "\n";
		s += "trim = " + trim + "\n\n";

		s += "contains = " + contains + "\n";
		s += "equals = " + equals + "\n";
		s += "equalsIgnoreCase = " + equalsIgnoreCase + "\n";
		s += "contentEquals = " + contentEquals + "\n";
		s += "startsWith = " + startsWith + "\n";
		s += "endsWith = " + endsWith + "\n";
		s += "nullCompare = " + nullCompare + "\n";
		s += "isEmpty = " + isEmpty + "\n\n";
		return s;
	}
}
