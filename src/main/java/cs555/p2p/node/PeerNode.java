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
import java.util.Collections;
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
	private String nickname;

	private final static int MAX_THREADS = 100;
	Semaphore routingTableLock;
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
				LOGGER.info(String.format("%s successfully entered the network", nickname));
				PeerTriplet triplet = new PeerTriplet(this.hostname, this.port, this.identifier);
				rightLeafset[0] = triplet;
				leftLeafSet[0] = triplet;
			}else {
				LOGGER.info(String.format("Sending entry request from %s:%d with id %s", this.hostname, this.port, this.identifier));
				TCPSender sender = new TCPSender(new Socket(response.getEntryHost(), response.getEntryPort()));
				sender.sendData(new EntryRequest(this.hostname, this.port, identifier, IDENTITIFER_BITS / 4).getBytes());
				sender.close();
			}
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

	private void handleEntryAcceptance(EntryAcceptanceResponse response) {
		LOGGER.info(String.format("%s successfully entered the network", nickname));
		try {
			routingTableLock.acquire(100);
			for(int row = 0; row < response.getTableRows().length; row++) {
				this.routingTable[row] = response.getTableRows()[row];
			}
			routingTableLock.release(100);

			for(int col = 0; col < LEAF_SET_SIZE; col++) {
				this.leftLeafSet[col] = response.getLeftLeafSet()[col];
				this.rightLeafset[col] = response.getRightLeafSet()[col];
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println(formatRoutingTable());
		System.out.println(formatLeafSet());

	}

	private String formatLeafSet() {
		StringBuilder builder = new StringBuilder();
		builder.append("Left Leaf Set: ");
		builder.append('[');
		for(PeerTriplet peer : leftLeafSet) {
			builder.append(peer.identifier);
			builder.append(' ');
		}
		builder.append("]\n");
		builder.append('[');
		for(PeerTriplet peer : rightLeafset) {
			builder.append(peer.identifier);
			builder.append(' ');
		}
		builder.append("]\n");
		return builder.toString();
	}

	private String formatRoutingTable() {
		StringBuilder builder = new StringBuilder();
		builder.append(Utils.formatString("",6));
		for(int i = 0; i < 16; i++) {
			builder.append(Utils.formatString("Col " + i, 6));
		}
		builder.append('\n');
		for(int row = 0; row < routingTable.length; row++) {
			builder.append(Utils.formatString("Row " + row, 6));
			if(routingTable[row] != null) {
				for (PeerTriplet peer : routingTable[row]) {
					if (peer != null)
						builder.append(Utils.formatString(peer.identifier, 6));
				}
			}
			builder.append('\n');
		}
		return builder.toString();
	}

	private void returnToEnteringNode(EntryRequest request) {
		try {
			TCPSender sender = new TCPSender(new Socket(request.getHost(), request.getPort()));
			EntryAcceptanceResponse response = new EntryAcceptanceResponse(leftLeafSet, rightLeafset, request.getTableRows());
			sender.sendData(response.getBytes());
			sender.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void forwardEntryRequest(EntryRequest request, PeerTriplet dest, int rowIndex) {
		if(request.getTableRows()[rowIndex] == null) {
			try {

				routingTableLock.acquire();
				request.getTableRows()[rowIndex] = Arrays.copyOf(routingTable[rowIndex], routingTable[rowIndex].length);
				routingTableLock.release();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
		try {
			senders.computeIfAbsent(identifier, id -> {
				try {
					return new TCPSender(new Socket(dest.host, dest.port));
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}).sendData(request.getBytes());
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
	private void handlePeerEntryRequest(EntryRequest request) throws UnexpectedException {
		int rowIndex = IDUtils.firstNonMatchingIndex(identifier, request.getDestinationId());
		if(rowIndex >= IDENTITIFER_BITS/4)
			throw new UnexpectedException("ID of target matches current ID: Node ID's must be unique");


		int colIndex = Integer.parseInt(request.getDestinationId().substring(rowIndex, rowIndex+1), 16);
		PeerTriplet rowColEntry = findValidEntry(rowIndex, colIndex);
		if(rowColEntry == null) {
			LOGGER.info(String.format("Returning entry request to %s:%d with id: %s", request.getHost(), request.getPort(), request.getDestinationId()));
			returnToEnteringNode(request);
		}else {
			LOGGER.info(String.format("Forwarding entry request to %s:%d with id: %s", request.getHost(), request.getPort(), request.getDestinationId()));
			forwardEntryRequest(request, rowColEntry, rowIndex);
		}


	}

	/**
	 * Find best matching entry in the routing table
	 * Consults leaf set in the case that no closer entry is found in the routing table
	 *
	 * @param row the row in the routing table to start the search at
	 * @param col the column to start search at in the routing table (the base16 number at row index in destination id)
	 * @return the PeerTriplet to route to. Special case, return null if no valid entries in LeafSet
	 * 		i.e. returns null if all entries in leaf set are current node
	 */
	private PeerTriplet findValidEntry(int row, int col) {
		while(col >= 0) {
			try {
				routingTableLock.acquire();
				if(routingTable[row][col] != null) return routingTable[row][col];
				routingTableLock.release();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			col--;
		}
		int currentIDAtIndex = Integer.parseInt(identifier.substring(row, row+1), 16);
		return findEntryInTreeSet(currentIDAtIndex, col);
	}

	/**
	 * Find valid entry in tree set to route message to
	 * @param myIDNum the first non matching number (base 16) in the current nodes ID
	 * @param destIDNum the first non matching number (base 16) in the destination ID
	 * @return the PeerTriplet to route to. Special case, return null if no valid entries in LeafSet
	 * 		i.e. returns null if all entries in leaf set are current node
	 */
	private PeerTriplet findEntryInTreeSet(int myIDNum, int destIDNum) {
		if(myIDNum > destIDNum) {
			for(int i = LEAF_SET_SIZE-1; i >= 0; i--) {
				if(rightLeafset[i] != null && !rightLeafset[i].identifier.equals(identifier)) return rightLeafset[i];
			}
		}
		for(int i = LEAF_SET_SIZE-1; i >= 0; i--) {
			if(leftLeafSet[i] != null && !leftLeafSet[i].identifier.equals(identifier)) return leftLeafSet[i];
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
					LOGGER.info(String.format("Entry request sent from %s", socket.getInetAddress().getCanonicalHostName()));
					handlePeerEntryRequest((EntryRequest) event);
				} catch (UnexpectedException e) {
					e.printStackTrace();
				}
				break;
			case ENTRY_ACCEPTANCE_RESPONSE:
				this.handleEntryAcceptance((EntryAcceptanceResponse) event);
				break;
			default:
				LOGGER.warning("No actions found for message of type: " + event.getType());
				break;
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
			}catch(NumberFormatException nfe) {
				LOGGER.severe("Discovery Node requires a valid integer port: 1024 < [port] < 65536");
				System.exit(1);
			}
		}
	}
}
