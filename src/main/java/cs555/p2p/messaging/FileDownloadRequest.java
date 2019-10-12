package cs555.p2p.messaging;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileDownloadRequest implements Event{
	private String hostname;
	private int port;
	private String filename;
	private String identifier;
	private List<String> route;

	public String getHostname() {
		return hostname;
	}

	public int getPort() {
		return port;
	}

	public String getFilename() {
		return filename;
	}

	public String getIdentifier() { return identifier; }

	public List<String> getRoute() { return route; }

	public void updateRoute(String id) { this.route.add(id); }

	public FileDownloadRequest(String hostname, int port, String filename, String identifier) {
		this.hostname = hostname;
		this.port = port;
		this.filename = filename;
		this.identifier = identifier;
		this.route = new ArrayList<>();
	}

	public FileDownloadRequest(DataInputStream din) {
		MessageReader messageReader = new MessageReader(din);
		route = new ArrayList<>();
		try {
			this.hostname = messageReader.readString();
			this.port = messageReader.readInt();
			this.filename = messageReader.readString();
			this.identifier = messageReader.readHex();
			messageReader.readStringList(route);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Type getType() {
		return Type.FILE_DOWNLOAD_REQUEST;
	}

	@Override
	public byte[] getBytes() throws IOException {
		MessageMarshaller messageMarshaller = new MessageMarshaller();
		messageMarshaller.writeInt(getType().getValue());
		messageMarshaller.writeString(hostname);
		messageMarshaller.writeInt(port);
		messageMarshaller.writeString(filename);
		messageMarshaller.writeHex(identifier);
		messageMarshaller.writeStringList(route);
		return messageMarshaller.getMarshalledData();
	}
}
