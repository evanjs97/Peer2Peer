package cs555.p2p.node.util;

import cs555.p2p.util.IDUtils;
import cs555.p2p.util.PeerTriplet;

import java.util.concurrent.Semaphore;
import java.util.logging.Logger;


public class NodeRouting {
	private final static Logger LOGGER = Logger.getLogger(NodeRouting.class.getName());
	private final static int MAX_THREADS = 100;
	private final static int LEAF_SET_SIZE = 1;
	Semaphore routingTableLock;
	Semaphore leafSetLock;
	private final PeerTriplet[][] routingTable;
	private final PeerTriplet[] rightLeafset;
	private final PeerTriplet[] leftLeafSet;
	private final String identifier;

	public PeerTriplet[][] getRoutingTable() {
		return routingTable;
	}

	public PeerTriplet[] getRoutingRow(int row) {
		return routingTable[row];
	}

	public PeerTriplet[] getRightLeafset() {
		return rightLeafset;
	}

	public PeerTriplet[] getLeftLeafSet() {
		return leftLeafSet;
	}

	public NodeRouting(int tableRows, int tableCols, String identifier) {
		this(new PeerTriplet[tableRows][tableCols], new PeerTriplet[LEAF_SET_SIZE], new PeerTriplet[LEAF_SET_SIZE], identifier);
	}

	public NodeRouting(PeerTriplet[][] routingTable, PeerTriplet[] rightLeafset, PeerTriplet[] leftLeafSet, String identifier) {
		this.routingTableLock = new Semaphore(MAX_THREADS);
		this.leafSetLock = new Semaphore(MAX_THREADS);
		this.routingTable = routingTable;
		this.rightLeafset = rightLeafset;
		this.leftLeafSet = leftLeafSet;
		this.identifier = identifier;
	}

