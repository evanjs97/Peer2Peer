package cs555.p2p.messaging;

import cs555.p2p.util.PeerTriplet;

import java.io.IOException;
import java.util.List;

public class EntryAcceptanceResponse implements Event{
	private final List<PeerTriplet[]> tableRows;
	private final PeerTriplet[] rightLeafset;
	private final PeerTriplet[] leftLeafSet;

	public EntryAcceptanceResponse(PeerTriplet[] leftLeafSet, PeerTriplet[] rightLeafset, List<PeerTriplet[]> tableRows) {
		this.tableRows = tableRows;
		this.rightLeafset = rightLeafset;
		this.leftLeafSet = leftLeafSet;
	}
	@Override
	public Type getType() {
		return Type.ENTRY_ACCEPTANCE_RESPONSE;
	}

	@Override
	public byte[] getBytes() throws IOException {
		return new byte[0];
	}
}
