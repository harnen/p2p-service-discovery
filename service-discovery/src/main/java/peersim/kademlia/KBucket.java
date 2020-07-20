package peersim.kademlia;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
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
	//protected TreeMap<BigInteger, Long> neighbours = null;
	protected List<BigInteger> neighbours;

	//replacementList
	//protected TreeMap<BigInteger, Long> replacements = null;
	protected List<BigInteger> replacements;

	// empty costructor
	public KBucket() {
		//neighbours = new TreeMap<BigInteger, Long>();
		neighbours = new ArrayList<BigInteger>();
		replacements = new ArrayList<BigInteger>();
	}

	// add a neighbour to this k-bucket
	public void addNeighbour(BigInteger node) {
		//long time = CommonState.getTime();
		for(BigInteger n : neighbours) {
			if(n.compareTo(node)==0)return;
		}
		if (neighbours.size() < KademliaCommonConfig.K) { // k-bucket isn't full
			//neighbours.put(node, time); // add neighbour to the tail of the list
			neighbours.add(0,node);
		} else {
			addReplacement(node);
		}
	}

	// add a replacement node in the list
	public void addReplacement(BigInteger node) {
		//long time = CommonState.getTime();
		
		for(BigInteger r : replacements) {
			if(r.compareTo(node)==0)return;
		}
		replacements.add(0,node);

		if (replacements.size() > KademliaCommonConfig.MAXREPLACEMENT) { // k-bucket isn't full
			//replacements.put(node, time); // add neighbour to the tail of the list
			replacements.remove(replacements.size()-1);
		} 
	}
	// remove a neighbour from this k-bucket
	public void removeNeighbour(BigInteger node) {
		neighbours.remove(node);
	}
	
	// remove a replacement node in the list
	public void removeReplacement(BigInteger node) {
		replacements.remove(node);
	}

	public Object clone() {
		KBucket dolly = new KBucket();
		for (BigInteger node : neighbours) {
			dolly.neighbours.add(new BigInteger(node.toByteArray()));
		}
		return dolly;
	}

	public String toString() {
		String res = "{\n";

		for (BigInteger node : neighbours) {
			res += node + "\n";
		}

		return res + "}";
	}
	
	public void checkAndReplaceLast() {
		
	}
}
