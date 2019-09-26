package cs555.p2p.messaging;

import java.io.DataInputStream;
import java.io.IOException;

public class RegisterRequest implements Event{

	private final String identifier;
	private final int port;

	public String getIdentifier() {
		return identifier;
	}

	public int getPort() {
		return port;
	}

	public RegisterRequest(String identifier, int port) {
		this.identifier = identifier;
		this.port = port;
	}

	public RegisterRequest(DataInputStream din) {
		MessageReader messageReader = new MessageReader(din);
		String id = "";
		int port = 0;
		try {
			id = messageReader.readHex();
			port = messageReader.readInt();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.identifier = id;
		this.port = port;
	}

	@Override
	public Type getType() {
		return Type.REGISTRATION_REQUEST;
	}

	@Override
	public byte[] getBytes() throws IOException {
		MessageMarshaller messageMarshaller = new MessageMarshaller();
		messageMarshaller.writeInt(getType().getValue());
		messageMarshaller.writeHex(identifier);
		messageMarshaller.writeInt(port);
		return messageMarshaller.getMarshalledData();
	}

}
