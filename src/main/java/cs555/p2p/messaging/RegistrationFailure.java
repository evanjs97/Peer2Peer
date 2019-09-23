package cs555.p2p.messaging;

import java.io.IOException;

public class RegistrationFailure implements Event{

	@Override
	public Type getType() {
		return Type.ID_NOT_AVAILABLE;
	}

	@Override
	public byte[] getBytes() throws IOException {
		MessageMarshaller messageMarshaller = new MessageMarshaller();
		messageMarshaller.writeInt(getType().getValue());
		return messageMarshaller.getMarshalledData();
	}
}
