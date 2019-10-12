package cs555.p2p.util;

import cs555.p2p.messaging.*;
import cs555.p2p.node.Node;
import cs555.p2p.transport.TCPSender;
import cs555.p2p.transport.TCPServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.DigestException;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataStorage implements Node{
	private final static Logger LOGGER = Logger.getLogger(DataStorage.class.getName());

	private String discoveryHost;
	private int discoveryPort;
	private TCPServer server;
	private int port;
	private String hostname;
	private String filename;
	private String fileDest;

	public DataStorage(String discoverHost, int discoverPort) {
		this.discoveryHost = discoverHost;
		this.discoveryPort = discoverPort;
		try {
			this.hostname = InetAddress.getLocalHost().getCanonicalHostName();
		} catch (UnknownHostException e) {
			try {
				this.hostname = InetAddress.getLocalHost().getHostAddress();
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}

	private void uploadFile(String filename, String dest) {
		this.filename = filename;
		this.fileDest = dest;
		try {
			server = new TCPServer(0, this);
			this.port = server.getLocalPort();
			Thread thread = new Thread(server);
			thread.start();

			PeerRequest peerRequest = new PeerRequest(port);
			TCPSender sender = new TCPSender(new Socket(discoveryHost, discoveryPort));
			sender.sendData(peerRequest.getBytes());
			sender.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void handlePeerResponse(PeerResponse response) {
		if(response.getHostname().equals("") || response.getHostname() == null) {
			server.stop();
			LOGGER.severe("Unable to find p2p node");
			System.exit(1);
		}
		byte[] fileBytes = Utils.getFileByteArr(filename);
		if(fileBytes == null) {
			LOGGER.severe("Unable to read file: " + filename);
			System.exit(1);
		}
		try {
			String identifier = IDUtils.SHAChecksum(fileBytes);
			identifier = identifier.substring(identifier.length()-4);
			StoreRequest storeRequest = new StoreRequest(identifier, fileBytes, fileDest, filename, hostname, port);
			TCPSender sender = new TCPSender(new Socket(response.getHostname(), response.getPort()));
			sender.sendData(storeRequest.getBytes());
			sender.close();
			LOGGER.info("Sending file to peer with : " + response.getHostname() + ":" + response.getPort());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (DigestException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
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

	public void handleStoreResponse(StoreResponse response) {
		if(response.wasSuccess()) {
			LOGGER.info("File was successfully uploaded");
		}else {
			LOGGER.severe("Failed to upload the file");
		}
		System.exit(0);
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
				DataStorage dataStorage = new DataStorage(args[0], discoveryPort);

				switch (args[2]) {
					case "put":
						if(args.length < 5) {
							LOGGER.severe("usage with put command: String:[discoveryServer] int:[port] command:[put] String:[filename] String:[destination]");
							System.exit(1);
						}
						dataStorage.uploadFile(args[3], args[4]);
				}
			} catch (NumberFormatException nfe) {
				nfe.printStackTrace();
			}

		}
	}

	@Override
	public void onEvent(Event event, Socket socket) {
		switch (event.getType()) {
			case PEER_RESPONSE:
				this.handlePeerResponse((PeerResponse) event);
				break;
			case STORE_RESPONSE:
				this.handleStoreResponse((StoreResponse) event);
				break;
		}
	}
}
