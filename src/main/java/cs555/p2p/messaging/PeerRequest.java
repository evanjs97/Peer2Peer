package cs555.p2p.messaging;

import java.io.DataInputStream;
import java.io.IOException;

public class PeerRequest implements Event{

	private String identifier;
	private int port;

	public String getIdentifier() { return identifier; }

	public int getPort() { return port; }

	public PeerRequest(int port) {

		identifier = null;
		this.port = port;
	}

	public PeerRequest(String identifier, int port) {

		this.identifier = identifier;
		this.port = port;
	}

	public PeerRequest(DataInputStream din) {
		MessageReader messageReader = new MessageReader(din);
		try {
			boolean exists = messageReader.readBoolean();
			if(exists) identifier = messageReader.readHex();
			port = messageReader.readInt();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Type getType() {
		return Type.PEER_REQUEST;
	}

	@Override
	public byte[] getBytes() throws IOException {
		MessageMarshaller messageMarshaller = new MessageMarshaller();
		messageMarshaller.writeInt(getType().getValue());
		messageMarshaller.writeBoolean(identifier != null);
		if(identifier != null) messageMarshaller.writeHex(identifier);
		messageMarshaller.writeInt(port);
		return messageMarshaller.getMarshalledData();
	}
}
