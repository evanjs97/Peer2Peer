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
//				this.idValue = Integer.parseInt(identifier, 16);
				LOGGER.info(String.format("%s successfully entered the network with ID: %s", nickname, identifier));
				routing.addToLeafSet(new PeerTriplet(hostname, port, identifier));
				routing.printRoutingTable();
				routing.printLeafSet();
			}else {
//				LOGGER.info(String.format("Collision using ID: %s, entering with new ID", identifier));
//				LOGGER.info(String.format("Sending entry request from id: %s to %s:%d", this.identifier, response.getEntryHost(), response.getEntryPort()));
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
		routing.aquireLeafSet();
		for(PeerTriplet neighbor : routing.getRightLeafset()) {
			if(neighbor != null && !neighbor.identifier.equals(identifier)) sendEvent(neighbor.identifier, neighbor.host, neighbor.port, broadcast);
		}
		for(PeerTriplet neighbor : routing.getLeftLeafSet()) {
			if(neighbor != null && !neighbor.identifier.equals(identifier)) sendEvent(neighbor.identifier, neighbor.host, neighbor.port, broadcast);
		}
		routing.releaseLeafSet();
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
			System.arraycopy(routing.getRightLeafset(), 0, neighborRightSet, 1, neighborRightSet.length-1);
			System.arraycopy(routing.getLeftLeafSet(), 0, neighborLeftSet, 0, neighborLeftSet.length);
			neighborRightSet[0] = new PeerTriplet(hostname, port, identifier);
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

		if(routing.getRoutingRow(rowIndex) != null) {
			request.setTableRow(rowIndex, Arrays.copyOf(routing.getRoutingRow(rowIndex), 16));
		}
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
		PeerTriplet rowColEntry = routing.findRoutingDest(rowIndex, colIndex, originHost, request.getForwardPort(), request.getDestinationId());
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
			default:
				LOGGER.warning("No actions found for message of type: " + event.getType());
				break;
		}
	}

	public void inputHandler() {
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
							default:
								break;
						}
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
				int discoveryPort = Integer.parseInt(args[1]);
				if(discoveryPort > 65535 || discoveryPort < 1024) throw new NumberFormatException();
				PeerNode peerNode;
				if(args.length > 2) {
					peerNode = new PeerNode(args[0], discoveryPort, args[2]);
				}else if(args.length > 3) {
					peerNode = new PeerNode(args[0], discoveryPort, args[2], args[3]);
				}else {
					peerNode = new PeerNode(args[0], discoveryPort);
				}
				peerNode.init();
				peerNode.inputHandler();
			}catch(NumberFormatException nfe) {
				LOGGER.severe("Discovery Node requires a valid integer port: 1024 < [port] < 65536");
				System.exit(1);
			}
		}
	}
}
