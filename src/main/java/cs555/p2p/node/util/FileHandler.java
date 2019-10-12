package cs555.p2p.node.util;

import cs555.p2p.node.PeerNode;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class FileHandler {
	private static final Logger LOGGER = Logger.getLogger(FileHandler.class.getName());
	private static final String BASE_DIR = "/tmp/evanjs";
	private final ConcurrentHashMap<String, String> fileToID = new ConcurrentHashMap<>();

	public boolean storeFile(String filename, String destination, byte[] fileBytes, String identifier) {
		filename = filename.contains("/") ? filename.substring(filename.lastIndexOf("/")+1) : filename;
		String separator = destination.charAt(0) == '/' ? "" : "/";
		String end = destination.charAt(destination.length()-1) == '/' ? "" : "/";
		String path = BASE_DIR + separator + destination + end;
		File file = new File(path);
		boolean success = file.mkdirs();
		if(!success) {
			LOGGER.severe("Unable to create required directories with path: " + path);
			return false;
		}

		File actualFile = new File(path + filename);
		try {
			FileOutputStream outputStream = new FileOutputStream(actualFile);
			outputStream.write(fileBytes);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		fileToID.put(path+filename, identifier);
		return true;
	}
}
