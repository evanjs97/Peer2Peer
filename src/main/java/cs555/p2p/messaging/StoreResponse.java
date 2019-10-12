package cs555.p2p.messaging;

import java.io.DataInputStream;
import java.io.IOException;

public class StoreResponse implements Event{
	private boolean success;

	public StoreResponse(boolean success) {
		this.success = success;
	}

	public boolean wasSuccess() { return this.success; }

	public StoreResponse(DataInputStream din) {
		MessageReader messageReader = new MessageReader(din);
		try {
			success = messageReader.readBoolean();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Type getType() {
		return Type.STORE_RESPONSE;
	}

	@Override
	public byte[] getBytes() throws IOException {
		MessageMarshaller messageMarshaller = new MessageMarshaller();
		messageMarshaller.writeInt(getType().getValue());
		messageMarshaller.writeBoolean(success);
		return messageMarshaller.getMarshalledData();
	}
}
