package cs555.p2p.messaging;

import cs555.p2p.util.IDUtils;
import cs555.p2p.util.PeerTriplet;

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

	public String readHex() throws IOException {
		return IDUtils.convertBytesToHex(readByteArr());
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

	public PeerTriplet[] read1DPeerArray() throws IOException {
		int arrSize = readInt();
		if(readInt() == 0) return null;
		PeerTriplet[] arr = new PeerTriplet[arrSize];
		for(int i = 0; i < arr.length; i++) {
			arr[i] = new PeerTriplet(this);
		}
		return arr;
	}

	public void read1DPeerArrayList(List<PeerTriplet[]> list) throws IOException {
		int arraySize = readInt();
		for(int i = 0; i < arraySize; i++) {
			list.add(read1DPeerArray());
		}
	}

	public PeerTriplet[][] read2DPeerArray() throws IOException {
		int arraySize = readInt();
		PeerTriplet[][] array = new PeerTriplet[arraySize][];
		for(int i = 0; i < arraySize; i++) {
			array[i] = read1DPeerArray();
		}
		return array;
	}

	public void close() throws IOException {
		din.close();
	}
}