package cs555.p2p.messaging;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileDownloadResponse implements Event{

	private byte[] fileBytes;
	private List<String> route;

	public List<String> getRoute() {
		return route;
	}

	public byte[] getFileBytes() { return fileBytes; }

	public FileDownloadResponse(byte[] bytes, List<String> route) {
		this.fileBytes = bytes;
		this.route = route;
	}

	public FileDownloadResponse(DataInputStream din) {
		MessageReader messageReader = new MessageReader(din);
		try {
			route = new ArrayList<>();
			boolean success = messageReader.readBoolean();
			fileBytes = success ? messageReader.readByteArr() : null;
			messageReader.readStringList(route);
			messageReader.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public Type getType() {
		return Type.FILE_DOWNLOAD_RESPONSE;
	}

	@Override
	public byte[] getBytes() throws IOException {
		MessageMarshaller messageMarshaller = new MessageMarshaller();
		messageMarshaller.writeInt(getType().getValue());
		messageMarshaller.writeBoolean(fileBytes != null);
		messageMarshaller.writeByteArr(fileBytes);
		messageMarshaller.writeStringList(route);
		return messageMarshaller.getMarshalledData();
	}
}
