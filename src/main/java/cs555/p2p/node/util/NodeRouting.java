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
			if(routingTable[row][col] != null) {
				if(ThreadLocalRandom.current().nextInt(2) == 1)routingTable[row][col] = entry;
			}else routingTable[row][col] = entry;
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

//	private PeerTriplet checkLeftRoutingTable(int row, int col, int minDistance, String destID) {
//		try {
//			routingTableLock.acquire(1);
//			if(routingTable[row] == null) routingTable[row] = new PeerTriplet[16];
//			for(int i = col; i > 0; i--) {
//				if (routingTable[row][i] == null) {
//					continue;
//				}
//				int rightDistance = IDUtils.rightDistance(destID, routingTable[row][i].identifier);
//				if (rightDistance < minDistance) {
//					routingTableLock.release(1);
//					return routingTable[row][i];
//				}
//			}
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		routingTableLock.release(1);
//		return null;
//	}
//
//	private PeerTriplet checkRightRoutingTable(int row, int col, int minDistance, String destID) {
//		try {
//			routingTableLock.acquire(1);
//			if(routingTable[row] == null) routingTable[row] = new PeerTriplet[16];
//			for(int i = col; i < routingTable[row].length; i++) {
//				if (routingTable[row][i] == null) {
//					continue;
//				}
//				int leftDistance = IDUtils.leftDistance(destID, routingTable[row][i].identifier);
//				if (leftDistance < minDistance) {
//					routingTableLock.release(1);
//					return routingTable[row][i];
//				}
//			}
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		routingTableLock.release(1);
//		return null;
//	}

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

//	private PeerTriplet findMinInRow(int row, int startCol, int endCol, int minDistance, String destID, boolean right) {
//		PeerTriplet minimum = null;
//		try {
//
//			routingTableLock.acquire(1);
//			if(routingTable[row] == null) routingTable[row] = new PeerTriplet[16];
//			for(int i = startCol; i < endCol; i++) {
//				if (routingTable[row][i] == null) {
//					continue;
//				}
//				int distance;
//				if(!right) {
//					distance = IDUtils.leftDistance(destID, routingTable[row][i].identifier);
//				}else {
//					distance = IDUtils.rightDistance(destID, routingTable[row][i].identifier);
//				}
////				}else {
////					distance = Math.min(IDUtils.rightDistance(routingTable[row][i].identifier, destID), IDUtils.leftDistance(routingTable[row][i].identifier, destID));
////				}
//				LOGGER.info("DISTANCE: for node: " + routingTable[row][i].identifier+ " OLD: " + minDistance + " NEW: " + distance + " RIGHT: " + right);
//				if (distance < minDistance) {
//
////					routingTableLock.release(1);
//					minimum = routingTable[row][i];
//					minDistance = distance;
//				}
//			}
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		routingTableLock.release(1);
//		return minimum;
//	}

//	private PeerTriplet findPeerInLeafSet(PeerTriplet[] leafSet, String destID, int minDistance, boolean left) {
////		int minDistance = IDUtils.rightDistance(destID, identifier);
//		try {
//			leafSetLock.acquire(1);
//			for(int i = LEAF_SET_SIZE-1; i >= 0; i--) {
//				if(leafSet[i].identifier.equals(identifier)) continue;
//				int distance;
//				if(!left) distance = IDUtils.rightDistance(leafSet[i].identifier, destID);
//				else distance = IDUtils.leftDistance(leafSet[i].identifier, destID);
//				if(minDistance > distance) {
////					System.out.println(leafSet[i].identifier);
//					leafSetLock.release(1);
//					return leafSet[i];
//				}
//			}
//
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		leafSetLock.release(1);
//		return null;
//	}

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

