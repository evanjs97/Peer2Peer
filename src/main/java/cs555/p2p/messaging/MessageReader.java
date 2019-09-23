package cs555.p2p.messaging;

import java.io.DataInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

public class MessageReader {

	private final DataInputStream din;

	public MessageReader(DataInputStream din) {
		this.din = din;
	}

	public int readInt() throws IOException {
		return din.readInt();
	}

	public boolean readBoolean() throws IOException {
		return din.readBoolean();
	}

	public long readLong() throws IOException {
		return din.readLong();
	}

	public double readDouble() throws IOException {
		return din.readDouble();
	}

	public Instant readInstant() throws IOException {
		return Instant.parse(readString());
	}

	public String readString() throws IOException{
		return new String(readByteArr());
	}

	public byte[] readByteArr() throws IOException {
		byte[] bytes = new byte[din.readInt()];
		din.readFully(bytes);
		return bytes;
	}

	public void readIntUtilList(List<Integer> ints) throws IOException {
		int listSize = din.readInt();
		for(int i = 0; i < listSize; i++) {
			ints.add(readInt());
		}
	}

	public void readStringList(List<String> ints) throws IOException {
		int listSize = din.readInt();
		for(int i = 0; i < listSize; i++) {
			ints.add(readString());
		}
	}

	public void close() throws IOException {
		din.close();
	}
}