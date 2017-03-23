package org.ihtsdo.orchestration.rest.util;

public class PathUtil {

	public static String getStringBetween(String path, String before, String after) {
		path = stringAfter(path, after);
		path = stringBefore(path, before);
		return path;
	}

	public static String stringBefore(String path, String str) {
		path = path.substring(0, path.lastIndexOf(str));
		return path;
	}

	public static String stringAfter(String path, String s) {
		path = path.substring(path.lastIndexOf(s) + s.length());
		return path;
	}
}
