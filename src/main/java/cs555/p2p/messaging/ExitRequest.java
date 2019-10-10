package cs555.p2p.messaging;

import cs555.p2p.util.PeerTriplet;

import java.io.DataInputStream;
import java.io.IOException;

public class ExitRequest implements Event{

	private String identifier;
	private PeerTriplet newLeft;
	private int leftIndex;
	private PeerTriplet newRight;
	private int rightIndex;
	private int port;

	public String getIdentifier() {
		return identifier;
	}

	public PeerTriplet getNewLeft() {
		return newLeft;
	}

	public int getLeftIndex() {
		return leftIndex;
	}

	public PeerTriplet getNewRight() {
		return newRight;
	}

	public int getRightIndex() {
		return rightIndex;
	}

	public int getPort() { return port; }

	public ExitRequest(String identifier, PeerTriplet newLeft, PeerTriplet newRight, int leftIndex, int rightIndex, int port) {
		this.identifier = identifier;
		this.newLeft = newLeft;
		this.newRight = newRight;
		this.leftIndex = leftIndex;
		this.rightIndex = rightIndex;
		this.port = port;
	}

	public ExitRequest(DataInputStream din) {

		MessageReader messageReader = new MessageReader(din);
		try {
			identifier = messageReader.readString();
			newLeft = messageReader.readBoolean() ? new PeerTriplet(messageReader) : null;
			newRight = messageReader.readBoolean() ? new PeerTriplet(messageReader) : null;
			leftIndex = messageReader.readInt();
			rightIndex = messageReader.readInt();
			port = messageReader.readInt();
			messageReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Type getType() {
		return Type.EXIT_REQUEST;
	}

	@Override
	public byte[] getBytes() throws IOException {
		MessageMarshaller messageMarshaller = new MessageMarshaller();
		messageMarshaller.writeInt(getType().getValue());
		messageMarshaller.writeString(identifier);
		messageMarshaller.writeBoolean(newLeft != null);
		if(newLeft != null) newLeft.writeToStream(messageMarshaller);
		messageMarshaller.writeBoolean(newRight != null);
		if(newRight != null) newRight.writeToStream(messageMarshaller);
		messageMarshaller.writeInt(leftIndex);
		messageMarshaller.writeInt(rightIndex);
		messageMarshaller.writeInt(port);

		return messageMarshaller.getMarshalledData();
	}
}
