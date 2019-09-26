package cs555.p2p.messaging;

import cs555.p2p.util.PeerTriplet;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EntryRequest implements Event{

	private final int port;
	private final String host;
	private final String destinationId;
	private final List<PeerTriplet[]> tableRows;

	public int getPort() {
		return port;
	}

	public String getHost() {
		return host;
	}

	public String getDestinationId() {
		return destinationId;
	}

	public List<PeerTriplet[]> getTableRows() {
		return tableRows;
	}

	public EntryRequest(String host, int port, String destinationId) {
		this.host = host;
		this.port = port;
		this.destinationId = destinationId;
		tableRows = new ArrayList<>();
	}

	public EntryRequest(String host, int port, String destinationId, List<PeerTriplet[]> tableRows) {
		this.host = host;
		this.port = port;
		this.destinationId = destinationId;
		this.tableRows = tableRows;
	}

	public EntryRequest(DataInputStream din) {
		MessageReader messageReader = new MessageReader(din);
		String host = "";
		int port = 0;
		String dest = "";
		tableRows = new ArrayList<>();
		try {
			host = messageReader.readString();
			port = messageReader.readInt();
			dest = messageReader.readHex();
			messageReader.read1DPeerArrayList(tableRows);
			messageReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.host = host;
		this.port = port;
		this.destinationId = dest;

	}

	@Override
	public Type getType() {
		return Type.ENTRY_REQUEST;
	}

	@Override
	public byte[] getBytes() throws IOException {
		MessageMarshaller messageMarshaller = new MessageMarshaller();
		messageMarshaller.writeString(host);
		messageMarshaller.writeInt(getType().getValue());
		messageMarshaller.writeHex(destinationId);
		messageMarshaller.writeInt(port);
		messageMarshaller.write1dPeerArrList(tableRows);
		return messageMarshaller.getMarshalledData();
	}
}
