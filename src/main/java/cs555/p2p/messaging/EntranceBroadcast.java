package cs555.p2p.messaging;

import cs555.p2p.util.PeerTriplet;

import java.io.DataInputStream;
import java.io.IOException;

public class EntranceBroadcast implements Event{
	private PeerTriplet self;

	public PeerTriplet getPeer() {
		return this.self;
	}

	public EntranceBroadcast(PeerTriplet self) {
		this.self = self;
	}

	public EntranceBroadcast(DataInputStream din) {
		MessageReader messageReader = new MessageReader(din);
		this.self = new PeerTriplet(messageReader);
		try {
			messageReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Type getType() {
		return Type.ENTRANCE_BROADCAST;
	}

	@Override
	public byte[] getBytes() throws IOException {
		MessageMarshaller messageMarshaller = new MessageMarshaller();
		messageMarshaller.writeInt(getType().getValue());
		self.writeToStream(messageMarshaller);
		return messageMarshaller.getMarshalledData();
	}
}
