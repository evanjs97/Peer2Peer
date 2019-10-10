package cs555.p2p.util;

import cs555.p2p.messaging.StoreRequest;
import cs555.p2p.transport.TCPSender;

import java.net.Socket;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StoreData {
	private final static Logger LOGGER = Logger.getLogger(StoreData.class.getName());

	private String discoveryHost;
	private int discoveryPort;

	public StoreData(String discoverHost, int discoverPort) {
		this.discoveryHost = discoverHost;
		this.discoveryPort = discoverPort;
	}

	private void uploadFile(String filename, String dest) {
		byte[] fileBytes = Utils.getFileByteArr(filename);
		if(fileBytes == null) {
			LOGGER.severe("Unable to read file: " + filename);
			System.exit(1);
		}
		StoreRequest storeRequest = new StoreRequest()
		TCPSender sender = new TCPSender(new Socket(discoveryHost, discoveryPort))
	}

	public void inputHandler() {
		Scanner scan = new Scanner(System.in);
		while (true) {
			while (scan.hasNextLine()) {
				String[] input = scan.nextLine().split("\\s+");
				switch (input[0]) {
					case "put":
						uploadFile(input[1], input[2]);
						break;
				}
			}
		}
	}

	public static void main(String[] args) {
		LOGGER.setLevel(Level.INFO);
		if (args.length < 3) {
			LOGGER.severe("Peer Node requires at least 2 arguments []: String:[discoveryServer] int:[port] command:[put]");
			System.exit(1);
		} else {
			try {
				int discoveryPort = Integer.parseInt(args[1]);
				if (discoveryPort > 65535 || discoveryPort < 1024) throw new NumberFormatException();
				StoreData storeData = new StoreData(args[0], discoveryPort);

				switch (args[2]) {
					case "put":
						if(args.length < 5) {
							LOGGER.severe("usage with put command: String:[discoveryServer] int:[port] command:[put] String:[filename] String:[destination]");
							System.exit(1);
						}
						storeData.uploadFile(args[3], args[4]);
				}
			} catch (NumberFormatException nfe) {
				nfe.printStackTrace();
			}

		}
	}
}
