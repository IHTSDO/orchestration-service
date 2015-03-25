package org.ihtsdo.utils;

//TODO When this class is mature, move it to OTF Common
public class DateUtils {
	
	public static final String DATE_SEPARATOR = "-";

	public static String formatAsISO (String dateAsYYYYMMDD) {
		if (dateAsYYYYMMDD == null || dateAsYYYYMMDD.length() != 8) {
			throw new NumberFormatException ("Date '" + dateAsYYYYMMDD + "' cannot be formatted as ISO YYYY-MM-DD");
		}
		StringBuffer buff = new StringBuffer();
		buff.append(dateAsYYYYMMDD.substring(0, 4))
			.append(DATE_SEPARATOR)
			.append(dateAsYYYYMMDD.substring(4, 6))
			.append(DATE_SEPARATOR)
			.append(dateAsYYYYMMDD.substring(6));
		return buff.toString();
	}
}