	public void aquireTable(){
		try {
			routingTableLock.acquire(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void releaseTable() {
		routingTableLock.release(1);
	}

	public void aquireLeafSet() {
		try {
			leafSetLock.acquire(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void releaseLeafSet() {
		leafSetLock.release(1);
	}

	public void addEntryToRow(PeerTriplet entry, int row) {
		int col = Integer.parseInt(entry.identifier.substring(row, row+1), 16);
		try {
			routingTableLock.acquire(MAX_THREADS);
			if(routingTable[row] == null) {
				routingTable[row] = new PeerTriplet[16];
			}
			routingTable[row][col] = entry;
			routingTableLock.release(MAX_THREADS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		printRoutingTable();
	}

	public void overwriteRoutingTable(PeerTriplet[][] table, PeerTriplet[] rightLeafset, PeerTriplet[] leftLeafSet) {
		try {
			routingTableLock.acquire(MAX_THREADS);
			for(int row = 0; row < table.length; row++) {
				this.routingTable[row] = table[row];
			}
			routingTableLock.release(MAX_THREADS);

			leafSetLock.acquire(MAX_THREADS);
			for(int col = 0; col < LEAF_SET_SIZE; col++) {
				if(leftLeafSet[col] != null) {
					this.leftLeafSet[col] = leftLeafSet[col];
				}
				if(rightLeafset[col] != null) this.rightLeafset[col] = rightLeafset[col];
			}
			leafSetLock.release(MAX_THREADS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
//		printRoutingTable();

	}

	public void printRoutingTable() {
		try {
			routingTableLock.acquire(1);
			LOGGER.info(Logging.formatRoutingTable(routingTable));
			routingTableLock.release(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void printLeafSet() {
		try {
			leafSetLock.acquire(1);
			LOGGER.info(Logging.formatLeafSet(leftLeafSet, rightLeafset));
			leafSetLock.release(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private PeerTriplet checkUpperTable(int startRow, int startCol, String destID) {
		int minDistance = IDUtils.rightDistance(destID, identifier);
		PeerTriplet minPeer = null;
		while(startRow >= 0) {
			if(routingTable[startRow] == null) {
				routingTable[startRow] = new PeerTriplet[16];
			}
			for(int i = startCol; i >= 0; i--) {
				if(routingTable[startRow][i] == null) {
					continue;
				}
				int rightDistance = IDUtils.rightDistance(destID, routingTable[startRow][i].identifier);
				if(rightDistance < minDistance) {
					minDistance = rightDistance;
					minPeer = routingTable[startRow][i];
					if(minPeer.identifier.compareTo(destID) < 0) return minPeer;
				}
			}
			startRow--;
			startCol = 15;
		}
		return minPeer;
	}

	private PeerTriplet checkLowerTable(int startRow, int startCol, String destID) {
		int minDistance = IDUtils.rightDistance(destID, identifier);
		PeerTriplet minPeer = null;
		while(startRow < routingTable.length) {
			if(routingTable[startRow] == null) {
				routingTable[startRow] = new PeerTriplet[16];
			}
			for(int i = startCol; i < routingTable[startRow].length; i++) {
				if(routingTable[startRow][i] == null) {
					continue;
				}
				int rightDistance = IDUtils.rightDistance(destID, routingTable[startRow][i].identifier);
				if(rightDistance < minDistance) {
					minDistance = rightDistance;
					minPeer = routingTable[startRow][i];
				}
			}
			startRow++;
			startCol = 0;
		}
		return minPeer;
	}

	private PeerTriplet findPeerInLeafSet(PeerTriplet[] leafSet, String destID) {
		int minDistance = IDUtils.rightDistance(destID, identifier);
		for(int i = LEAF_SET_SIZE-1; i >= 0; i--) {
			if(leafSet[i].identifier.equals(identifier)) continue;
			if(minDistance > IDUtils.rightDistance(destID, leafSet[i].identifier) && leafSet[i].identifier.compareTo(destID) <= 0) {
				System.out.println(leafSet[i].identifier);
				return leafSet[i];
			}
		}
		return null;
	}

	private PeerTriplet checkLeafSet(String destID) {
		PeerTriplet peer = null;
		try {
			leafSetLock.acquire(1);
			if(destID.compareTo(identifier) > 0) {
				peer = findPeerInLeafSet(leftLeafSet, destID);
			}else {
				peer =  findPeerInLeafSet(rightLeafset, destID);
			}
			leafSetLock.release(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return peer;
	}

	public PeerTriplet findRoutingDest(int startRow, int startCol, String originHost, int originPort, String destID) {
		PeerTriplet right = checkUpperTable(startRow, startCol, destID);
		PeerTriplet left = checkLowerTable(startRow, startCol, destID);
		PeerTriplet leaf = checkLeafSet(destID);

		if(left == null && right == null) {
			return leaf;
		}
		if(right == null) {
			return left;
		}
		if(left == null) {
			return right;
		}

		int rightDist = IDUtils.rightDistance(destID, right.identifier);
		int leftDist = IDUtils.rightDistance(destID, left.identifier);
		int leafDist = IDUtils.rightDistance(destID, leaf.identifier);

		if(rightDist < leftDist) {
			return rightDist < leafDist ? right : leaf;
		}else {
			return leafDist < leftDist ? leaf : left;
		}

	}

	public boolean addToLeafSet(PeerTriplet peer) {
		try {
			leafSetLock.acquire(MAX_THREADS);
			int newRightDistance = IDUtils.rightDistance(identifier, peer.identifier);
			int newLeftDistance = IDUtils.leftDistance(identifier, peer.identifier);
			boolean found = false;
			for (int i = 0; i < LEAF_SET_SIZE; i++) {
				if(rightLeafset[i] == null || rightLeafset[i].identifier.equals(identifier) ||
						(newRightDistance < IDUtils.rightDistance(identifier, rightLeafset[i].identifier) && !found)) {
					rightLeafset[i] = peer;
					found = true;
				}
				if(leftLeafSet[i] == null || leftLeafSet[i].identifier.equals(identifier) ||
						(newLeftDistance < IDUtils.leftDistance(identifier, leftLeafSet[i].identifier) && !found)) {
					leftLeafSet[i] = peer;
					found = true;
				}
			}
			leafSetLock.release(MAX_THREADS);
			if(found) printLeafSet();
			return found;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		leafSetLock.release(MAX_THREADS);
		return false;
	}

}
