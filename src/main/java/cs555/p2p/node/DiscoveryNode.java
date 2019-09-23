package cs555.p2p.node;

import cs555.p2p.messaging.Event;
import cs555.p2p.transport.TCPServer;

import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.*;

public class DiscoveryNode implements Node{
	private final static Logger LOGGER = Logger.getLogger(DiscoveryNode.class.getName());
	private class HostPort {
		int port;
		String host;

		HostPort(String host, int port) {
			this.host = host;
			this.port = port;
		}
	}

	private int port;
	private final ConcurrentHashMap<String, HostPort> nodeIDMappings;

	private DiscoveryNode(int port) {
		this.port = port;
		this.nodeIDMappings = new ConcurrentHashMap<>();
	}

	private void registrationFailure(String host, int port) {

	}

	private void init() {
		TCPServer tcpServer = new TCPServer(port, this);
		this.port = tcpServer.getLocalPort();
		Thread serverThread = new Thread(tcpServer);
		serverThread.start();
	}

	private void registerPeer(String host, int port, String id) {
		HostPort previous = nodeIDMappings.putIfAbsent(id, new HostPort(host, port));
		if(previous != null) registrationFailure(host, port);
	}

	private void getRandomPeer() {
		Object[] keys = nodeIDMappings.keySet().toArray();
		String key = (String) keys[ThreadLocalRandom.current().nextInt(0, keys.length)];
		HostPort value = nodeIDMappings.get(key);
	}

	@Override
	public void onEvent(Event event, Socket socket) {

	}

	public static void main(String[] args) {
		LOGGER.setLevel(Level.SEVERE);
		if(args.length < 1) {
			LOGGER.severe("Discovery Node requires at least 1 argument: int:[port]");
			System.exit(1);
		}else {
			try {
				int port = Integer.parseInt(args[0]);
				if(port > 65535 || port < 1024) throw new NumberFormatException();
				DiscoveryNode discoveryNode = new DiscoveryNode(port);
				discoveryNode.init();

			}catch(NumberFormatException nfe) {
				LOGGER.severe("Discovery Node requires a valid integer port: 1024 < [port] < 65536");
				System.exit(1);
			}
		}
	}

}
