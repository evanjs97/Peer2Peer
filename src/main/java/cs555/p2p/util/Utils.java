package cs555.p2p.util;

import java.io.*;
import java.nio.file.Files;
import java.util.logging.Logger;

public class Utils {
	private final static Logger LOGGER = Logger.getLogger(Utils.class.getName());
	public static String formatString(String s, int newLength) {
		if(s.length() >= newLength) return s;
		StringBuilder builder = new StringBuilder(s);
		while(builder.length() < newLength) {
			builder.append(' ');
		}
		return builder.toString();
	}

	public static byte[] getFileByteArr(String filename) {
		File file = new File(filename);
		if(!file.exists()) {
			LOGGER.severe("No file exists with that path: " + filename);
			System.exit(1);
		}
		try {
			byte[] fileBytes = Files.readAllBytes(file.toPath());
			return fileBytes;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
