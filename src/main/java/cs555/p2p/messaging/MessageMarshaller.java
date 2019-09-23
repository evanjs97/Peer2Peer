package cs555.p2p.messaging;

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