package cs555.p2p.messaging;

import cs555.p2p.util.PeerTriplet;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class EntryAcceptanceResponse implements Event{
	private final PeerTriplet[][] tableRows;
	private final PeerTriplet[] rightLeafSet;
	private final PeerTriplet[] leftLeafSet;
	private final List<String> route;

	public PeerTriplet[][] getTableRows() {
		return tableRows;
	}

	public PeerTriplet[] getRightLeafSet() {
		return rightLeafSet;
	}

	public PeerTriplet[] getLeftLeafSet() {
		return leftLeafSet;
	}

	public List<String> getRoute() {
		return route;
	}

	public EntryAcceptanceResponse(PeerTriplet[] leftLeafSet, PeerTriplet[] rightLeafSet, PeerTriplet[][] tableRows, List<String> route) {
		this.tableRows = tableRows;
		this.rightLeafSet = rightLeafSet;
		this.leftLeafSet = leftLeafSet;
		this.route = route;
	}

	public EntryAcceptanceResponse(DataInputStream din) {
		MessageReader messageReader = new MessageReader(din);
		PeerTriplet[][] tableRows = null;
		PeerTriplet[] leftSet = null;
		PeerTriplet[] rightSet = null;
		route = new LinkedList<>();
		try {
			tableRows = messageReader.read2DPeerArray();
			rightSet = messageReader.read1DPeerArray();
			leftSet = messageReader.read1DPeerArray();
			messageReader.readStringList(route);
			messageReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.tableRows = tableRows;
		this.rightLeafSet = rightSet;
		this.leftLeafSet = leftSet;
	}

	@Override
	public Type getType() {
		return Type.ENTRY_ACCEPTANCE_RESPONSE;
	}

	@Override
	public byte[] getBytes() throws IOException {
		MessageMarshaller messageMarshaller = new MessageMarshaller();
		messageMarshaller.writeInt(getType().getValue());
		messageMarshaller.write2DPeerArray(tableRows);
		messageMarshaller.write1DPeerArray(rightLeafSet);
		messageMarshaller.write1DPeerArray(leftLeafSet);
		messageMarshaller.writeStringList(route);
		return messageMarshaller.getMarshalledData();
	}
}
