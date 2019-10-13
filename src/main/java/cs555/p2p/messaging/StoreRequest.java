package cs555.p2p.messaging;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StoreRequest implements Event{
	private List<String> route;
	private String identifier;
	private byte[] fileBytes;
	private String destination;
	private String filename;
	private String storeDataHost;
	private int storeDataPort;
	private boolean requestResponse;
	public int hops;

	public void incrementHops() { hops++; }

	public int getHops() { return hops; }

	public boolean getRequestResponse() { return requestResponse; }

	public List<String> getRoute() {
		return route;
	}

	public String getIdentifier() {
		return identifier;
	}

	public byte[] getFileBytes() {
		return fileBytes;
	}

	public String getDestination() {
		return destination;
	}

	public String getFilename() {
		return filename;
	}

	public String getStoreDataHost() {
		return storeDataHost;
	}

	public int getStoreDataPort() {
		return storeDataPort;
	}

	public void updateRoute(String id) { route.add(id); }

	public StoreRequest(String identifier, byte[] fileBytes, String destination, String filename, String hostname, int port, boolean requestResponse) {
		this.identifier = identifier;
		this.fileBytes = fileBytes;
		route = new ArrayList<>();
		this.destination = destination;
		this.filename = filename;
		this.storeDataHost = hostname;
		this.storeDataPort = port;
		this.requestResponse = requestResponse;
		this.hops = 1;
	}

	public StoreRequest(String identifier, byte[] fileBytes, String destination, String filename, String hostname, int port) {
		this(identifier, fileBytes, destination, filename, hostname, port, true);
	}

	public StoreRequest(DataInputStream din) {
		MessageReader messageReader = new MessageReader(din);
		route = new ArrayList<>();
		try {
			messageReader.readStringList(route);
			identifier = messageReader.readHex();
			fileBytes = messageReader.readByteArr();
			destination = messageReader.readString();
			filename = messageReader.readString();
			storeDataHost = messageReader.readString();
			storeDataPort = messageReader.readInt();
			requestResponse = messageReader.readBoolean();
			hops = messageReader.readInt();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Type getType() {
		return Type.STORE_REQUEST;
	}

	@Override
	public byte[] getBytes() throws IOException {
		MessageMarshaller messageMarshaller = new MessageMarshaller();
		messageMarshaller.writeInt(getType().getValue());
		messageMarshaller.writeStringList(route);
		messageMarshaller.writeHex(identifier);
		messageMarshaller.writeByteArr(fileBytes);
		messageMarshaller.writeString(destination);
		messageMarshaller.writeString(filename);
		messageMarshaller.writeString(storeDataHost);
		messageMarshaller.writeInt(storeDataPort);
		messageMarshaller.writeBoolean(requestResponse);
		messageMarshaller.writeInt(hops);
		return messageMarshaller.getMarshalledData();
	}
}
