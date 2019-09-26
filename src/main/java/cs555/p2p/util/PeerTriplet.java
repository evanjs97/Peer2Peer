package cs555.p2p.util;

public class PeerTriplet {
	public final int port;
	public final String host;
	public final String identifier;

	public PeerTriplet(String host, int port, String identifier) {
		this.host = host;
		this.port = port;
		this.identifier = identifier;
	}
}
