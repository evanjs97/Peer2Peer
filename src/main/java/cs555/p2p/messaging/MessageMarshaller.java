package cs555.p2p.messaging;

import cs555.p2p.util.IDUtils;
import cs555.p2p.util.PeerTriplet;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

public class MessageMarshaller {

	private final ByteArrayOutputStream baOutStream;
	private final DataOutputStream dout;

	public MessageMarshaller() {
		baOutStream = new ByteArrayOutputStream();
		dout = new DataOutputStream(new BufferedOutputStream(baOutStream));
	}

	public void writeInt(int value) throws IOException{
		dout.writeInt(value);
	}

	public void writeBoolean(boolean value) throws IOException {
		dout.writeBoolean(value);
	}

	public void writeLong(long value) throws IOException {
		dout.writeLong(value);
	}

	public void writeDouble(double value) throws IOException {
		dout.writeDouble(value);
	}

	public void writeInstant(Instant time) throws IOException {
		writeString(time.toString());
	}

	public void writeString(String str) throws IOException {
		byte[] strBytes = str.getBytes();
		dout.writeInt(strBytes.length);
		dout.write(strBytes);
	}

	public void writeHex(String hex) throws IOException {
		writeByteArr(IDUtils.convertHexToBytes(hex));
	}

	public void writeByteArr(byte[] arr) throws IOException {
		dout.writeInt(arr.length);
		dout.write(arr);
	}

	public void writeIntList(List<Integer> list) throws IOException {
		writeInt(list.size());
		for(Integer i : list) {
			writeInt(i);
		}
	}

	public void writeStringList(List<String> list) throws IOException {
		writeInt(list.size());
		for(String str : list) {
			writeString(str);
		}
	}

	public void write1DPeerArray(PeerTriplet[] arr) throws IOException {
		if(arr == null) {
			writeInt(0);
			return;
		}
		writeInt(arr.length);
		for(PeerTriplet peer : arr) {
			if(peer == null) writeBoolean(false);
			else {
				writeBoolean(true);
				peer.writeToStream(this);
			}
		}
	}

	public void write1dPeerArrList(List<PeerTriplet[]> list) throws IOException {
		writeInt(list.size());
		for(PeerTriplet[] arr : list) {
			write1DPeerArray(arr);
		}
	}

	public void write2DPeerArray(PeerTriplet[][] array) throws IOException {
		writeInt(array.length);
		for(PeerTriplet[] arr : array) {
			write1DPeerArray(arr);
		}
	}

	public void marshallIntStringInt(int value1, String str, int value2) throws IOException {
		writeInt(value1);
		writeString(str);
		writeInt(value2);
	}

	public byte[] getMarshalledData() throws IOException {
		dout.flush();
		byte[] marshalledData = baOutStream.toByteArray();
		baOutStream.close();
		dout.close();
		return marshalledData;
	}
}