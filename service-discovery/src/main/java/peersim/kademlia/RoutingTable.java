package peersim.kademlia;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;

import peersim.core.CommonState;

import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Gives an implementation for the routing table component of a kademlia node
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class RoutingTable implements Cloneable {

	protected int bucketMinDistance;

	// node ID of the node
	protected BigInteger nodeId = null;

	// k-buckets
	//protected TreeMap<Integer, KBucket> k_buckets = null;
	protected KBucket k_buckets[];

	protected int nBuckets,k,maxReplacements;
	//protected KademliaProtocol prot;
	// ______________________________________________________________________________________________
	/**
	 * instanciates a new empty routing table with the specified size
	 */
	public RoutingTable(int nBuckets, int k, int maxReplacements) {
		// initialize k-bukets
		k_buckets = new KBucket[nBuckets];
		this.nBuckets = nBuckets;
		this.k=k;
		this.maxReplacements=maxReplacements;
		bucketMinDistance = KademliaCommonConfig.BITS - nBuckets;
		for (int i = 0; i < k_buckets.length; i++) {
			k_buckets[i] = new KBucket(this,k,maxReplacements);
		}

		//this.prot=prot;
	}

	public int containsNode(BigInteger id){
		for(int i = 0; i < nBuckets; i++){
			for(BigInteger nodeId: k_buckets[i].neighbours){
				if(nodeId.equals(id))
					return i;
			}
		}
		return -1;
	}

	// add a neighbour to the correct k-bucket
	public boolean addNeighbour(BigInteger node) {
		// get the lenght of the longest common prefix (correspond to the correct k-bucket)
		//System.out.println("Bucket add node "+node+" in "+nodeId+" at bucket "+Util.logDistance(nodeId, node));
		if(node.compareTo(nodeId)==0) return false;
			
		return getBucket(node).addNeighbour(node);
	}

	// remove a neighbour from the correct k-bucket
	public void removeNeighbour(BigInteger node) {
		// get the lenght of the longest common prefix (correspond to the correct k-bucket)
		getBucket(node).removeNeighbour(node);
	}
	

	// return the neighbours with a specific common prefix len
	public BigInteger[] getNeighbours (final int dist) {
		BigInteger[] result = new BigInteger[0];
		ArrayList<BigInteger> resultList = new ArrayList<BigInteger>();
		resultList.addAll(bucketAtDistance(dist).neighbours);
		//System.out.println("Getneighbours "+resultList.size());
		if(resultList.size()<k && (dist+1)<=256) {
			//System.out.println("Getneighbours "+resultList.size());
			resultList.addAll(bucketAtDistance(dist+1).neighbours);

		}
		if(resultList.size()<k& (dist-1)>=0) {
			//System.out.println("Getneighbours "+resultList.size());
			resultList.addAll(bucketAtDistance(dist-1).neighbours);
		}
		return resultList.toArray(result);
	}
	
	public BigInteger[] getKClosestNeighbours (final int k, int dist) {
		BigInteger[] result = new BigInteger[k];
		ArrayList<BigInteger> resultList = new ArrayList<BigInteger>();
		while(resultList.size()<k && dist<=KademliaCommonConfig.BITS) {
			resultList.addAll(bucketAtDistance(dist).neighbours);
			dist++;
		}
		
		return resultList.toArray(result);

	}



	// ______________________________________________________________________________________________
	public Object clone() {
		RoutingTable dolly = new RoutingTable(nBuckets,k,maxReplacements);
		for (int i = 0; i < k_buckets.length; i++) {
			k_buckets[i] = new KBucket(this,k,maxReplacements);
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
		//System.out.println("Routingtable refreshBuckkets "+CommonState.getTime());
		Random rnd= new Random();
		//for(int i=0;i<nBuckets;i++) {
		KBucket b = k_buckets[rnd.nextInt(nBuckets)];
		if(b.neighbours.size()>0) {
			b.checkAndReplaceLast();
			return;
		}
		//}
	}
	
	public Set<BigInteger> getAllNeighbours(){
		Set<BigInteger> allNeighbours = new HashSet<BigInteger>();
		for(KBucket b: k_buckets) {
			allNeighbours.addAll(b.neighbours);
		}
		return allNeighbours;
	}
	
	public void sendToFront(BigInteger node)
	{
		if(getBucket(node).neighbours.remove(node))
			getBucket(node).neighbours.add(0,node);
	}
	
	
	
	public KBucket getBucket(BigInteger node) {
		//return bucketAtDistance(Util.prefixLen(nodeId, node));
		return bucketAtDistance(Util.logDistance(nodeId, node));
	}

	public int getBucketNum(BigInteger node) {
		int dist = Util.logDistance(nodeId, node);
		if (dist <= bucketMinDistance) {
			return 0;
		}
		return dist - bucketMinDistance - 1;
	}
	
	protected KBucket bucketAtDistance(int distance) {
		if (distance <= bucketMinDistance) {
			//System.out.println("bucket at distance "+distance+" "+bucketMinDistance+" "+0);
			return k_buckets[0];
		}
		//System.out.println("bucket at distance "+bucketMinDistance+" "+distance+" "+(distance - bucketMinDistance - 1));
		return k_buckets[distance - bucketMinDistance - 1];
	}
	
	public void setNodeId(BigInteger id){
		this.nodeId = id;
	}
	
	protected BigInteger generateRandomNode(int b) {
		
		BigInteger randNode;
		//do {
			UniformRandomGenerator urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, CommonState.r);
			BigInteger rand = urg.generate();
			String rand2 = Util.put0(rand);
			
			int distance = b + this.bucketMinDistance + 1;
				
			int prefixlen = (KademliaCommonConfig.BITS - distance);
			
			String nodeId2 = Util.put0(nodeId);
			
			String randomString = "";
			//System.out.println(nodeId2+" "+prefixlen+" "+b+" "+distance);
			//System.out.println(rand2);
			if(prefixlen>0) {
				if(nodeId2.charAt(prefixlen)==rand2.charAt(prefixlen)) {
					if(Integer.parseInt(nodeId2.substring(prefixlen-1,prefixlen))==0) {
						randomString = nodeId2.substring(0,prefixlen).concat("1");
					} else {
						randomString = nodeId2.substring(0,prefixlen).concat("0");
					}
					randomString = randomString.concat(rand2.substring(prefixlen+1,rand2.length()));
				//logger.warning(randomString);
				} else 
					randomString = nodeId2.substring(0,prefixlen).concat(rand2.substring(prefixlen,rand2.length()));
			//logger.warning(randomString);
				randNode = new BigInteger(randomString,2);

			} else {
				randNode = rand;
			}
			
			//logger.warning("Distance "+Util.logDistance(randNode, b));
			
		//}while(Util.logDistance(randNode, nodeId)!=b);
		
		return randNode;
	}
	
	public int getbucketMinDistance() {
		return bucketMinDistance;
	}
	

	// ______________________________________________________________________________________________

} // End of class
// ______________________________________________________________________________________________
