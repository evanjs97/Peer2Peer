package cs555.p2p.messaging;

import cs555.p2p.util.PeerTriplet;

import java.io.DataInputStream;
import java.io.IOException;
import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.List;

public class EntryRequest implements Event{

	private final int port;
	private final String host;
	private final String destinationId;
	private final PeerTriplet[][] tableRows;

	public int getPort() {
		return port;
	}

	public String getHost() {
		return host;
	}

	public String getDestinationId() {
		return destinationId;
	}

	public PeerTriplet[][] getTableRows() {
		return tableRows;
	}

	public EntryRequest(String host, int port, String destinationId, int numRows) {
		this.host = host;
		this.port = port;
		this.destinationId = destinationId;
		tableRows = new PeerTriplet[numRows][];
	}

	public EntryRequest(String host, int port, String destinationId, PeerTriplet[][] tableRows) {
		this.host = host;
		this.port = port;
		this.destinationId = destinationId;
		this.tableRows = tableRows;
	}

	public EntryRequest(DataInputStream din) throws UnexpectedException {
		MessageReader messageReader = new MessageReader(din);
		String host = "";
		int port = 0;
		String dest = "";
		PeerTriplet[][] array = null;
		try {
			host = messageReader.readString();

			port = messageReader.readInt();
			dest = messageReader.readHex();
			System.out.println("READING: " + host + ":" + port + " DEST: " + dest);
			array = messageReader.read2DPeerArray();
			messageReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if(array == null) throw new UnexpectedException("Error: Entry Request contained invalid array: exiting");

		this.host = host;
		this.port = port;
		this.destinationId = dest;
		this.tableRows = array;
	}

	@Override
	public Type getType() {
		return Type.ENTRY_REQUEST;
	}

	@Override
	public byte[] getBytes() throws IOException {
		MessageMarshaller messageMarshaller = new MessageMarshaller();
		System.out.println("WRITING: " + host +":" +port + " DEST: " + destinationId);
		messageMarshaller.writeInt(getType().getValue());
		messageMarshaller.writeString(host);
		messageMarshaller.writeInt(port);
		messageMarshaller.writeHex(destinationId);
		messageMarshaller.write2DPeerArray(tableRows);
		return messageMarshaller.getMarshalledData();
	}
}
