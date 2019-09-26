package cs555.p2p.util;

import java.time.Instant;

public class IDUtils {

	public enum ID_SIZE {
		ID_SHORT,
		ID_INT
	}

	/**
	 * This method generates an id based off the current time
	 * @param size the size of the id to generate either ID_SHORT(16 bits) or ID_LONG(32 bits)
	 * @return the newly generated id
	 */
	public static String generateIDByTimestamp(ID_SIZE size) {
		long time = Instant.now().getEpochSecond();
		String hex =  Long.toHexString(time);

		switch (size) {
			case ID_SHORT:
				return hex.substring(4);
			case ID_INT:
				return hex;
			default:
				return hex;
		}
	}

	/**
	 * This method converts a set of bytes into a Hexadecimal representation.
	 *
	 * @param buf
	 * @return
	 */
	public static String convertBytesToHex(byte[] buf) {
		StringBuffer strBuf = new StringBuffer();
		for (int i = 0; i < buf.length; i++) {
			int byteValue = (int) buf[i] & 0xff;
			if (byteValue <= 15) {
				strBuf.append("0");
			}
			strBuf.append(Integer.toString(byteValue, 16));
		}
		return strBuf.toString();
	}
	/**
	 * This method converts a specified hexadecimal String into a set of bytes.
	 *
	 * @param hexString
	 * @return
	 */
	public static byte[] convertHexToBytes(String hexString) {
		int size = hexString.length();
		byte[] buf = new byte[size / 2];
		int j = 0;
		for (int i = 0; i < size; i++) {
			String a = hexString.substring(i, i + 2);
			int valA = Integer.parseInt(a, 16);
			i++;
			buf[j] = (byte) valA;
			j++;
		}
		return buf;
	}

	public static int firstNonMatchingIndex(String id1, String id2) {
		if(id1.length() != id2.length())
			throw new UnsupportedOperationException("Node identifiers must be of equal length: id1 length: " + id1.length() + " id2 length: " + id2.length());
		for(int i = 0; i < id1.length(); i++) {
			if(id1.charAt(i) != id2.charAt(i)) return i;
		}
		return id1.length();
	}
}
