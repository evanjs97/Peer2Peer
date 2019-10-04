package cs555.p2p.util;

import java.math.BigInteger;
import java.rmi.UnexpectedException;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
		String str =  ""+Instant.now().getNano();
		String hex = null;
		try {
			hex = SHAChecksum(str.getBytes());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (DigestException e) {
			e.printStackTrace();
		}
		if(hex == null) return "";
		switch (size) {
			case ID_SHORT:
				return hex.substring(hex.length()-4);
			case ID_INT:
				return hex;
			default:
				return hex;
		}
	}

	public static String SHAChecksum(byte[] bytes) throws NoSuchAlgorithmException, DigestException {
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
		return hashToHexString(messageDigest.digest(bytes));
	}

	public static String hashToHexString(byte[] hash) {
		BigInteger hashNumber = new BigInteger(1, hash);

		StringBuilder builder = new StringBuilder(hashNumber.toString(16));

		while (builder.length() < 40) {
			builder.insert(0, '0');
		}

		return builder.toString();
	}

	public static boolean hostIsCloser(String id1, String id2, String destID)  {
//		System.out.println("COMPARING HOSTS: " + id1 + "\t" + id2 + "\t" + destID);
		int c12 = id1.compareTo(id2);
		int dest1 = destID.compareTo(id1);
		int dest2 = destID.compareTo(id2);
		//id1 is before 2 and dest is greater than 1
//		System.out.println("COMPARISONS: " + c12 + "\t" + dest1 + "\t" + dest2);
		if((c12 > 0 && (dest1 >= 0 || dest2 < 0)) || (c12 < 0 && dest2 < 0)) return true;
		else return false;
//		throw new UnexpectedException("Error Host IDs are matching");
	}

	public enum ID_COMPARE{
		LEFT,
		RIGHT,
		FIRST,
		LAST;
	}
	public static int compareIDS(String id1, String id2, ID_COMPARE comparison) {
		if(comparison == ID_COMPARE.FIRST) return id1.compareTo(id2);
		else return id2.compareTo(id1);
	}

	public static boolean betterLeftChild(String currentLeft, String newLeft, String nodeID) {
		int currNew = currentLeft.compareTo(newLeft);
		int newID = newLeft.compareTo(nodeID);
		int currID = currentLeft.compareTo(nodeID);
		System.out.println("COMPARING LEFT: " + currentLeft + "\t" + newLeft + "\t" + nodeID + "\t" + currNew + "\t" + newID + "\t" + currID);
		if((currNew > 0 && newID > 0) || (currNew < 0 && currID < 0 && newID > 0)) return true;
		return false;
	}

	public static boolean betterRightChild(String currentRight, String newRight, String nodeID) {
		int currNew = currentRight.compareTo( newRight);
		int newID = newRight.compareTo(nodeID);
		int currID = currentRight.compareTo(nodeID);
		System.out.println("COMPARING RIGHT: " + currentRight + "\t" + newRight + "\t" + nodeID + "\t" + currNew + "\t" + newID + "\t" + currID);
		if((currNew < 0 && newID < 0) || (currNew > 0 && currID > 0 && newID < 0) || (currNew < 0 && currID > 0)) return true;
		return false;
	}

	public static boolean betterChild(String current, String newID, String nodeID, ID_COMPARE comparison) {
		if(comparison == ID_COMPARE.LEFT) return betterLeftChild(current, newID, nodeID);
		else return betterRightChild(current, newID, nodeID);
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
