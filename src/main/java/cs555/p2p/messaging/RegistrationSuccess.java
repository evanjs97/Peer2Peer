package cs555.p2p.messaging;

import java.io.DataInputStream;
import java.io.IOException;

public class RegistrationSuccess implements Event{

	private final String entryHost;
	private final int entryPort;

	public RegistrationSuccess(String host, int port) {
		this.entryHost = host;
		this.entryPort = port;
	}

	public RegistrationSuccess(DataInputStream din) {
		MessageReader messageReader = new MessageReader(din);

		String host = "";
		int port = 0;
		try {
			host = messageReader.readString();
			port = messageReader.readInt();
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}
		this.entryHost = host;
		this.entryPort = port;
	}

	@Override
	public Type getType() {
		return Type.REGISTRATION_SUCCESS;
	}


	@Override
	public byte[] getBytes() throws IOException {
		MessageMarshaller messageMarshaller = new MessageMarshaller();
		messageMarshaller.marshallIntStringInt(getType().getValue(), entryHost, entryPort);
		return messageMarshaller.getMarshalledData();
	}
}
