package cs555.p2p.node;

import cs555.p2p.messaging.Event;
import cs555.p2p.transport.TCPServer;

import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PeerNode implements Node{
	private final static Logger LOGGER = Logger.getLogger(PeerNode.class.getName());

	private final String discoveryHost;
	private final int discoveryPort;
	private int port;
	private String identifier;

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
	}

	private void init() {
		TCPServer tcpServer = new TCPServer(port, this);
		this.port = tcpServer.getLocalPort();
		Thread serverThread = new Thread(tcpServer);
		serverThread.start();
	}

	private void generateID() {

	}

	private void register() {
		if(identifier == null) generateID();


	}


	@Override
	public void onEvent(Event event, Socket socket) {

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
