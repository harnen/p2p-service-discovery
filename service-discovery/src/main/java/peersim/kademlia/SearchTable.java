package peersim.kademlia;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
//import java.util.Random;

import peersim.core.CommonState;

public class SearchTable extends RoutingTable {

    /**
	 * Table to keep track of topic registrations
	 */
    private List<BigInteger> pendingTickets;
    
    private Discv5TicketProtocol protocol;
    
    private Topic t;
    
    private int myPid;
    
    boolean pendingLookup;
    
    boolean refresh;
    
    //just for statistics
    HashMap<Integer, Integer> removedPerDist;
    HashSet<BigInteger> added;
    
	public SearchTable(int nBuckets, int k, int maxReplacements,Discv5TicketProtocol protocol,Topic t, int myPid, boolean refresh) {
		
		super(nBuckets, k, maxReplacements);
		
		this.protocol = protocol;
		
		this.t = t;
		
		this.nodeId = t.getTopicID();
		
		this.myPid = myPid;
				
		this.refresh = refresh;
		
		removedPerDist = new HashMap<Integer, Integer>();
		
		added = new HashSet<BigInteger>();

	}
	
	// add a neighbour to the correct k-bucket
	public boolean addNeighbour(BigInteger node) {
		boolean result = false;
		if(!added.contains(node)) {
			result = super.addNeighbour(node);

			if(result) {
				added.add(node);
			}
		}
		return result;
	}
	

	
	// add a neighbour to the correct k-bucket
	public void addNeighbour(BigInteger[] node) {
		for(BigInteger dest : node) {
			addNeighbour(dest);
		}
		
	}


	// remove a neighbour from the correct k-bucket
	public void removeNeighbour(BigInteger node) {
	
		getBucket(node).removeNeighbour(node);
		
		int dist = Util.logDistance(nodeId, node);
		if(!removedPerDist.containsKey(dist)){
			removedPerDist.put(dist, 1);
		}else {
			removedPerDist.put(dist, removedPerDist.get(dist) + 1);
		}
	}	
	
	/**
	 * Check nodes and replace buckets with valid nodes from replacement list
	 * 
	 */
	public void refreshBuckets() {
		
		int i = CommonState.r.nextInt(nBuckets);
		KBucket b = k_buckets[i];
		
		while(b.neighbours.size()<b.k&&b.replacements.size()>0) {
			
			BigInteger n = b.replacements.get(CommonState.r.nextInt(b.replacements.size()));
			addNeighbour(n);
			b.replacements.remove(n);
		}

		if(b.neighbours.size()==0&&refresh) {
			BigInteger randomNode = generateRandomNode(i);
			protocol.sendLookup(randomNode, myPid);
		}

	}

	
	public int getnBuckets() {
		return nBuckets;
	}
	
	public void print() {
		System.out.println("Search table topic "+t.getTopic()+":");
		int sum = 0;
		for(int dist = 256; dist > bucketMinDistance ; dist-- ) {
			int removed = 0;
			if(removedPerDist.containsKey(dist))
				removed = removedPerDist.get(dist);
			
			System.out.println("search "+t.getTopic()+" b[" + dist + "]: " + super.bucketAtDistance(dist).occupancy() +" replacements:"+super.bucketAtDistance(dist).replacements.size()+" +" + removed);
			sum += removed;
		}
		System.out.println("Asked "+t.getTopic()+" "+ sum + " nodes.");
	}
	
	public void dump() {
		System.out.println("Search table topic "+t.getTopic()+":");
		int sum = 0;
		for(int dist = 256; dist > bucketMinDistance ; dist-- ) {
						
			System.out.println("search "+t.getTopic()+" b[" + dist + "]: " + super.bucketAtDistance(dist).neighbours);
			System.out.println("\tb_r[" + dist + "]: " + super.bucketAtDistance(dist).replacements);
			
		}
		System.out.println("Asked "+t.getTopic()+" "+ sum + " nodes.");
	}


}
