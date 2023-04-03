package com.iscas.crashtracker.utils;

import soot.Type;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

	public static String filterRegex(String str) {
		str = str.replace("[\\s\\S]*","!@#any#@!");
		str = str.replace("(","\\(").replace(")","\\)")
				.replace("[","\\[").replace("]","\\]")
				.replace("{","\\{").replace("}","\\}");
		str = str.replace("!@#any#@!","[\\s\\S]*");
		return str;
	}
	public static String getPkgPrefix(String pkg, int num) {
		String[] ss = pkg.split("\\.");
		if(ss.length < num) return  pkg;

		String prefix = "";
		for(int i=0; i<num;i++){
			prefix += ss[i]+".";
		}
		return  prefix;
	}


	/**
	 * refine cls name
	 * 
	 * @param str
	 * @return
	 */
	public static String getclsName(String str) {
		String clsName = str.replace("class \"L", "");
		clsName = clsName.replace(";\"", "");
		clsName = clsName.replace("/", ".");
		return clsName;
	}

	/**
	 * refineString
	 * 
	 * @param old
	 * @return
	 */
	public static String refineString(String old) {
		if (old == null || old.equals("\"\""))
			return "null";
		String newStr = old.replace("\\", "").replace("\"", "");
		return newStr;
	}

	/**
	 * judge whther a string is integer
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isInteger(String str) {
		if (str == null || str.equals(""))
			return false;
		Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
		return pattern.matcher(str).matches();
	}

	public static String getProjectName(String str) {
		String regEx = "[^a-z|^A-Z|^0-9|^_]";
		Pattern p = Pattern.compile(regEx);
		Matcher n = p.matcher(str);
		return n.replaceAll("_");
	}
	public static boolean isStringType(Type type){
		return type.toString().equals("java.lang.String");
	}


}
