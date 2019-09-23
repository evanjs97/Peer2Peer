package cs555.p2p.node;

import cs555.p2p.messaging.Event;
import cs555.p2p.messaging.RegisterRequest;
import cs555.p2p.messaging.RegistrationFailure;
import cs555.p2p.messaging.RegistrationSuccess;
import cs555.p2p.transport.TCPSender;
import cs555.p2p.transport.TCPServer;

import java.io.IOException;
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

	private void registrationFailure(HostPort hostPort) {
		try {
			TCPSender sender = new TCPSender(new Socket(hostPort.host, hostPort.port));
			sender.sendData(new RegistrationFailure().getBytes());
			sender.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void registrationSuccess(HostPort hostPort) {
		HostPort randomPeer = getRandomPeer();
		try {
			TCPSender sender = new TCPSender(new Socket(hostPort.host, hostPort.port));
			sender.sendData(new RegistrationSuccess(randomPeer.host, randomPeer.port).getBytes());
			sender.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void init() {
		TCPServer tcpServer = new TCPServer(port, this);
		this.port = tcpServer.getLocalPort();
		Thread serverThread = new Thread(tcpServer);
		serverThread.start();
	}

	private void registerPeer(RegisterRequest request, Socket socket) {
		HostPort add = new HostPort(socket.getInetAddress().getCanonicalHostName(), request.getPort());
		HostPort previous = nodeIDMappings.putIfAbsent(request.getIdentifier(), add);
		if(previous != null) registrationFailure(add);
		else registrationSuccess(add);
	}

	private HostPort getRandomPeer() {
		Object[] keys = nodeIDMappings.keySet().toArray();
		String key = (String) keys[ThreadLocalRandom.current().nextInt(0, keys.length)];
		return nodeIDMappings.get(key);
	}

	@Override
	public void onEvent(Event event, Socket socket) {
		switch (event.getType()) {
			case REGISTRATION_REQUEST:
				registerPeer((RegisterRequest) event, socket);
				break;
			default:
				LOGGER.warning("No actions found for message of type: " + event.getType());
				break;
		}
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
