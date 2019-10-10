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
			routingTable[row][col] = entry;
			routingTableLock.release(MAX_THREADS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		printRoutingTable();
	}

	public void addEntryToRowIfNonEmpty(PeerTriplet entry, int row) {
		int col = Integer.parseInt(entry.identifier.substring(row, row+1), 16);
		try {
			routingTableLock.acquire(MAX_THREADS);
			if(routingTable[row] == null) {
				routingTable[row] = new PeerTriplet[16];
			}
			if(routingTable[row][col] == null) routingTable[row][col] = entry;
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

	private PeerTriplet checkLeftRoutingTable(int row, int col, int minDistance, String destID) {
		try {
			routingTableLock.acquire(1);
			if(routingTable[row] == null) routingTable[row] = new PeerTriplet[16];
			for(int i = col; i > 0; i--) {
				if (routingTable[row][i] == null) {
					continue;
				}
				int rightDistance = IDUtils.rightDistance(destID, routingTable[row][i].identifier);
				if (rightDistance < minDistance) {
					routingTableLock.release(1);
					return routingTable[row][i];
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		routingTableLock.release(1);
		return null;
	}

	private PeerTriplet checkRightRoutingTable(int row, int col, int minDistance, String destID) {
		try {
			routingTableLock.acquire(1);
			if(routingTable[row] == null) routingTable[row] = new PeerTriplet[16];
			for(int i = col; i < routingTable[row].length; i++) {
				if (routingTable[row][i] == null) {
					continue;
				}
				int leftDistance = IDUtils.leftDistance(destID, routingTable[row][i].identifier);
				if (leftDistance < minDistance) {
					routingTableLock.release(1);
					return routingTable[row][i];
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		routingTableLock.release(1);
		return null;
	}

	private PeerTriplet findPeerInLeafSet(PeerTriplet[] leafSet, String destID, int minDistance, boolean left) {
//		int minDistance = IDUtils.rightDistance(destID, identifier);
		try {
			leafSetLock.acquire(1);
			for(int i = LEAF_SET_SIZE-1; i >= 0; i--) {
				if(leafSet[i].identifier.equals(identifier)) continue;
				int distance;
				if(!left) distance = IDUtils.rightDistance(leafSet[i].identifier, destID);
				else distance = IDUtils.leftDistance(leafSet[i].identifier, destID);
				if(minDistance > distance) {
//					System.out.println(leafSet[i].identifier);
					leafSetLock.release(1);
					return leafSet[i];
				}
			}

		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		leafSetLock.release(1);
		return null;
	}

	public PeerTriplet findRoutingDest(int startRow, int startCol, String destID) {
		int currentRightDist = IDUtils.rightDistance(identifier, destID);
		int currentLeftDist = IDUtils.leftDistance(identifier, destID);
		PeerTriplet peer;
		if(currentLeftDist <= currentRightDist) peer = findPeerInLeafSet(leftLeafSet, destID, currentLeftDist, true);
		else peer = findPeerInLeafSet(rightLeafset, destID, currentRightDist, false);

		if(peer == null) return null;

		PeerTriplet dest;
		if(currentLeftDist <= currentRightDist) {
			 dest = checkLeftRoutingTable(startRow, startCol, currentLeftDist, destID);
		}else {
			dest = checkRightRoutingTable(startRow, startCol, currentRightDist, destID);
		}

		return dest == null ? peer : dest;
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
