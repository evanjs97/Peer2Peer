package cs555.p2p.messaging;

import cs555.p2p.util.PeerTriplet;

import java.io.DataInputStream;
import java.io.IOException;
import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class EntryRequest implements Event{

	private final int port;
	private final String host;
	private final String destinationId;
	private final PeerTriplet[][] tableRows;
	private final List<String> routeTrace;
	private int forwardPort;
	private int hopCount;

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

	public void setTableRow(int row, PeerTriplet[] arr) {
		if(tableRows[row] == null) tableRows[row] = arr;
		else {
			for(int col = 0; col < arr.length; col++) {
				if(arr[col] != null) setTableEntryIfEmpty(row, col, arr[col]);
			}
		}
	}

	public void setTableEntryIfEmpty(int row, int col, PeerTriplet entry) {
		if(tableRows[row] == null) tableRows[row] = new PeerTriplet[16];
		if(tableRows[row][col] == null) tableRows[row][col] = entry;
	}

	public List<String> getRouteTrace() {
		return this.routeTrace;
	}

	public void addRouteID(String id) {
		routeTrace.add(id);
	}

	public int getHopCount() {
		return this.hopCount;
	}

	public void incrementHopCount() {
		hopCount++;
	}

	public int getForwardPort() {
		return forwardPort;
	}

	public void setForwardPort(int forwardPort) {
		this.forwardPort = forwardPort;
	}

	public EntryRequest(String host, int port, String destinationId, int numRows, int forwardPort) {
		this.host = host;
		this.port = port;
		this.destinationId = destinationId;
		tableRows = new PeerTriplet[numRows][];
		this.forwardPort = forwardPort;
		this.hopCount = 1;
		routeTrace = new LinkedList<>();
		routeTrace.add(destinationId);
	}

	public EntryRequest(String host, int port, String destinationId, PeerTriplet[][] tableRows, int forwardPort) {
		this.host = host;
		this.port = port;
		this.destinationId = destinationId;
		this.tableRows = tableRows;
		this.forwardPort = forwardPort;
		this.hopCount = 1;
		routeTrace = new LinkedList<>();
		routeTrace.add(destinationId);
	}

	public EntryRequest(DataInputStream din) throws UnexpectedException {
		MessageReader messageReader = new MessageReader(din);
		String host = "";
		int port = 0;
		String dest = "";
		PeerTriplet[][] array = null;
		int forwardPort = 0;
		int hops = 0;
		routeTrace = new LinkedList<>();
		try {
			host = messageReader.readString();

			port = messageReader.readInt();
			dest = messageReader.readHex();
			System.out.println("READING: " + host + ":" + port + " DEST: " + dest);
			array = messageReader.read2DPeerArray();
			forwardPort = messageReader.readInt();
			hops = messageReader.readInt();
			messageReader.readStringList(routeTrace);
			messageReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if(array == null) throw new UnexpectedException("Error: Entry Request contained invalid array: exiting");

		this.host = host;
		this.port = port;
		this.destinationId = dest;
		this.tableRows = array;
		this.forwardPort = forwardPort;
		this.hopCount = hops;
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
		messageMarshaller.writeInt(forwardPort);
		messageMarshaller.writeInt(hopCount);
		messageMarshaller.writeStringList(routeTrace);
		return messageMarshaller.getMarshalledData();
	}
}
