package cs555.p2p.messaging;

import cs555.p2p.util.PeerTriplet;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;

public class EntryAcceptanceResponse implements Event{
	private final PeerTriplet[][] tableRows;
	private final PeerTriplet[] rightLeafSet;
	private final PeerTriplet[] leftLeafSet;


	public PeerTriplet[][] getTableRows() {
		return tableRows;
	}

	public PeerTriplet[] getRightLeafSet() {
		return rightLeafSet;
	}

	public PeerTriplet[] getLeftLeafSet() {
		return leftLeafSet;
	}

	public EntryAcceptanceResponse(PeerTriplet[] leftLeafSet, PeerTriplet[] rightLeafSet, PeerTriplet[][] tableRows) {
		this.tableRows = tableRows;
		this.rightLeafSet = rightLeafSet;
		this.leftLeafSet = leftLeafSet;
	}

	public EntryAcceptanceResponse(DataInputStream din) {
		MessageReader messageReader = new MessageReader(din);
		PeerTriplet[][] tableRows = null;
		PeerTriplet[] leftSet = null;
		PeerTriplet[] rightSet = null;
		try {
			tableRows = messageReader.read2DPeerArray();
			rightSet = messageReader.read1DPeerArray();
			leftSet = messageReader.read1DPeerArray();
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
		return messageMarshaller.getMarshalledData();
	}
}
