package peersim.kademlia;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class represents a find operation and offer the methods needed to maintain and update the closest set.<br>
 * It also maintains the number of parallel requsts that can has a maximum of ALPHA.
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class Operation {

	/**
	 * unique sequence number generator of the operation
	 */
	private static long OPERATION_ID_GENERATOR = 0;


	public boolean finished = false;
	/**
	 * represent univocally the find operation
	 */
	public long operationId;
	
	/**
	 * Type of the initial message
	 */
	public int type;

    /**
	 * Id of the node to find
	 */
	public BigInteger destNode;

	/**
	 * Body of the original find message
	 */
	public Object body;

	/**
	 * number of available find request message to send (it must be always less than ALPHA)
	 */
	public int available_requests;

	/**
	 * Start timestamp of the search operation
	 */
	protected long timestamp = 0;

	/**
	 * Number of hops the message did
	 */
	protected int nrHops = 0;

	/**
	 * This map contains the K closest nodes and corresponding boolean value that indicates if the nodes has been already queried
	 * or not
	 */
	protected HashMap<BigInteger, Boolean> closestSet;

	/**
	 * defaul constructor
	 * 
	 * @param destNode
	 *            Id of the node to find
	 */
	public Operation(BigInteger dstNode, int type, long timestamp) {
		this.timestamp = timestamp;
		this.destNode = dstNode;
		this.type = type;

		// set a new find id
		operationId = OPERATION_ID_GENERATOR++;

		// set availabe request to ALPHA
		available_requests = KademliaCommonConfig.ALPHA;

		// initialize closestSet
		closestSet = new HashMap<BigInteger, Boolean>();
	}

	/**
	 * update closestSet with the new information received
	 * 
	 * @param neighbours
	 */
	public void elaborateResponse(BigInteger[] neighbours) {
		// update responseNumber
		available_requests++;

		// add to closestSet
		for (BigInteger n : neighbours) {

			if (n != null) {
				if (!closestSet.containsKey(n)) {
					if (closestSet.size() < KademliaCommonConfig.K) { // add directly
						closestSet.put(n, false);
					} else { // find in the closest set if there are nodes whit less distance
						BigInteger newdist = Util.distance(n, destNode);

						// find the node with max distance
						BigInteger maxdist = newdist;
						BigInteger nodemaxdist = n;
						for (BigInteger i : closestSet.keySet()) {
							BigInteger dist = Util.distance(i, destNode);

							if (dist.compareTo(maxdist) > 0) {
								maxdist = dist;
								nodemaxdist = i;
							}
						}

						if (nodemaxdist.compareTo(n) != 0) {
							closestSet.remove(nodemaxdist);
							closestSet.put(n, false);
						}
					}
				}
			}
		}

		/*String s = "closestSet to " + destNode + "\n";
		for (BigInteger clos : closestSet.keySet()) {
			 s+= clos + "-";
		}
		System.out.println(s);*/

	}

	/**
	 * get the first neighbour in closest set which has not been already queried
	 * 
	 * @return the Id of the node or null if there aren't available node
	 */
	public BigInteger getNeighbour() {
		// find closest neighbour ( the first not already queried)
		BigInteger res = null;
		for (BigInteger n : closestSet.keySet()) {
			if (n != null && closestSet.get(n) == false) {
				if (res == null) {
					res = n;
				} else if (Util.distance(n, destNode).compareTo(Util.distance(res, destNode)) < 0) {
					res = n;
				}
			}
		}

		// Has been found a valid neighbour
		if (res != null) {
			closestSet.remove(res);
			closestSet.put(res, true);
			available_requests--; // decrease available request
		}

		return res;
	}

	
	/**
	 * get the neighbours in closest set which has not been already queried
	 * 
	 * @return the closest nodes set up to K
	 */
	public List<BigInteger> getNeighboursList() {
		return new ArrayList<BigInteger>(closestSet.keySet());
		//return new ArrayList<BigInteger>(closestSet.keySet()).subList(0, KademliaCommonConfig.K-1);
	}
	
}
