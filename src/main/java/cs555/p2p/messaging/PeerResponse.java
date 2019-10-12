package cs555.p2p.messaging;

import cs555.p2p.util.PeerTriplet;

import java.io.DataInputStream;
import java.io.IOException;

public class PeerResponse implements Event{
	private String hostname;
	private int port;

	public String getHostname() { return hostname; }

	public int getPort() { return port; }

	public PeerResponse() {
		hostname = "";
		port = 0;
	}

	public PeerResponse(String hostname, int port) {
		this.hostname = hostname;
		this.port = port;
	}

	public PeerResponse(DataInputStream din) {
		MessageReader messageReader = new MessageReader(din);
		try {
			hostname = messageReader.readString();
			port = messageReader.readInt();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Event.Type getType() {
		return Event.Type.PEER_RESPONSE;
	}

	@Override
	public byte[] getBytes() throws IOException {
		MessageMarshaller messageMarshaller = new MessageMarshaller();
		messageMarshaller.writeInt(getType().getValue());
		messageMarshaller.writeString(hostname);
		messageMarshaller.writeInt(port);
		return messageMarshaller.getMarshalledData();
	}
}
