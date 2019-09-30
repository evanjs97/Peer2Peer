package cs555.p2p.node;

import cs555.p2p.messaging.Event;
import cs555.p2p.messaging.RegisterRequest;
import cs555.p2p.messaging.RegistrationFailure;
import cs555.p2p.messaging.RegistrationSuccess;
import cs555.p2p.transport.TCPSender;
import cs555.p2p.transport.TCPServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
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

	private final String nickname = "The Night King";
	private String hostname;
	private int port;
	private final ConcurrentHashMap<String, HostPort> nodeIDMappings;

	private DiscoveryNode(int port) {
		LOGGER.info("Created Discovery Node");
		this.port = port;
		this.nodeIDMappings = new ConcurrentHashMap<>();
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

	private void registrationSuccess(HostPort hostPort, String id) {
		HostPort randomPeer = getRandomPeer(id);
		try {
			TCPSender sender = new TCPSender(new Socket(hostPort.host, hostPort.port));
			RegistrationSuccess response;
			if(randomPeer == null) response = new RegistrationSuccess("", 0);
			else response = new RegistrationSuccess(randomPeer.host, randomPeer.port);
			sender.sendData(response.getBytes());
			sender.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void init() {
		LOGGER.info("Starting Discovery Node");
		TCPServer tcpServer = new TCPServer(port, this);
		this.port = tcpServer.getLocalPort();
		Thread serverThread = new Thread(tcpServer);
		serverThread.start();
		LOGGER.info(String.format("%s: Starting up at address %s:%d", nickname, hostname, port));
	}

	private void registerPeer(RegisterRequest request, Socket socket) {
		HostPort add = new HostPort(socket.getInetAddress().getCanonicalHostName(), request.getPort());
		HostPort previous = nodeIDMappings.putIfAbsent(request.getIdentifier(), add);
		LOGGER.info(String.format("Received Register Request from: %s:%d", socket.getInetAddress().getCanonicalHostName(), request.getPort()));
		LOGGER.info("ID: " + request.getIdentifier());
		if(previous != null) registrationFailure(add);
		else registrationSuccess(add, request.getIdentifier());
	}

	private HostPort getRandomPeer(String id) {
		Object[] keys = nodeIDMappings.keySet().toArray();
		if(keys.length == 1) return null;
		int index = ThreadLocalRandom.current().nextInt(0, keys.length);
		String key = (String) keys[index];
		if(key.equals(id)) {
			if(index == 0) key = (String) keys[index+1];
			else key = (String) keys[index-1];
		}
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
		LOGGER.setLevel(Level.INFO);
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
