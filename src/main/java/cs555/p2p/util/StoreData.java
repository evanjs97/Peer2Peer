package cs555.p2p.util;

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
		if (args.length < 2) {
			LOGGER.severe("Peer Node requires at least 2 arguments []: String:[discoveryServer] int:[port]");
			System.exit(1);
		} else {
			try {
				int discoveryPort = Integer.parseInt(args[1]);
				if (discoveryPort > 65535 || discoveryPort < 1024) throw new NumberFormatException();
				StoreData storeData = new StoreData(args[0], discoveryPort);
				storeData.inputHandler();
			} catch (NumberFormatException nfe) {
				nfe.printStackTrace();
			}

		}
	}
}
