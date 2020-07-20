package peersim.kademlia;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Map;
import java.util.Random;

/**
 * Gives an implementation for the routing table component of a kademlia node
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class RoutingTable implements Cloneable {

	// node ID of the node
	protected BigInteger nodeId = null;

	// k-buckets
	protected TreeMap<Integer, KBucket> k_buckets = null;

	// ______________________________________________________________________________________________
	/**
	 * instanciates a new empty routing table with the specified size
	 */
	public RoutingTable() {
		k_buckets = new TreeMap<Integer, KBucket>();
		// initialize k-bukets
		for (int i = 0; i <= KademliaCommonConfig.NBUCKETS; i++) {
			k_buckets.put(i, new KBucket());
		}
	}

	// add a neighbour to the correct k-bucket
	public void addNeighbour(BigInteger node) {
		// get the lenght of the longest common prefix (correspond to the correct k-bucket)
		int prefix_len = Util.prefixLen(nodeId, node);

		// add the node to the k-bucket
		k_buckets.get(prefix_len).addNeighbour(node);
	}

	// remove a neighbour from the correct k-bucket
	public void removeNeighbour(BigInteger node) {
		// get the lenght of the longest common prefix (correspond to the correct k-bucket)
		int prefix_len = Util.prefixLen(nodeId, node);

		// add the node to the k-bucket
		k_buckets.get(prefix_len).removeNeighbour(node);
	}

	// return the neighbours with a specific common prefix len
	public BigInteger[] getNeighbours (final int dist) {
		BigInteger[] result = new BigInteger[0];
		return k_buckets.get(dist).neighbours.toArray(result);
		//return k_buckets.get(dist).neighbours.keySet().toArray(result);
	}

	// return the closest neighbour to a key from the correct k-bucket
	public BigInteger[] getNeighbours(final BigInteger key, final BigInteger src) {
		// resulting neighbours
		BigInteger[] result = new BigInteger[KademliaCommonConfig.K];

		// neighbour candidates
		ArrayList<BigInteger> neighbour_candidates = new ArrayList<BigInteger>();

		// get the lenght of the longest common prefix
		int prefix_len = Util.prefixLen(nodeId, key);

		// return the k-bucket if is full
		if (k_buckets.get(prefix_len).neighbours.size() >= KademliaCommonConfig.K) {
			//return k_buckets.get(prefix_len).neighbours.keySet().toArray(result);
			return k_buckets.get(prefix_len).neighbours.toArray(result);

		}

		// else get k closest node from all k-buckets
		prefix_len = 0;
		while (prefix_len < KademliaCommonConfig.ALPHA) {
			//neighbour_candidates.addAll(k_buckets.get(prefix_len).neighbours.keySet());
			neighbour_candidates.addAll(k_buckets.get(prefix_len).neighbours);
			// remove source id
			neighbour_candidates.remove(src);
			prefix_len++;
		}

		// create a map (distance, node)
		TreeMap<BigInteger, BigInteger> distance_map = new TreeMap<BigInteger, BigInteger>();

		for (BigInteger node : neighbour_candidates) {
			distance_map.put(Util.distance(node, key), node);
		}

		int i = 0;
		for (BigInteger iii : distance_map.keySet()) {
			if (i < KademliaCommonConfig.K) {
				result[i] = distance_map.get(iii);
				i++;
			}
		}

		return result;
	}

	// ______________________________________________________________________________________________
	public Object clone() {
		RoutingTable dolly = new RoutingTable();
		for (int i = 0; i < KademliaCommonConfig.NBUCKETS; i++) {
			k_buckets.put(i, new KBucket());// (KBucket) k_buckets.get(i).clone());
		}
		return dolly;
	}

	// ______________________________________________________________________________________________
	/**
	 * print a string representation of the table
	 * 
	 * @return String
	 */
	public String toString() {
		return "";
	}
	
	/**
	 * Check nodes and replace buckets with valid nodes from replacement list
	 * 
	 */
	public void refreshBuckets() {
		Random rnd= new Random();
		for(int i=0;i<KademliaCommonConfig.NBUCKETS;i++) {
			KBucket b = k_buckets.get(rnd.nextInt(KademliaCommonConfig.NBUCKETS));
			if(b.neighbours.size()>0) {
				b.checkAndReplaceLast();
				return;
			}
		}
	}
	// ______________________________________________________________________________________________

} // End of class
// ______________________________________________________________________________________________
