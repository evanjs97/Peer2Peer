package cs555.p2p.node.util;

import cs555.p2p.node.PeerNode;

import java.io.*;
import java.nio.file.Files;
import java.util.Map;
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
		if(file.exists()) file.delete();
		file.mkdirs();
//		if(!success) {
//			LOGGER.severe("Unable to create required directories with path: " + path);
//			return false;
//		}

		File actualFile = new File(path + filename);

		try {
			FileOutputStream outputStream = new FileOutputStream(actualFile);
			outputStream.write(fileBytes);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		fileToID.put(path+filename, identifier);
		return true;
	}


	public byte[] readFile(String filename) {
		LOGGER.info("Reading file: " + filename);
		String separator = filename.charAt(0) == '/' ? "" : "/";
		File file = new File(BASE_DIR+separator+filename);
		try {
			byte[] fileBytes = new byte[(int) file.length()];
			FileInputStream inputStream = new FileInputStream(file);
			inputStream.read(fileBytes);
			return fileBytes;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Stored Files:\n");
		builder.append("  ID    Path");
		for(Map.Entry<String, String> entry : fileToID.entrySet()) {
			builder.append(entry.getValue());
			builder.append('\t');
			builder.append(entry.getKey());
			builder.append('\n');
		}
		return builder.toString();
	}
}
