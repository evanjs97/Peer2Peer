package cs555.p2p.node;

import cs555.p2p.messaging.*;
import cs555.p2p.node.util.FileHandler;
import cs555.p2p.node.util.NodeRouting;
import cs555.p2p.transport.TCPSender;
import cs555.p2p.transport.TCPServer;
import cs555.p2p.util.IDUtils;
import cs555.p2p.util.PeerTriplet;
import cs555.p2p.util.Utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.UnexpectedException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

//Questions about entry protocol
//What if message routes get to new node before many other nodes (sparse routing table)
//What if the randomly chosen node is new node's nearest neighbor (no routing table for new node?)
public class PeerNode implements Node{
	private final static Logger LOGGER = Logger.getLogger(PeerNode.class.getName());
	private final static int IDENTITIFER_BITS = 16;
	private final static int LEAF_SET_SIZE = 1;

	private TCPServer tcpServer;
	private final FileHandler fileHandler;
	private final String discoveryHost;
	private final int discoveryPort;
	private int port;
	private String hostname;
	private String identifier;
	private String nickname;

	private NodeRouting routing;
//	private final ConcurrentHashMap<String, TCPSender> senders;

	private PeerNode(String discoveryHost, int discoveryPort) {
		this(discoveryHost, discoveryPort, null, 0, null);
	}

	private PeerNode(String discoveryHost, int discoveryPort, String nickname) {
		this(discoveryHost, discoveryPort, null, 0, nickname);
	}

	private PeerNode(String discoveryHost, int discoveryPort, String identifier, String nickname) {
		this(discoveryHost, discoveryPort, identifier,0, nickname);
	}

	private PeerNode(String discoveryHost, int discoveryPort, String identifier, int port, String nickname) {
		this.discoveryHost = discoveryHost;
		this.discoveryPort = discoveryPort;
//		this.senders = new ConcurrentHashMap<>();
		this.port = port;
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
		this.identifier = identifier;
		this.nickname = nickname == null ? this.hostname : nickname;
		this.fileHandler = new FileHandler();
	}

	private void init() {
		this.tcpServer = new TCPServer(port, this);
		this.port = tcpServer.getLocalPort();
		Thread serverThread = new Thread(tcpServer);
		serverThread.start();
		register();
		LOGGER.info(String.format("%s: Starting up at address %s:%d with identifier %s", nickname, hostname, port, identifier));
	}

	private void generateID() {
		this.identifier = IDUtils.generateIDByTimestamp(IDUtils.ID_SIZE.ID_SHORT);
	}

