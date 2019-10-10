package cs555.p2p.node.util;

import cs555.p2p.util.PeerTriplet;
import cs555.p2p.util.Utils;

public class Logging {
	public static String formatLeafSet(PeerTriplet[] leftLeafSet, PeerTriplet[] rightLeafset) {
		StringBuilder builder = new StringBuilder();
		builder.append("Left Leaf Set: ");
		builder.append('[');
		for(PeerTriplet peer : leftLeafSet) {
			if(peer != null) {
				builder.append(peer.identifier);
				builder.append(' ');
			}
		}
		builder.append("]\n");
		builder.append("Right Leaf Set: ");
		builder.append('[');
		for(PeerTriplet peer : rightLeafset) {
			if(peer != null) {
				builder.append(peer.identifier);
				builder.append(' ');
			}
		}
		builder.append("]\n");

		return builder.toString();
	}

	public static String formatRoutingTable(PeerTriplet[][] routingTable) {
		StringBuilder builder = new StringBuilder();
		builder.append('\n');

		for(int i = 0; i < 17; i++) {
			builder.append("+-------");
		}
		builder.append("+\n");
		builder.append(Utils.formatString("|",8));
		builder.append('|');
		for(int i = 0; i < 16; i++) {
			builder.append(Utils.formatString("Col " + i, 7));
			builder.append('|');
		}
		builder.append('\n');
		for(int row = 0; row < routingTable.length; row++) {
			builder.append(Utils.formatString("|Row " + row, 8));
			builder.append('|');
			if(routingTable[row] != null) {
				for (PeerTriplet peer : routingTable[row]) {
					if (peer != null)
						builder.append(Utils.formatString(peer.identifier, 7));
					else builder.append(Utils.formatString("",7));
					builder.append('|');
				}
			}else {
				for(int i = 0; i < 16; i++) {
					builder.append(Utils.formatString("", 7));
					builder.append('|');
				}
			}
			builder.append('\n');
		}
		for(int i = 0; i < 17; i++) {
			builder.append("+-------");
		}
		builder.append("+\n");

		return builder.toString();
	}
}
