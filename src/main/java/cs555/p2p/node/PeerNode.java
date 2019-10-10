package cs555.p2p.node;

import cs555.p2p.messaging.*;
import cs555.p2p.node.util.NodeRouting;
import cs555.p2p.transport.TCPSender;
import cs555.p2p.transport.TCPServer;
import cs555.p2p.util.IDUtils;
import cs555.p2p.util.PeerTriplet;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.UnexpectedException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
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

	private final String discoveryHost;
	private final int discoveryPort;
	private int port;
	private String hostname;
	private String identifier;
	private String nickname;

	public NodeRouting routing;
	private final ConcurrentHashMap<String, TCPSender> senders;

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
		this.senders = new ConcurrentHashMap<>();
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
	}

	private void init() {
		TCPServer tcpServer = new TCPServer(port, this);
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
				LOGGER.info(String.format("%s successfully entered the network with ID: %s", nickname, identifier));
				routing.addToLeafSet(new PeerTriplet(hostname, port, identifier));
				routing.printRoutingTable();
				routing.printLeafSet();
			}else {
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
	}

	private void handleEntryAcceptance(EntryAcceptanceResponse response) {
		LOGGER.info(String.format("%s successfully entered the network with ID: %s", nickname, identifier));
		routing.overwriteRoutingTable(response.getTableRows(), response.getRightLeafSet(), response.getLeftLeafSet());

		printRoute(response.getRoute());
		routing.printRoutingTable();
		routing.printLeafSet();

		sendInfoToPeers();

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



			EntryAcceptanceResponse response = new EntryAcceptanceResponse(neighborLeftSet, neighborRightSet, request.getTableRows(), request.getRouteTrace());
			sender.sendData(response.getBytes());
			sender.flush();
			senders.put(request.getDestinationId(), sender);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void forwardEntryRequest(EntryRequest request, PeerTriplet dest, int rowIndex) {
		request.setForwardPort(this.port);

		sendEvent(dest.identifier, dest.host, dest.port, request);
	}

	private void sendEvent(String identifier, String host, int port, Event event) {
		try {
			senders.computeIfAbsent(identifier, id -> {
				try {
					return new TCPSender(new Socket(host, port));
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}).sendData(event.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
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

		request.addRouteID(identifier);
		int colIndex = Integer.parseInt(request.getDestinationId().substring(rowIndex, rowIndex+1), 16);
		LOGGER.info(String.format("Received entry request from %s:%d with id %s and hop count: %d", request.getHost(),
				request.getPort(), request.getDestinationId(), request.getHopCount()));
		PeerTriplet rowColEntry = routing.findRoutingDest(rowIndex, colIndex, request.getDestinationId());

		routing.aquireTable();
		for(int row = rowIndex; row >= 0; row--) {
			if (routing.getRoutingRow(row) != null) {
				request.setTableRow(row, Arrays.copyOf(routing.getRoutingRow(row), 16));
			}
		}
		routing.releaseTable();

		request.setTableEntryIfEmpty(rowIndex, Integer.parseInt(identifier.substring(rowIndex, rowIndex+1),16), new PeerTriplet(hostname, port, identifier));
		if(rowColEntry == null || rowColEntry.identifier.equals(request.getDestinationId())) {
			LOGGER.info(String.format("Returning entry request to %s:%d with id: %s", request.getHost(), request.getPort(), request.getDestinationId()));
			returnToEnteringNode(request);
		}else {
			LOGGER.info(String.format("Forwarding entry request to %s:%d with id: %s", rowColEntry.host, rowColEntry.port, rowColEntry.identifier));

			forwardEntryRequest(request, rowColEntry, rowIndex);
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
			default:
				LOGGER.warning("No actions found for message of type: " + event.getType());
				break;
		}
	}

	private void handleExitRequest(ExitRequest request) {
		routing.aquireLeafSet();
		LOGGER.info("EXIT REQUEST: LEFT: " + request.getNewLeft() + " RIGHT: " + request.getNewRight());
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

	private void exitGracefully() {
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
							case "all":
								routing.printRoutingTable();
								routing.printLeafSet();
								break;
							case "traversal":
								traverseNetwork();
								break;
							default:
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
				PeerNode peerNode;
				if(args.length == 3) {
					peerNode = new PeerNode(args[0], discoveryPort, args[2]);
				}else if(args.length == 4) {
					peerNode = new PeerNode(args[0], discoveryPort, args[3], args[2]);
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
