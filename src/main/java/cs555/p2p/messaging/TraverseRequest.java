package cs555.p2p.messaging;

import cs555.p2p.util.PeerTriplet;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TraverseRequest implements Event{
	private final List<String> peers;

	public List<String> getPeers() {
		return peers;
	}

	public void addPeer(String id) {
		peers.add(id);
	}

	public TraverseRequest(String first) {
		peers = new ArrayList<>();
		peers.add(first);
	}

	public TraverseRequest(DataInputStream din) {
		peers = new ArrayList<>();
		try {
			MessageReader messageReader = new MessageReader(din);
			messageReader.readStringList(peers);
			messageReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Type getType() {
		return Type.TRAVERSE_REQUEST;
	}

	@Override
	public byte[] getBytes() throws IOException {
		MessageMarshaller messageMarshaller = new MessageMarshaller();
		messageMarshaller.writeInt(getType().getValue());
		messageMarshaller.writeStringList(peers);
		return messageMarshaller.getMarshalledData();
	}
}
