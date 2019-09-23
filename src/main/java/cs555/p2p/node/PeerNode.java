package cs555.p2p.node;

import cs555.p2p.messaging.Event;
import cs555.p2p.messaging.RegisterRequest;
import cs555.p2p.messaging.RegistrationSuccess;
import cs555.p2p.transport.TCPSender;
import cs555.p2p.transport.TCPServer;
import cs555.p2p.util.IDUtils;

import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PeerNode implements Node{
	private final static Logger LOGGER = Logger.getLogger(PeerNode.class.getName());
	private final static int IDENTITIFER_BITS = 16;

	private final String discoveryHost;
	private final int discoveryPort;
	private int port;
	private String identifier;
	private final String[][] routingTable;

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
		this.identifier = identifier;
		this.routingTable = new String[IDENTITIFER_BITS/4][IDENTITIFER_BITS/4];
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

	private void p2pEntry(RegistrationSuccess response) {

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
