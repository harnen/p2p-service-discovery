package peersim.kademlia;

import java.math.BigInteger;
import java.util.TreeMap;

import peersim.core.CommonState;

/**
 * This class implements a kademlia k-bucket. Function for the management of the neighbours update are also implemented
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class KBucket implements Cloneable {

	// k-bucket array
	protected TreeMap<BigInteger, Long> neighbours = null;
	
	protected TreeMap<BigInteger, Long> replacement = null;

	//protected TreeMap<String, Long> ips = null;


	// empty costructor
	public KBucket() {
		neighbours = new TreeMap<BigInteger, Long>();
	}

	// add a neighbour to this k-bucket
	public void addNeighbour(BigInteger node) {
		long time = CommonState.getTime();
		if (neighbours.size() < KademliaCommonConfig.K) { // k-bucket isn't full
			neighbours.put(node, time); // add neighbour to the tail of the list
		}
	}
	
	// add a neighbour to this replacement bucket
	public void addReplacement(BigInteger node) {
		long time = CommonState.getTime();
		if (replacement.size() < KademliaCommonConfig.MAX_REPLACEMENTS) { // k-bucket isn't full
			replacement.put(node, time); // add neighbour to the tail of the list
		}
	}


	// remove a neighbour from this k-bucket
	public void removeNeighbour(BigInteger node) {
		neighbours.remove(node);
	}

	// remove a neighbour from the replacement list
	public void removeReplacement(BigInteger node) {
		replacement.remove(node);
	}
	
	public Object clone() {
		KBucket dolly = new KBucket();
		for (BigInteger node : neighbours.keySet()) {
			dolly.neighbours.put(new BigInteger(node.toByteArray()), 0l);
		}
		return dolly;
	}

	public String toString() {
		String res = "{\n";

		for (BigInteger node : neighbours.keySet()) {
			res += node + "\n";
		}

		return res + "}";
	}
}
