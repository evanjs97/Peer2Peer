package cs555.p2p.node;

import cs555.p2p.messaging.EntryRequest;
import cs555.p2p.messaging.Event;
import cs555.p2p.messaging.RegisterRequest;
import cs555.p2p.messaging.RegistrationSuccess;
import cs555.p2p.transport.TCPSender;
import cs555.p2p.transport.TCPServer;
import cs555.p2p.util.IDUtils;
import cs555.p2p.util.PeerTriplet;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.UnexpectedException;
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
	private final PeerTriplet[][] routingTable;
	private final PeerTriplet[] rightLeafset;
	private final PeerTriplet[] leftLeafSet;

	private PeerNode(String discoveryHost, int discoveryPort) {
		this(discoveryHost, discoveryPort,0);
	}

	private PeerNode(String discoveryHost, int discoveryPort, int port) {
		this(discoveryHost, discoveryPort, null, port);
	}

	private PeerNode(String discoveryHost, int discoveryPort, String identifier) {
		this(discoveryHost, discoveryPort, identifier, 0);
	}

	private PeerNode(String discoveryHost, int discoveryPort, String identifier, int port) {
		this.discoveryHost = discoveryHost;
		this.discoveryPort = discoveryPort;
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
	}

	private void init() {
		TCPServer tcpServer = new TCPServer(port, this);
		this.port = tcpServer.getLocalPort();
		Thread serverThread = new Thread(tcpServer);
		serverThread.start();
		register();
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
			TCPSender sender = new TCPSender(new Socket(response.getEntryHost(), response.getEntryPort()));
			sender.sendData(new EntryRequest(this.hostname, this.port, identifier).getBytes());
			sender.close();
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

	private void returnToEnteringNode(EntryRequest request) {

	}

	/**
	 * Handles incoming peer entry request and routes entry request to next node in path to destination node (where entry request originated)
	 * In the case that the current node is the closest node route message back to originating node (destination node)
	 * @param request the incoming entry request
	 * @throws UnexpectedException if ID's match
	 */
	private void handlePeerEntryRequest(EntryRequest request) throws UnexpectedException {
		int rowIndex = IDUtils.firstNonMatchingIndex(identifier, request.getDestinationId());
		if(rowIndex >= routingTable.length)
			throw new UnexpectedException("ID of target matches current ID: Node ID's must be unique");


		int colIndex = Integer.parseInt(request.getDestinationId().substring(rowIndex, rowIndex+1), 16);
		PeerTriplet rowColEntry = findValidEntry(rowIndex, colIndex);
		if(rowColEntry == null) returnToEnteringNode(request);


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
			if(routingTable[row][col] != null) return routingTable[row][col];
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
			for(int i = LEAF_SET_SIZE-1; i >= 0; i++) {
				if(!rightLeafset[i].equals(identifier)) return rightLeafset[i];
			}
		}
		for(int i = LEAF_SET_SIZE-1; i >= 0; i++) {
			if(!leftLeafSet[i].equals(identifier)) return leftLeafSet[i];
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
					handlePeerEntryRequest((EntryRequest) event);
				} catch (UnexpectedException e) {
					e.printStackTrace();
				}
				break;
			default:
				LOGGER.warning("No actions found for message of type: " + event.getType());
				break;
		}
	}

	public static void main(String[] args) {
		LOGGER.setLevel(Level.SEVERE);
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
