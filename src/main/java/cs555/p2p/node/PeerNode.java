package cs555.p2p.node;

import cs555.p2p.messaging.*;
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
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
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
//	private int idValue;
	private String nickname;

	private final static int MAX_THREADS = 100;
	Semaphore routingTableLock;
	Semaphore leafSetLock;
	private final PeerTriplet[][] routingTable;
	private final PeerTriplet[] rightLeafset;
	private final PeerTriplet[] leftLeafSet;
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

//	private PeerNode(String discoveryHost, int discoveryPort, String identifier, String nickname, int port) {
//		this(discoveryHost, discoveryPort, identifier,0, nickname);
//	}

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
		this.routingTable = new PeerTriplet[IDENTITIFER_BITS/4][16];
		this.rightLeafset = new PeerTriplet[LEAF_SET_SIZE];
		this.leftLeafSet = new PeerTriplet[LEAF_SET_SIZE];
		this.routingTableLock = new Semaphore(MAX_THREADS);
		this.leafSetLock = new Semaphore(MAX_THREADS);
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
			if(response.getEntryHost().isEmpty() && response.getEntryPort() == 0) {
//				this.idValue = Integer.parseInt(identifier, 16);
				LOGGER.info(String.format("%s successfully entered the network", nickname));
				PeerTriplet triplet = new PeerTriplet(this.hostname, this.port, this.identifier);
				try {
					leafSetLock.acquire(1);
					rightLeafset[0] = triplet;
					leftLeafSet[0] = triplet;
					leafSetLock.release(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}else {
				LOGGER.info(String.format("Sending entry request from id: %s to %s:%d", this.identifier, response.getEntryHost(), response.getEntryPort()));
				TCPSender sender = new TCPSender(new Socket(response.getEntryHost(), response.getEntryPort()));
				sender.sendData(new EntryRequest(this.hostname, this.port, identifier, IDENTITIFER_BITS / 4, this.port).getBytes());
				sender.close();
			}
			LOGGER.info(formatLeafSet());
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

//	private void createRightLeafsetFromNeighbor(PeerTriplet[] neighbor, PeerTriplet right) {
//		System.arraycopy(neighbor, 0, rightLeafset, 1, neighbor.length-1);
//		this.rightLeafset[0] = right;
//	}

	private void sendInfoToPeers() {
		try {
			routingTableLock.acquire(1);
			EntranceBroadcast broadcast = new EntranceBroadcast(new PeerTriplet(hostname, port, identifier));
			for(PeerTriplet[] row : routingTable) {
				if(row != null) {
					for(PeerTriplet peer : row) {
						if(peer != null) {
							sendEvent(peer.identifier, peer.host, peer.port, broadcast);
						}
					}
				}
			}
			routingTableLock.release(1);
			leafSetLock.acquire(1);
			for(PeerTriplet neighbor : rightLeafset) {
				if(neighbor != null) sendEvent(neighbor.identifier, neighbor.host, neighbor.port, broadcast);
			}
			for(PeerTriplet neighbor : leftLeafSet) {
				if(neighbor != null) sendEvent(neighbor.identifier, neighbor.host, neighbor.port, broadcast);
			}
			leafSetLock.release(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	private  boolean addEntryToLeafSet(PeerTriplet peer) {
//		int hexValue = Integer.parseInt(peer.identifier, 16);
//		LOGGER.info(String.format("MYID: %d ENTRY ID: %d", idValue, hexValue));
		boolean add1 = addToLeafSet(leftLeafSet, peer, IDUtils.ID_COMPARE.LEFT);
		boolean add2 = addToLeafSet(rightLeafset, peer, IDUtils.ID_COMPARE.RIGHT);
		return add1 || add2;
	}


	private boolean addToLeafSet(PeerTriplet[] leafSet, PeerTriplet peer, IDUtils.ID_COMPARE comparison) {
		try {
			leafSetLock.acquire(1);
			for (int i = 0; i < leafSet.length; i++) {
				if (leafSet[i] == null || leafSet[i].identifier.equals(identifier) ||
						IDUtils.betterChild(leafSet[i].identifier, peer.identifier, identifier, comparison)) {
					leafSetLock.acquire(99);
					System.arraycopy(leafSet, i, leafSet, i+1, leafSet.length-i-1);
					leafSet[i] = peer;
					leafSetLock.release(100);
					LOGGER.info(formatLeafSet());
					return true;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		leafSetLock.release(1);
		return false;
	}

	private void addEntryToRow(PeerTriplet entry, int row) {
		int col = Integer.parseInt(entry.identifier.substring(row, row+1), 16);
		try {
			routingTableLock.acquire(MAX_THREADS);
			if(routingTable[row] == null) {
				routingTable[row] = new PeerTriplet[16];
			}
			if(routingTable[row][col] == null) {
				routingTable[row][col] = entry;
			}
			routingTableLock.release(MAX_THREADS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		LOGGER.info(formatRoutingTable());
	}

	private void handleEntranceBroadCast(EntranceBroadcast broadcast) {
		int firstNonMatching = IDUtils.firstNonMatchingIndex(broadcast.getPeer().identifier, identifier);
		addEntryToLeafSet(broadcast.getPeer());
		addEntryToRow(broadcast.getPeer(), firstNonMatching);

	}

	private void handleEntryAcceptance(EntryAcceptanceResponse response) {
//		this.idValue = Integer.parseInt(identifier, 16);
		LOGGER.info(String.format("%s successfully entered the network", nickname));

		try {
			routingTableLock.acquire(MAX_THREADS);
			for(int row = 0; row < response.getTableRows().length; row++) {
				this.routingTable[row] = response.getTableRows()[row];
			}
			routingTableLock.release(MAX_THREADS);

			leafSetLock.acquire(MAX_THREADS);
			for(int col = 0; col < LEAF_SET_SIZE; col++) {
				if(response.getLeftLeafSet()[col] != null) {
					this.leftLeafSet[col] = response.getLeftLeafSet()[col];
				}
				if(response.getRightLeafSet()[col] != null) this.rightLeafset[col] = response.getRightLeafSet()[col];
			}
			leafSetLock.release(MAX_THREADS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		LOGGER.info(formatRoutingTable());
		LOGGER.info(formatLeafSet());

		sendInfoToPeers();

	}

	private String formatLeafSet() {
		StringBuilder builder = new StringBuilder();
		builder.append("Left Leaf Set: ");
		builder.append('[');
		try {
			leafSetLock.acquire(1);
			for(PeerTriplet peer : leftLeafSet) {
				if(peer != null) {
					builder.append(peer.identifier);
					builder.append(' ');
				}
			}
			builder.append("]\n");
			builder.append("Right Leaf Set: ");
			builder.append('[');
			for(PeerTriplet peer : rightLeafset) {
				if(peer != null) {
					builder.append(peer.identifier);
					builder.append(' ');
				}
			}
			leafSetLock.release(1);
			builder.append("]\n");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return builder.toString();
	}

	private String formatRoutingTable() {
		StringBuilder builder = new StringBuilder();
		builder.append('\n');
		builder.append(Utils.formatString("",7));
		for(int i = 0; i < 16; i++) {
			builder.append(Utils.formatString("Col " + i, 7));
			builder.append('|');
		}
		builder.append('\n');
		try {
			routingTableLock.acquire(1);
			for(int row = 0; row < routingTable.length; row++) {
				builder.append(Utils.formatString("Row " + row, 7));
				if(routingTable[row] != null) {
					for (PeerTriplet peer : routingTable[row]) {
						if (peer != null)
							builder.append(Utils.formatString(peer.identifier, 7));
						else builder.append(Utils.formatString("",7));
						builder.append('|');
					}
				}else {
					for(int i = 0; i < 16; i++) {
						builder.append(Utils.formatString("", 7));
						builder.append('|');
					}
				}
				builder.append('\n');
			}
			routingTableLock.release(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return builder.toString();
	}

	private void returnToEnteringNode(EntryRequest request) {
		try {
			TCPSender sender = new TCPSender(new Socket(request.getHost(), request.getPort()));
			PeerTriplet[] neighborRightSet = new PeerTriplet[LEAF_SET_SIZE];
			PeerTriplet[] neighborLeftSet = new PeerTriplet[LEAF_SET_SIZE];
			System.arraycopy(rightLeafset, 0, neighborRightSet, 1, neighborRightSet.length-1);
			System.arraycopy(leftLeafSet, 0, neighborLeftSet, 0, neighborLeftSet.length);
			neighborRightSet[0] = new PeerTriplet(hostname, port, identifier);
//
//			try {
			addEntryToLeafSet(new PeerTriplet(request.getHost(), request.getPort(), request.getDestinationId()));
//				leafSetLock.acquire(100);
//				leftLeafSet[0] = new PeerTriplet(request.getHost(), request.getPort(), request.getDestinationId());
//				leafSetLock.release(100);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}


			EntryAcceptanceResponse response = new EntryAcceptanceResponse(neighborLeftSet, neighborRightSet, request.getTableRows());
			sender.sendData(response.getBytes());
			sender.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void forwardEntryRequest(EntryRequest request, PeerTriplet dest, int rowIndex) {
		request.setForwardPort(this.port);
		if(request.getTableRows()[rowIndex] == null && routingTable[rowIndex] != null) {
			try {

				routingTableLock.acquire(1);
				request.getTableRows()[rowIndex] = Arrays.copyOf(routingTable[rowIndex], routingTable[rowIndex].length);
				routingTableLock.release(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

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
		LOGGER.info(String.format("Handling entry request from %s:%d", request.getHost(), request.getPort()));
		LOGGER.info(String.format("Entry request sent from %s:%d", originHost, request.getForwardPort()));
		int rowIndex = IDUtils.firstNonMatchingIndex(identifier, request.getDestinationId());
		if(rowIndex >= IDENTITIFER_BITS/4)
			throw new UnexpectedException("ID of target matches current ID: Node ID's must be unique");


		int colIndex = Integer.parseInt(request.getDestinationId().substring(rowIndex, rowIndex+1), 16);
		PeerTriplet rowColEntry = findValidEntry(rowIndex, colIndex, originHost, request.getForwardPort(), request.getDestinationId());
		if(rowColEntry == null || rowColEntry.identifier.equals(request.getDestinationId())) {
			LOGGER.info(String.format("Returning entry request to %s:%d with id: %s", request.getHost(), request.getPort(), request.getDestinationId()));
			returnToEnteringNode(request);
		}else {
			LOGGER.info(String.format("Forwarding entry request to %s:%d with id: %s", rowColEntry.host, rowColEntry.port, rowColEntry.identifier));
			forwardEntryRequest(request, rowColEntry, rowIndex);
		}


	}

	/**
	 * Find best matching entry in the routing table
	 * start searching at ideal col then get search smaller numbers/cols
	 * Consults leaf set in the case that no closer entry is found in the routing table
	 *
	 * @param row the row in the routing table to start the search at
	 * @param col the column to start search at in the routing table (the base16 number at row index in destination id)
	 * @return the PeerTriplet to route to. Special case, return null if no valid entries in LeafSet
	 * 		i.e. returns null if all entries in leaf set are current node
	 */
	private PeerTriplet findValidEntry(int row, int col, String forwardHost, int forwardPort, String destID) {
		try {
			routingTableLock.acquire(1);
			System.out.println("LOOKING IN COLUMN: " + col);
			if(routingTable[row] != null) {
				for(int i = col; i >= 0; i--) {
					if(routingTable[row][col] != null && (!routingTable[row][col].host.equals(forwardHost)
							|| routingTable[row][col].port != forwardPort)) {
						if(IDUtils.hostIsCloser(routingTable[row][col].identifier, identifier, destID)) {
							LOGGER.info("FOUND FORWARDING HOST: " + routingTable[row][col].identifier);
							routingTableLock.release(1);
							return routingTable[row][col];
						}

					}
				}
			}
			routingTableLock.release(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		int currentIDAtIndex = Integer.parseInt(identifier.substring(row, row+1), 16);
		return findEntryInTreeSet(currentIDAtIndex, col, forwardHost, forwardPort, destID);
	}


	/**
	 * Find valid entry in tree set to route message to
	 * @param myIDNum the first non matching number (base 16) in the current nodes ID
	 * @param destIDNum the first non matching number (base 16) in the destination ID
	 * @return the PeerTriplet to route to. Special case, return null if no valid entries in LeafSet
	 * 		i.e. returns null if all entries in leaf set are current node
	 */
	private PeerTriplet findEntryInTreeSet(int myIDNum, int destIDNum, String forwardHost, int forwardPort, String destID) {
		try {
			leafSetLock.acquire(1);
			if(identifier.compareTo(destID) > 0) {
				for (int i = 0; i < LEAF_SET_SIZE; i++) {
					if (rightLeafset[i] != null && !rightLeafset[i].identifier.equals(identifier)
							&& (!rightLeafset[i].host.equals(forwardHost) || rightLeafset[i].port != forwardPort)
							&& IDUtils.hostIsCloser(rightLeafset[i].identifier, identifier, destID)) {
						leafSetLock.release(1);
						return rightLeafset[i];
					}
				}
			}

			for (int i = LEAF_SET_SIZE - 1; i >= 0; i--) {
				if (leftLeafSet[i] != null && !leftLeafSet[i].identifier.equals(identifier)
						&& (!leftLeafSet[i].host.equals(forwardHost) || leftLeafSet[i].port != forwardPort)
						&& IDUtils.hostIsCloser(rightLeafset[i].identifier, identifier, destID)) {
					leafSetLock.release(1);
					return leftLeafSet[i];
				}
			}

			leafSetLock.release(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return null;
	}


	@Override
	public void onEvent(Event event, Socket socket) {
		switch (event.getType()) {
			case REGISTRATION_SUCCESS:
				p2pEntry((RegistrationSuccess) event);
				break;
			case ID_NOT_AVAILABLE:
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
								LOGGER.info(formatLeafSet());
								break;
							case "table":
								LOGGER.info(formatRoutingTable());
								break;
							case "all":
								LOGGER.info(formatRoutingTable());
								LOGGER.info(formatLeafSet());
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