	private void register() {
		if(identifier == null) generateID();
		System.out.println(identifier);
		try {
			TCPSender sender = new TCPSender(new Socket(this.discoveryHost, this.discoveryPort));
			sender.sendData(new RegisterRequest(identifier, port).getBytes());
			sender.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Request entry into the p2p system
	 * @param response the RegistrationSuccess message received from the server
	 */
	private void p2pEntry(RegistrationSuccess response) {
		try {
			routing = new NodeRouting(IDENTITIFER_BITS/4, 16, identifier);
			if(response.getEntryHost().isEmpty() && response.getEntryPort() == 0) {
				LOGGER.info("No other peers, no hops needed");
				LOGGER.info(String.format("%s successfully entered the network with ID: %s", nickname, identifier));
				routing.printRoutingTable();
				routing.addToLeafSet(new PeerTriplet(hostname, port, identifier));
			}else {
				LOGGER.info(String.format("Sending Entry Request to %s:%d", response.getEntryHost(), response.getEntryPort()));
				TCPSender sender = new TCPSender(new Socket(response.getEntryHost(), response.getEntryPort()));
				sender.sendData(new EntryRequest(this.hostname, this.port, identifier, IDENTITIFER_BITS / 4, this.port).getBytes());
				sender.close();
			}
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

	private void sendInfoToPeers() {
		routing.aquireTable();

		EntranceBroadcast broadcast = new EntranceBroadcast(new PeerTriplet(hostname, port, identifier));
		for(PeerTriplet[] row : routing.getRoutingTable()) {
			if(row != null) {
				for(PeerTriplet peer : row) {
					if(peer != null) {
						sendEvent(peer.identifier, peer.host, peer.port, broadcast);
					}
				}
			}
		}
		routing.releaseTable();
		sendMessageToLeafNodes(broadcast);
	}

	private void handleEntranceBroadCast(EntranceBroadcast broadcast) {
		int firstNonMatching = IDUtils.firstNonMatchingIndex(broadcast.getPeer().identifier, identifier);
		routing.addEntryToRow(broadcast.getPeer(), firstNonMatching);
		routing.addToLeafSet(broadcast.getPeer());
		sendFilesToNewNode(broadcast.getPeer());
	}

	private void handleEntryAcceptance(EntryAcceptanceResponse response) {
		LOGGER.info(String.format("%s successfully entered the network with ID: %s in %d hops", nickname, identifier, response.getHops()));
		routing.overwriteRoutingTable(response.getTableRows(), response.getRightLeafSet(), response.getLeftLeafSet());

		response.getRoute().add(identifier);
		printRoute(response.getRoute());
		routing.printRoutingTable();
		routing.printLeafSet();

		sendInfoToPeers();
	}

	private void sendFilesToNewNode(PeerTriplet peer) {
		List<String> deletedFiles = new ArrayList<>();
		for(Map.Entry<String, String> entry : fileHandler.getFileSet()) {
			int myDist = Math.min(IDUtils.rightDistance(identifier, entry.getValue()), IDUtils.leftDistance(identifier, entry.getValue()));
			int peerDist = Math.min(IDUtils.rightDistance(peer.identifier, entry.getValue()), IDUtils.leftDistance(peer.identifier, entry.getValue()));
			if(peerDist < myDist || (peerDist == myDist && peer.identifier.compareTo(identifier) > 0)) {
				byte[] fileBytes = Utils.getFileByteArr(entry.getKey());
				String folder =  entry.getKey().substring(entry.getKey().lastIndexOf("/tmp/evanjs/")+12, entry.getKey().lastIndexOf('/')+1);
				String filename = entry.getKey().substring(entry.getKey().lastIndexOf('/')+1);

				StoreRequest request = new StoreRequest(entry.getValue(), fileBytes, folder, filename, hostname, port, false);
				sendEvent(peer.identifier, peer.host, peer.port, request);
				LOGGER.info("Migrating file " + filename + " to new neighbor with closer ID: " + peer.identifier);
				deletedFiles.add(entry.getKey());
			}
		}
		for(String file : deletedFiles) {
			fileHandler.removeFile(file);
		}
	}

	private void printStoredFiles() {
		LOGGER.info(fileHandler.toString());
	}

	private void printRoute(List<String> route) {
		LOGGER.info("\nRoute: "+String.join(" ---> ", route));
	}

	private void returnToEnteringNode(EntryRequest request) {
		try {
			TCPSender sender = new TCPSender(new Socket(request.getHost(), request.getPort()));
			PeerTriplet[] neighborRightSet = new PeerTriplet[LEAF_SET_SIZE];
			PeerTriplet[] neighborLeftSet = new PeerTriplet[LEAF_SET_SIZE];
			routing.aquireLeafSet();
			System.arraycopy(routing.getRightLeafset(), 0, neighborRightSet, 0, neighborRightSet.length);
			System.arraycopy(routing.getLeftLeafSet(), 0, neighborLeftSet, 0, neighborLeftSet.length);
			if(IDUtils.leftDistance(identifier, request.getDestinationId()) <= IDUtils.rightDistance(identifier, request.getDestinationId())) {
				neighborRightSet[0] = new PeerTriplet(hostname, port, identifier);
			}else neighborLeftSet[0] = new PeerTriplet(hostname, port, identifier);
//			routing.addToLeafSet(new PeerTriplet(request.getHost(), request.getPort(), request.getDestinationId()));
			routing.releaseLeafSet();



			EntryAcceptanceResponse response = new EntryAcceptanceResponse(neighborLeftSet, neighborRightSet, request.getTableRows(), request.getRouteTrace(), request.getHopCount());
			sender.sendData(response.getBytes());
			sender.close();
//			senders.put(request.getDestinationId(), sender);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean forwardRequest(Event request, PeerTriplet dest) {

		boolean success = sendEvent(dest.identifier, dest.host, dest.port, request);
		if(!success) {
			LOGGER.info("Found dead peer in routing table with ID: " + dest.identifier + " pruning...");
			routing.removeEntry(dest.identifier);
			return false;
		}else return true;

	}

	private boolean sendEvent(String identifier, String host, int port, Event event) {
		try {
			TCPSender sender = new TCPSender(new Socket(host, port));
			sender.sendData(event.getBytes());
			sender.close();
//			senders.computeIfAbsent(identifier, id -> {
//				try {
//					return new TCPSender(new Socket(host, port));
//				} catch (IOException e) {
//					return null;
//				}
//			}).sendData(event.getBytes());

		} catch (Exception e) {
//			senders.remove(identifier);
			return false;
//			e.printStackTrace();
		}
		return true;
	}

	/**
	 * Handles incoming peer entry request and routes entry request to next node in path to destination node (where entry request originated)
	 * In the case that the current node is the closest node route message back to originating node (destination node)
	 * @param request the incoming entry request
	 * @throws UnexpectedException if ID's match
	 */
	private void handlePeerEntryRequest(EntryRequest request, String originHost) throws UnexpectedException {
		int rowIndex = IDUtils.firstNonMatchingIndex(identifier, request.getDestinationId());
		if(rowIndex >= IDENTITIFER_BITS/4)
			throw new UnexpectedException("ID of target matches current ID: Node ID's must be unique");
		request.incrementHopCount();
		request.addRouteID(identifier);
		int colIndex = Integer.parseInt(request.getDestinationId().substring(rowIndex, rowIndex+1), 16);
		PeerTriplet rowColEntry = routing.findClosestPeer(request.getDestinationId());

		LOGGER.info(String.format("Received entry request from %s:%d with id %s and hop count: %d", request.getHost(), request.getPort(), request.getDestinationId(), request.getHopCount()));
		routing.aquireTable();
		for(int row = rowIndex; row >= 0; row--) {
			if (routing.getRoutingRow(row) != null) {
				request.setTableRow(row, Arrays.copyOf(routing.getRoutingRow(row), 16));
			}
		}
		routing.releaseTable();

		request.setTableEntryIfEmpty(rowIndex, Integer.parseInt(identifier.substring(rowIndex, rowIndex+1),16), new PeerTriplet(hostname, port, identifier));
		if(rowColEntry == null || rowColEntry.identifier.equals(request.getDestinationId())) {
			LOGGER.info(String.format("Returning entry request to origin %s:%d with id: %s", request.getHost(), request.getPort(), request.getDestinationId()));

			returnToEnteringNode(request);
		}else {
//			LOGGER.info(String.format("Forwarding entry request to %s:%d with id: %s", rowColEntry.host, rowColEntry.port, rowColEntry.identifier));
			request.setForwardPort(this.port);
			LOGGER.info("Forwarding entry request to peer with ID: " + rowColEntry.identifier);
			boolean success = forwardRequest(request, rowColEntry);
			while(!success) {
				rowColEntry = routing.findClosestPeer(request.getDestinationId());
				if(rowColEntry == null) {
					returnToEnteringNode(request);
					return;
				}else success = forwardRequest(request, rowColEntry);
			}

		}


	}

	@Override
	public void onEvent(Event event, Socket socket) {
		switch (event.getType()) {
			case REGISTRATION_SUCCESS:
				p2pEntry((RegistrationSuccess) event);
				break;
			case ID_NOT_AVAILABLE:
				LOGGER.info(String.format("Collision using ID: %s, entering with new ID", identifier));
				generateID();
				register();
				break;
			case ENTRY_REQUEST:
				try {

					handlePeerEntryRequest((EntryRequest) event, socket.getInetAddress().getCanonicalHostName());
				} catch (UnexpectedException e) {
					e.printStackTrace();
				}
				break;
			case ENTRY_ACCEPTANCE_RESPONSE:
				this.handleEntryAcceptance((EntryAcceptanceResponse) event);
				break;
			case ENTRANCE_BROADCAST:
				this.handleEntranceBroadCast((EntranceBroadcast) event);
				break;
			case TRAVERSE_REQUEST:
				handleTraversal((TraverseRequest) event);
				break;
			case EXIT_REQUEST:
				handleExitRequest((ExitRequest) event);
				break;
			case EXIT_SUCCESS_RESPONSE:
				exitGracefully();
				break;
			case STORE_REQUEST:
				handleStoreRequest((StoreRequest) event);
				break;
			case FILE_DOWNLOAD_REQUEST:
				handleFileDownloadRequest((FileDownloadRequest) event);
				break;
			case STORE_RESPONSE:
				StoreResponse response = (StoreResponse) event;
				if(response.wasSuccess())
					LOGGER.info("Successfully forwarded file to new node");
				else LOGGER.warning("Unable to forward file to new node");
				break;
			default:
				LOGGER.warning("No actions found for message of type: " + event.getType());
				break;
		}
	}

	private void handleFileDownloadRequest(FileDownloadRequest request) {
		request.updateRoute(identifier);
		int row = IDUtils.firstNonMatchingIndex(identifier, request.getIdentifier());
		int col = Integer.parseInt(request.getIdentifier().substring(row, row+1),16);
		PeerTriplet routingDest = routing.findDataRoute(request.getIdentifier());
		request.incrementHops();
		if(routingDest == null) {
			LOGGER.info("Handling file download request with ID: " + request.getIdentifier());
			retrieveFile(request);
		}else {
			LOGGER.info("Forwarding file download request with ID: " + request.getIdentifier());
			boolean success = forwardRequest(request, routingDest);
			while(!success) {
				routingDest = routing.findDataRoute(request.getIdentifier());
				if(routingDest == null) {
					retrieveFile(request);
					return;
				}else success = forwardRequest(request, routingDest);
			}
			LOGGER.info("Hop Count: " + request.getHops() + " Forwarded file download request to peer with ID: " + routingDest.identifier);
		}
	}

	private void retrieveFile(FileDownloadRequest request) {
		byte[] fileBytes = fileHandler.readFile(request.getFilename());
		FileDownloadResponse response = new FileDownloadResponse(fileBytes, request.getRoute(), request.getHops());
		try {
			TCPSender sender = new TCPSender(new Socket(request.getHostname(), request.getPort()));
			sender.sendData(response.getBytes());
			sender.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void storeFile(StoreRequest request) {
		boolean success = fileHandler.storeFile(request.getFilename(), request.getDestination(), request.getFileBytes(), request.getIdentifier());
		if(success) {
			LOGGER.info("Successfully stored file with name: " + request.getFilename() + " and ID: " + request.getIdentifier() + " in " + request.getHops() + " hops");
//			printRoute(request.getRoute());
		}
		StoreResponse response = new StoreResponse(success, request.getRoute(), request.getHops());
		if(request.getRequestResponse()) {
			try {
				TCPSender sender = new TCPSender(new Socket(request.getStoreDataHost(), request.getStoreDataPort()));
				sender.sendData(response.getBytes());
				sender.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void handleStoreRequest(StoreRequest request) {
		request.updateRoute(identifier);
		int row = IDUtils.firstNonMatchingIndex(identifier, request.getIdentifier());
		int col = Integer.parseInt(request.getIdentifier().substring(row, row+1),16);
		PeerTriplet routingDest = routing.findDataRoute(request.getIdentifier());
		request.incrementHops();
		if(routingDest == null) {
			storeFile(request);
		}else {
			boolean success = forwardRequest(request, routingDest);

			while(!success) {
				routingDest = routing.findDataRoute(request.getIdentifier());
				if(routingDest == null) {
					storeFile(request);
					return;
				}
				else success = forwardRequest(request, routingDest);
			}
			LOGGER.info("Hop Count: " + request.getHops() + " Forwarded store request to peer with ID: " + routingDest.identifier);
		}


	}

	private void handleExitRequest(ExitRequest request) {
		routing.aquireLeafSet();
		LOGGER.info("Received EXIT request from peer with ID: " + request.getIdentifier());
		if(request.getNewRight() != null) routing.getRightLeafset()[request.getRightIndex()] = request.getNewRight();
		if(request.getNewLeft() != null) routing.getLeftLeafSet()[request.getLeftIndex()] = request.getNewLeft();
		routing.releaseLeafSet();
	}

	private void handleTraversal(TraverseRequest request) {
		request.addPeer(identifier);
		if(request.getPeers().get(0).equals(identifier)) {
			LOGGER.info("\nTraverse Route: "+String.join(" ---> ", request.getPeers()));
		}else sendTraversal(request);
	}

	private void sendTraversal(TraverseRequest request) {
		routing.aquireLeafSet();
		PeerTriplet left = routing.getLeftNeighbor();
		sendEvent(left.identifier, left.host, left.port, request);
		routing.releaseLeafSet();
	}

	private void traverseNetwork() {
		TraverseRequest request = new TraverseRequest(identifier);
		sendTraversal(request);
	}

	private void sendMessageToLeafNodes(Event event) {
		routing.aquireLeafSet();
		for(PeerTriplet peer : routing.getLeftLeafSet()) {
			sendEvent(peer.identifier, peer.host, peer.port, event);
		}
		for(PeerTriplet peer : routing.getRightLeafset()) {
			sendEvent(peer.identifier, peer.host, peer.port, event);
		}
		routing.releaseLeafSet();
	}

	private void sendFilesToNearestNeighbors() {
		routing.aquireLeafSet();
		PeerTriplet left = routing.getLeftNeighbor();
		PeerTriplet right = routing.getRightNeighbor();
		for(Map.Entry<String, String> entry : fileHandler.getFileSet()) {

			byte[] fileBytes= Utils.getFileByteArr(entry.getKey());
			int leftDist = IDUtils.rightDistance(left.identifier, entry.getValue());
			int rightDist = IDUtils.leftDistance(right.identifier, entry.getValue());
			String folder =  entry.getKey().substring(entry.getKey().lastIndexOf("/tmp/evanjs/")+12, entry.getKey().lastIndexOf('/')+1);
			String filename = entry.getKey().substring(entry.getKey().lastIndexOf('/')+1);

			LOGGER.info("File FOlder:" + folder + " Filename: " + filename);
			StoreRequest request = new StoreRequest(entry.getValue(), fileBytes, folder, filename, hostname, port, false);
			if(leftDist <= rightDist) {
				LOGGER.info("Sending file to neighbor: " + entry.getKey() + " with id: " + left.identifier);
				sendEvent(left.identifier, left.host, left.port, request);
			}else {
				LOGGER.info("Sending file to neighbor: " + entry.getKey() + " with id: " + right.identifier);
				sendEvent(right.identifier, right.host, right.port, request);
			}
		}
		routing.releaseLeafSet();
	}

	private void exitGracefully() {
		tcpServer.stop();
		routing.aquireLeafSet();
		for(int i = 0; i < LEAF_SET_SIZE; i++) {
			PeerTriplet left = routing.getLeftLeafSet()[i];
			PeerTriplet right = routing.getRightLeafset()[i];

			ExitRequest leftExit = new ExitRequest(identifier, null, right, 0, i, port);
			sendEvent(left.identifier, left.host, left.port, leftExit);

			ExitRequest rightExit = new ExitRequest(identifier, left, null, i, 0, port);
			sendEvent(right.identifier, right.host, right.port, rightExit);
		}
		routing.releaseLeafSet();

		sendFilesToNearestNeighbors();
		LOGGER.info(String.format("%s exited the network", nickname));

		System.exit(0);
	}

	private void exitNetwork() {
		try {
			TCPSender sender = new TCPSender(new Socket(discoveryHost, discoveryPort));
			ExitRequest exitRequest = new ExitRequest(identifier, null, null, 0, 0, port);
			sender.sendData(exitRequest.getBytes());
			sender.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void inputHandler() {
		Scanner scan = new Scanner(System.in);
		while(true) {
			while (scan.hasNextLine()) {
				String[] input = scan.nextLine().split("\\s+");
				switch (input[0]) {
					case "print":
						if(input.length < 2) {
							LOGGER.warning("Usage: print [leafset|table|all|]");
							break;
						}
						switch(input[1]) {
							case "leafset":

								routing.printLeafSet();
								break;
							case "table":
								routing.printRoutingTable();
								break;
							case "routing":
								routing.printRoutingTable();
								routing.printLeafSet();
								break;
							case "traversal":
								traverseNetwork();
								break;
							case "files":
							default:
								printStoredFiles();
								break;
						}
						break;
					case "exit":
						exitNetwork();
						break;
				}
			}
		}
	}

	public static void main(String[] args) {
		LOGGER.setLevel(Level.INFO);
		if(args.length < 2) {
			LOGGER.severe("Peer Node requires at least 2 arguments []: String:[discoveryServer] int:[port]");
			System.exit(1);
		}else {
			try {
				LOGGER.info(Arrays.toString(args));
				int discoveryPort = Integer.parseInt(args[1]);
				if(discoveryPort > 65535 || discoveryPort < 1024) throw new NumberFormatException();
				PeerNode peerNode = null;
				if(args.length == 3) {
					if(args[2].startsWith("name=")) {
						peerNode = new PeerNode(args[0], discoveryPort, args[2].substring(5));
					}else if(args[2].startsWith("id=")) {
						peerNode = new PeerNode(args[0], discoveryPort, args[2].substring(3), args[2].substring(3));
					}else {
						LOGGER.severe("Usage String:[discoveryServer] int:[port] options: [id=String] [name=String]");
						System.exit(1);
					}
				}else if(args.length == 4) {
					if(args[2].startsWith("name=") && args[3].startsWith("id=")) {
						peerNode = new PeerNode(args[0], discoveryPort, args[3].substring(3), args[2].substring(5));
					}else if(args[3].startsWith("name=") && args[2].startsWith("id=")) {
						peerNode = new PeerNode(args[0], discoveryPort, args[2].substring(3), args[3].substring(5));
					}else {
						LOGGER.severe("Usage String:[discoveryServer] int:[port] options: [id=String] [name=String]");
						System.exit(1);
					}
				}else {
					peerNode = new PeerNode(args[0], discoveryPort);
				}
				peerNode.init();
				peerNode.inputHandler();

			}catch(NumberFormatException nfe) {
				nfe.printStackTrace();
				LOGGER.severe("Discovery Node requires a valid integer port: 1024 < [port] < 65536");
			}
		}
	}
}
