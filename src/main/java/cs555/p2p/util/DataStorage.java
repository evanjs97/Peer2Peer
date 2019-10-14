package cs555.p2p.util;

import cs555.p2p.messaging.*;
import cs555.p2p.node.Node;
import cs555.p2p.transport.TCPSender;
import cs555.p2p.transport.TCPServer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.DigestException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
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
	private Command command;

	public enum Command {
		PUT,
		GET
	}

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
		this.command = Command.PUT;
		this.filename = filename;
		this.fileDest = dest;
		sendPeerRequest();
	}

	private void sendPeerRequest() {
		server = new TCPServer(0, this);
		this.port = server.getLocalPort();
		Thread thread = new Thread(server);
		thread.start();
		PeerRequest peerRequest = new PeerRequest(port);
		try {
			TCPSender sender = new TCPSender(new Socket(discoveryHost, discoveryPort));
			sender.sendData(peerRequest.getBytes());
			sender.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void downloadFile(String filename, String dest) {
		this.command = Command.GET;
		this.fileDest = dest;
		this.filename = filename;
		sendPeerRequest();
	}

	public void sendFile(PeerResponse response) {
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
			String temp = filename.contains("/") ? filename.substring(filename.lastIndexOf('/')+1) : filename;
			String dest = fileDest.charAt(fileDest.length()-1) == '/' ? fileDest : fileDest + '/';
			String identifier = IDUtils.SHAChecksum((dest+temp).getBytes());
			identifier = identifier.substring(identifier.length()-4);

			StoreRequest storeRequest = new StoreRequest(identifier, fileBytes, dest, temp, hostname, port);
			TCPSender sender = new TCPSender(new Socket(response.getHostname(), response.getPort()));
			sender.sendData(storeRequest.getBytes());
			sender.close();
			LOGGER.info(String.format("Sending file with ID: %s to peer with address: %s:%d", identifier, response.getHostname(),response.getPort()));
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

	public void sendFileRequest(PeerResponse response) {
		if(response.getHostname().equals("") || response.getHostname() == null) {
			server.stop();
			LOGGER.severe("Unable to find p2p node");
			System.exit(1);
		}
		try {
			String identifier = IDUtils.SHAChecksum(filename.getBytes());
			identifier = identifier.substring(identifier.length()-4);
			LOGGER.info(String.format("Retrieving file with ID: %s to peer with address: %s:%d", identifier, response.getHostname(),response.getPort()));
			TCPSender sender = new TCPSender(new Socket(response.getHostname(), response.getPort()));
			sender.sendData(new FileDownloadRequest(hostname, port, filename, identifier).getBytes());
			sender.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (DigestException e) {
			e.printStackTrace();
		}
	}

	private void printRoute(List<String> route) {
		LOGGER.info("\nRoute: "+String.join(" ---> ", route));
	}

	public void writeFileToDisk(FileDownloadResponse response) {
		response.getRoute().add(0, "Store Program");
		response.getRoute().add("Store Program");
		if(response.getFileBytes() == null) {
			LOGGER.severe("Error during file retrieval");
			printRoute(response.getRoute());
			System.exit(1);
		}
		String temp = filename.contains("/") ? filename.substring(filename.lastIndexOf('/')+1) : filename;
		String separator = fileDest.charAt(fileDest.length()-1) == '/' ? "" : "/";
		File file = new File(fileDest+separator);
		if(file.exists()) file.delete();
		file.mkdirs();

		File actualFile = new File(fileDest+separator+temp);
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(actualFile);
			fileOutputStream.write(response.getFileBytes());
			LOGGER.info("Successfully downloaded file in " + response.getHops() + " hops");
			printRoute(response.getRoute());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		server.stop();
		System.exit(0);
	}


	public void handleStoreResponse(StoreResponse response) {
		if(response.wasSuccess()) {
			LOGGER.info("File was successfully uploaded in " + response.getHops() + " hops");
			response.getRoute().add(0, "Store Program");
			response.getRoute().add("Store Program");
			printRoute(response.getRoute());
		}else {
			LOGGER.severe("Failed to upload the file");
		}
		server.stop();
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
						break;
					case "get":
						if(args.length < 5) {
							LOGGER.severe("usage with put command: String:[discoveryServer] int:[port] command:[get] String:[filename] String:[destination]");
							System.exit(1);
						}
						dataStorage.downloadFile(args[3], args[4]);
						break;
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
				if(this.command == Command.PUT) this.sendFile((PeerResponse) event);
				else if(this.command == Command.GET) this.sendFileRequest((PeerResponse) event);
				break;
			case STORE_RESPONSE:
				this.handleStoreResponse((StoreResponse) event);
				break;
			case FILE_DOWNLOAD_RESPONSE:
				this.writeFileToDisk((FileDownloadResponse) event);
				break;
		}
	}
}