//	public PeerTriplet findClosestPeer(int startRow, int startCol, String destID) {
//		int currentRightDist = IDUtils.rightDistance(identifier, destID);
//		int currentLeftDist = IDUtils.leftDistance(identifier, destID);
//		int minDist = Math.min(currentLeftDist, currentRightDist);
//
//		boolean useRight = currentRightDist > currentLeftDist;
//		PeerTriplet left = findMinInLeafSet(leftLeafSet, destID, minDist, useRight);
//		PeerTriplet right = findMinInLeafSet(rightLeafset, destID, minDist, useRight);
//
//		int leftDistance = Integer.MAX_VALUE;
//		if(left != null) leftDistance = useRight ? IDUtils.rightDistance(destID, left.identifier) : IDUtils.leftDistance(destID, left.identifier);
//		int rightDistance = Integer.MAX_VALUE;
//		if(right != null) rightDistance = useRight ? IDUtils.rightDistance(destID, right.identifier) : IDUtils.leftDistance(destID, right.identifier);
//
//		int stopCol = Integer.parseInt(identifier.substring(startRow, startRow+1), 16);
//		LOGGER.info("FINDING MIN IN ROW: " + startRow + " COL: " + startCol);
//		PeerTriplet minimum = leftDistance < rightDistance && leftDistance < minDist ?
//				left : rightDistance < leftDistance && rightDistance < minDist ? right : null;
//		minDist = Math.min(minDist, Math.min(leftDistance, rightDistance));
//
//		left = findMinInRow(startRow, 0, startCol, minDist, destID, useRight);
//		right = findMinInRow(startRow, startCol, routingTable.length, minDist, destID, useRight);
////		left = checkLeftRoutingTable(startRow, startCol, minDist, destID);
////		right = checkRightRoutingTable(startRow, startCol, minDist, destID);
//
//		if(left != null) leftDistance = useRight ? IDUtils.rightDistance(destID,left.identifier) : IDUtils.leftDistance(destID, left.identifier);
//		else leftDistance = Integer.MAX_VALUE;
//		if(right != null) rightDistance = useRight ? IDUtils.rightDistance(destID,right.identifier) : IDUtils.leftDistance(destID, right.identifier);
//		else rightDistance = Integer.MAX_VALUE;
//
////		LOGGER.info("LEFT: " + left + " dist: " + leftDistance + " RIGHT: " + right + " dist: " + rightDistance);
//		if(leftDistance < rightDistance && leftDistance < minDist) {
//			return left;
//		}
//		if(rightDistance < leftDistance && rightDistance < minDist) {
//			return right;
//		}
//
//		return minimum;
//	}

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

//	public PeerTriplet findRoutingDest(int startRow, int startCol, String destID) {
//		int currentRightDist = IDUtils.rightDistance(identifier, destID);
//		int currentLeftDist = IDUtils.leftDistance(identifier, destID);
//		PeerTriplet peer;
//		if(currentLeftDist <= currentRightDist) {
//			peer = findPeerInLeafSet(leftLeafSet, destID, currentLeftDist, true);
//			if(peer != null) currentLeftDist = IDUtils.leftDistance(peer.identifier, destID);
//		}
//		else {
//			peer = findPeerInLeafSet(rightLeafset, destID, currentRightDist, false);
//			if(peer != null) currentRightDist = IDUtils.rightDistance(peer.identifier, destID);
//		}
//
//		if(peer == null) return null;
//
//		int direction = currentLeftDist <= currentRightDist ? -1 : 1;
//		PeerTriplet destLeft = findMinInRow(startRow, 0, startCol, Math.min(currentLeftDist, currentRightDist), destID, currentRightDist > currentLeftDist);
//		PeerTriplet destRight = findMinInRow(startRow, startCol+1, routingTable.length, Math.min(currentLeftDist, currentRightDist), destID, currentRightDist > currentLeftDist);
////		PeerTriplet destLeft = checkRightRoutingTable(startRow, startCol, Math.min(currentLeftDist, currentRightDist), destID);
////		PeerTriplet destRight = checkLeftRoutingTable(startRow, startCol, Math.min(currentLeftDist, currentRightDist), destID);
//
//		if(destLeft != null && destRight != null) {
//			if(currentLeftDist <= currentRightDist) {
//				int rightDist = IDUtils.leftDistance(destRight.identifier, destID);
//				int leftDist = IDUtils.leftDistance(destLeft.identifier, destID);
//				return leftDist < rightDist ? destLeft : destRight;
//			}else {
//				int rightDist = IDUtils.rightDistance(destRight.identifier, destID);
//				int leftDist = IDUtils.rightDistance(destLeft.identifier, destID);
//				return leftDist < rightDist ? destLeft : destRight;
//			}
//		}else if(destLeft != null) return destLeft;
//		else if(destRight != null) return destRight;
//
//		return peer;
//	}

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
			routingTableLock.acquire(1);
			if(routingTable[row][col] == null) return false;
			if(identifier.equals(routingTable[row][col].identifier)) {
				routingTable[row][col] = null;
				printRoutingTable();
				routingTableLock.release(1);
				return true;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		routingTableLock.release(1);
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
