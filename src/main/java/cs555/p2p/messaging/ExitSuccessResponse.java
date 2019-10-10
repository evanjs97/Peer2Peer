package cs555.p2p.messaging;

import java.io.IOException;

public class ExitSuccessResponse implements Event{
	@Override
	public Type getType() {
		return Type.EXIT_SUCCESS_RESPONSE;
	}

	@Override
	public byte[] getBytes() throws IOException {
		MessageMarshaller messageMarshaller = new MessageMarshaller();
		messageMarshaller.writeInt(getType().getValue());
		return messageMarshaller.getMarshalledData();
	}
}
