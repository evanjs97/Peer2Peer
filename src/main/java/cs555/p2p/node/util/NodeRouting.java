package cs555.p2p.node.util;

import cs555.p2p.util.IDUtils;
import cs555.p2p.util.PeerTriplet;

import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;


public class NodeRouting {
	private final static Logger LOGGER = Logger.getLogger(NodeRouting.class.getName());
	private final static int MAX_THREADS = 100;
	private final static int LEAF_SET_SIZE = 1;
	private final Semaphore routingTableLock;
	private final Semaphore leafSetLock;
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

	public PeerTriplet getLeftNeighbor() {
		return leftLeafSet[0];
	}

	public PeerTriplet getRightNeighbor() {
		return rightLeafset[0];
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
			if(routingTable[row][col] != null) {
				if(ThreadLocalRandom.current().nextInt(2) == 1)routingTable[row][col] = entry;
			}else routingTable[row][col] = entry;
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

	private PeerTriplet findClosest(int minDistance, String destID, boolean left) {
		PeerTriplet minimum = null;
		try {
			routingTableLock.acquire(1);
			for(int i = 0; i < routingTable.length; i++) {
				if(routingTable[i] == null) routingTable[i] = new PeerTriplet[16];
				for(int j = 0; j < routingTable[i].length; j++) {
					if(routingTable[i][j] == null) continue;
					int distance;
					if(left) {
						distance = IDUtils.leftDistance(destID, routingTable[i][j].identifier);
					}else {
						distance = IDUtils.rightDistance(destID, routingTable[i][j].identifier);
					}
					if(distance < minDistance) {
						minDistance = distance;
						minimum = routingTable[i][j];
					}
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		routingTableLock.release(1);
		return minimum;
	}

	public PeerTriplet findMinInLeafSet(PeerTriplet[] leafSet, String destID, int minDistance, boolean left) {
		PeerTriplet minimum = null;
		try {
			leafSetLock.acquire(1);
			for(int i = LEAF_SET_SIZE-1; i >= 0; i--) {
				if(leafSet[i].identifier.equals(identifier)) continue;
				int distance = !left ? IDUtils.rightDistance(destID, leafSet[i].identifier) : IDUtils.leftDistance(destID, leafSet[i].identifier);

				if(minDistance > distance) {
					minimum = leafSet[i];
					minDistance = distance;
				}
			}

		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		leafSetLock.release(1);
		return minimum;
	}


	public boolean isRightCloser(String destID) {
		try {
			leafSetLock.acquire(1);
			if(rightLeafset[0].identifier.equals(identifier)) return false;
			if(IDUtils.leftDistance(destID, identifier) > IDUtils.rightDistance(destID, rightLeafset[0].identifier)) {
				leafSetLock.release(1);
				return true;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		leafSetLock.release(1);
		return false;
	}

	public boolean isLeftCloser(String destID) {
		try {
			leafSetLock.acquire(1);
			if(leftLeafSet[0].identifier.equals(identifier)) return false;
			if(IDUtils.rightDistance(destID, identifier) >= IDUtils.leftDistance(destID, leftLeafSet[0].identifier)) {
				leafSetLock.release(1);
				return true;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		leafSetLock.release(1);
		return false;
	}

	public PeerTriplet findClosestPeer(String destID) {
		int currentRightDist = IDUtils.rightDistance(destID, identifier);
		int currentLeftDist = IDUtils.leftDistance(destID, identifier);

		boolean isLeft = currentLeftDist < currentRightDist;
		int minDistance = Math.min(currentLeftDist, currentRightDist);
		PeerTriplet left = findMinInLeafSet(leftLeafSet, destID, minDistance, isLeft);
		PeerTriplet right = findMinInLeafSet(rightLeafset, destID, minDistance, isLeft);

		PeerTriplet table = findClosest(minDistance, destID, isLeft);

		if(table != null) return table;

		int leftDist = Integer.MAX_VALUE;
		if(left != null) leftDist = isLeft ? IDUtils.leftDistance(destID, left.identifier) : IDUtils.rightDistance(destID, left.identifier);
		int rightDist = Integer.MAX_VALUE;
		if(right != null) rightDist = isLeft ? IDUtils.leftDistance(destID, right.identifier) : IDUtils.rightDistance(destID, right.identifier);

		if(leftDist <= rightDist) return left;
		else return right;
	}

	public PeerTriplet findDataRoute(String destID) {
		PeerTriplet peer = findClosestPeer(destID);
		if(peer == null) {
			if(IDUtils.leftDistance(destID, identifier) < IDUtils.rightDistance(destID, identifier)) {
				if(isRightCloser(destID)) return rightLeafset[0];
			}else {
				if (isLeftCloser(destID)) return leftLeafSet[0];
			}

		}
		return peer;
	}

	public boolean removeEntry(String identifier) {
		int row = IDUtils.firstNonMatchingIndex(this.identifier, identifier);
		int col = Integer.parseInt(identifier.substring(row, row+1), 16);
		try {
			routingTableLock.acquire(MAX_THREADS);
			if(routingTable[row][col] == null) return false;
			if(identifier.equals(routingTable[row][col].identifier)) {
				routingTable[row][col] = null;
				routingTableLock.release(MAX_THREADS);
				printRoutingTable();
				return true;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		routingTableLock.release(MAX_THREADS);
		LOGGER.info("REMOVED ENTRY");
		return false;
	}

	public boolean addToLeafSet(PeerTriplet peer) {
		try {
			int newRightDistance = IDUtils.rightDistance(identifier, peer.identifier); //6F53-2716
			int newLeftDistance = IDUtils.leftDistance(identifier, peer.identifier); //FFFF-6F53+2716
			boolean found = false;
			leafSetLock.acquire(MAX_THREADS);

			for (int i = 0; i < LEAF_SET_SIZE; i++) {
				int rightDistance = Integer.MAX_VALUE;
				if(rightLeafset[i] != null) rightDistance = IDUtils.rightDistance(identifier, rightLeafset[i].identifier);
				if(rightLeafset[i] == null || rightLeafset[i].identifier.equals(identifier) ||
						(newRightDistance < rightDistance && !found)) {
					rightLeafset[i] = peer;
					found = true;
				}
				int leftDistance = Integer.MAX_VALUE;
				if(leftLeafSet[i] != null) leftDistance = IDUtils.leftDistance(identifier, leftLeafSet[i].identifier);
				if(leftLeafSet[i] == null || leftLeafSet[i].identifier.equals(identifier) ||
						(newLeftDistance < leftDistance && !found)) {
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
