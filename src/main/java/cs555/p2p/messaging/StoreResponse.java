package cs555.p2p.messaging;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StoreResponse implements Event{
	private boolean success;
	private List<String> route;
	private int hops;

	public int getHops() { return hops; }

	public StoreResponse(boolean success, List<String> route, int hops) {
		this.success = success;
		this.route = route;
		this.hops = hops;
	}

	public boolean wasSuccess() { return this.success; }

	public List<String> getRoute() { return route; }

	public StoreResponse(DataInputStream din) {
		MessageReader messageReader = new MessageReader(din);
		route = new ArrayList<>();
		try {
			success = messageReader.readBoolean();
			messageReader.readStringList(route);
			hops = messageReader.readInt();
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
		messageMarshaller.writeInt(hops);
		return messageMarshaller.getMarshalledData();
	}
}
