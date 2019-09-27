package cs555.p2p.messaging;

import cs555.p2p.util.PeerTriplet;

import java.io.IOException;
import java.util.List;

public class EntryAcceptanceResponse implements Event{
	private final PeerTriplet[][] tableRows;
	private final PeerTriplet[] rightLeafSet;
	private final PeerTriplet[] leftLeafSet;

	public EntryAcceptanceResponse(PeerTriplet[] leftLeafSet, PeerTriplet[] rightLeafSet, PeerTriplet[][] tableRows) {
		this.tableRows = tableRows;
		this.rightLeafSet = rightLeafSet;
		this.leftLeafSet = leftLeafSet;
	}
	@Override
	public Type getType() {
		return Type.ENTRY_ACCEPTANCE_RESPONSE;
	}

	@Override
	public byte[] getBytes() throws IOException {
		MessageMarshaller messageMarshaller = new MessageMarshaller();
		messageMarshaller.write2DPeerArray(tableRows);
		messageMarshaller.write1DPeerArray(rightLeafSet);
		messageMarshaller.write1DPeerArray(leftLeafSet);
		return messageMarshaller.getMarshalledData();
	}
}
