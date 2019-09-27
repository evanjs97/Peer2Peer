package cs555.p2p.util;

import cs555.p2p.messaging.MessageMarshaller;
import cs555.p2p.messaging.MessageReader;

import java.io.IOException;

public class PeerTriplet {
	public final int port;
	public final String host;
	public final String identifier;


	public PeerTriplet(String host, int port, String identifier) {
		this.host = host;
		this.port = port;
		this.identifier = identifier;
	}

	public PeerTriplet(MessageReader reader) {
		int port = 0;
		String host = null;
		String identifier = null;
		try {
			port = reader.readInt();
			host = reader.readString();
			identifier = reader.readHex();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.port = port;
		this.host = host;
		this.identifier = identifier;
	}

	public void writeToStream(MessageMarshaller marshaller) throws IOException {
		marshaller.writeInt(port);
		marshaller.writeString(host);
		marshaller.writeHex(identifier);
	}

	public String toString() {
		return this.host + ":" + this.identifier;
	}
}
