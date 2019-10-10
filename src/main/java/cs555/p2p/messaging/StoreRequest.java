package cs555.p2p.messaging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StoreRequest implements Event{
	private List<String> route;
	private String identifier;
	private byte[] fileBytes;

	public StoreRequest(String identifier, byte[] fileBytes) {
		this.identifier = identifier;
		this.fileBytes = fileBytes;
		route = new ArrayList<>();
	}

	@Override
	public Type getType() {
		return Type.STORE_REQUEST;
	}

	@Override
	public byte[] getBytes() throws IOException {
		MessageMarshaller messageMarshaller = new MessageMarshaller();
		messageMarshaller.writeInt(getType().getValue());
		messageMarshaller.writeStringList(route);
		messageMarshaller.writeString(identifier);
		messageMarshaller.writeByteArr(fileBytes);
		return messageMarshaller.getMarshalledData();
	}
}
