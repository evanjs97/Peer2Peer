package cs555.p2p.util;

public class Utils {
	public static String formatString(String s, int newLength) {
		if(s.length() >= newLength) return s;
		StringBuilder builder = new StringBuilder(s);
		while(builder.length() < newLength) {
			builder.append(' ');
		}
		return builder.toString();
	}
}
