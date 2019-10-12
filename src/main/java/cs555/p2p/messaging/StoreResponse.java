package cs555.p2p.messaging;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StoreResponse implements Event{
	private boolean success;
	private List<String> route;

	public StoreResponse(boolean success, List<String> route) {
		this.success = success;
		this.route = route;
	}

	public boolean wasSuccess() { return this.success; }

	public List<String> getRoute() { return route; }

	public StoreResponse(DataInputStream din) {
		MessageReader messageReader = new MessageReader(din);
		route = new ArrayList<>();
		try {
			success = messageReader.readBoolean();
			messageReader.readStringList(route);
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
		messageMarshaller.writeStringList(route);
		return messageMarshaller.getMarshalledData();
	}
}
