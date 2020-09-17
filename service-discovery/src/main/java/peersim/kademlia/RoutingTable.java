package peersim.kademlia;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.TreeMap;

import peersim.core.CommonState;

import java.util.Map;
import java.util.Random;

/**
 * Gives an implementation for the routing table component of a kademlia node
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class RoutingTable implements Cloneable {

	private final static int bucketMinDistance = KademliaCommonConfig.BITS - KademliaCommonConfig.NBUCKETS;

	// node ID of the node
	protected BigInteger nodeId = null;

	// k-buckets
	//protected TreeMap<Integer, KBucket> k_buckets = null;
	protected KBucket k_buckets[];

	//protected KademliaProtocol prot;
	// ______________________________________________________________________________________________
	/**
	 * instanciates a new empty routing table with the specified size
	 */
	public RoutingTable() {
		// initialize k-bukets
		//System.out.println("new routing table");
		k_buckets = new KBucket[KademliaCommonConfig.NBUCKETS];
		for (int i = 0; i < k_buckets.length; i++) {
			k_buckets[i] = new KBucket(this);
		}
		//this.prot=prot;
	}

	public int containsNode(BigInteger id){
		for(int i = 0; i < KademliaCommonConfig.NBUCKETS; i++){
			for(BigInteger nodeId: k_buckets[i].neighbours){
				if(nodeId.equals(id))
					return i;
			}
		}
		return -1;
	}

	// add a neighbour to the correct k-bucket
	public void addNeighbour(BigInteger node) {
		// get the lenght of the longest common prefix (correspond to the correct k-bucket)
		//System.out.println("Bucket add node "+node+" in "+nodeId+" at bucket "+(Util.logDistance(nodeId, node) - bucketMinDistance -1));
		if(node.compareTo(nodeId)==0) return;
		bucket(node).addNeighbour(node);
	}

	// remove a neighbour from the correct k-bucket
	public void removeNeighbour(BigInteger node) {
		// get the lenght of the longest common prefix (correspond to the correct k-bucket)
		bucket(node).removeNeighbour(node);
	}
	

	// return the neighbours with a specific common prefix len
	public BigInteger[] getNeighbours (final int dist) {
		BigInteger[] result = new BigInteger[0];
		ArrayList<BigInteger> resultList = new ArrayList<BigInteger>();
		resultList.addAll(bucketAtDistance(dist).neighbours);
		//System.out.println("Getneighbours "+resultList.size());
		if(resultList.size()<KademliaCommonConfig.K&&dist+1<=256) {
			//System.out.println("Getneighbours "+resultList.size());
			resultList.addAll(bucketAtDistance(dist+1).neighbours);

		}
		if(resultList.size()<KademliaCommonConfig.K&dist+1>=0) {
			//System.out.println("Getneighbours "+resultList.size());
			resultList.addAll(bucketAtDistance(dist-1).neighbours);
		}
		return resultList.toArray(result);

		
	}
	
	public BigInteger[] getKClosestNeighbours (final int k) {
		BigInteger[] result = new BigInteger[k];
		ArrayList<BigInteger> resultList = new ArrayList<BigInteger>();
		int dist=0;
		while(resultList.size()<k&&dist<256) {
			resultList.addAll(bucketAtDistance(dist).neighbours);
			dist++;
		}
		
		return resultList.toArray(result);

	}



	// ______________________________________________________________________________________________
	public Object clone() {
		RoutingTable dolly = new RoutingTable();
		for (int i = 0; i < k_buckets.length; i++) {
			k_buckets[i] = new KBucket(this);
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
	public void refreshBuckets(int kademliaid) {
		//System.out.println("Routingtable refreshBuckkets "+CommonState.getTime());
		Random rnd= new Random();
		for(int i=0;i<KademliaCommonConfig.NBUCKETS;i++) {
			KBucket b = k_buckets[rnd.nextInt(KademliaCommonConfig.NBUCKETS)];
			if(b.neighbours.size()>0) {
				b.checkAndReplaceLast(kademliaid);
				return;
			}
		}
	}
	
	public void sendToFront(BigInteger node)
	{
		if(bucket(node).neighbours.remove(node))
			bucket(node).neighbours.add(0,node);
	}
	
	
	public KBucket bucket(BigInteger node) {
		//return bucketAtDistance(Util.prefixLen(nodeId, node));
		return bucketAtDistance(Util.logDistance(nodeId, node));
	}
	
	private KBucket bucketAtDistance(int distance) {
		if (distance <= bucketMinDistance) {
			//System.out.println("bucket at distance "+distance+" "+bucketMinDistance+" "+0);
			return k_buckets[0];
		}
		//System.out.println("bucket at distance "+distance+" "+(distance - bucketMinDistance - 1));
		return k_buckets[distance - bucketMinDistance - 1];
	}
	// ______________________________________________________________________________________________

} // End of class
// ______________________________________________________________________________________________
