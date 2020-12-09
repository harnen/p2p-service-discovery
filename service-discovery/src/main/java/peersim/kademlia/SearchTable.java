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
		
		//pendingTickets = new ArrayList<BigInteger>();

		this.protocol = protocol;
		
		this.t = t;
		
		this.nodeId = t.getTopicID();
		
		this.myPid = myPid;
		
		//this.pendingLookup = true;
		
		this.refresh = refresh;
		
		removedPerDist = new HashMap<Integer, Integer>();
		
		added = new HashSet<BigInteger>();
		
		//System.out.println("New search table size "+k+" "+refresh);

		// TODO Auto-generated constructor stub
	}
	
	// add a neighbour to the correct k-bucket
	public boolean addNeighbour(BigInteger node) {
		boolean result = false;
		if(!added.contains(node)) {
			//System.out.println("Adding "+Util.logDistance(nodeId, node)+" "+node);
			result = super.addNeighbour(node);
			//don't add the same node multiple time
			if(result) {
				//System.out.println("Added "+node);
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
		
		/*if(pendingLookup) {
			pendingLookup = false;

        	Message m = new Message(Message.MSG_INIT_TOPIC_LOOKUP, t);
    		m.timestamp = CommonState.getTime();
			protocol.sendTopicLookup(m, t, myPid);
		}*/

	}


	// remove a neighbour from the correct k-bucket
	public void removeNeighbour(BigInteger node) {
		// get the lenght of the longest common prefix (correspond to the correct k-bucket)
		//pendingTickets.remove(node);
		getBucket(node).removeNeighbour(node);
		/*BigInteger[] replacements = new BigInteger[0];
		getBucket(node).replacements.toArray(replacements);
		System.out.println("remove neighbour "+node+" "+Util.logDistance(nodeId, node)+" adding "+replacements.length+" replacements.");
		addNeighbour(replacements);*/
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
		//System.out.println("Refresh bucket");
		//Random rnd= new Random();
		//for(int i=0;i<nBuckets;i++) {
		int i = CommonState.r.nextInt(nBuckets);
		KBucket b = k_buckets[i];
		
		while(b.neighbours.size()<k&&b.replacements.size()>0) {
			//protocol.sendLookup(t, myPid);
			//b.replace();
			//Random rand = new Random();
			BigInteger n = b.replacements.get(CommonState.r.nextInt(b.replacements.size()));
			addNeighbour(n);
			b.replacements.remove(n);
		}

		if(b.neighbours.size()==0&&refresh) {
			BigInteger randomNode = generateRandomNode(i);
			protocol.sendLookup(randomNode, myPid);
		}
		//print();

	}

	
	public int getnBuckets() {
		return nBuckets;
	}
	
	public void print() {
		System.out.println("Search table:");
		int sum = 0;
		for(int dist = 256; dist > bucketMinDistance ; dist-- ) {
			int removed = 0;
			if(removedPerDist.containsKey(dist))
				removed = removedPerDist.get(dist);
			
			System.out.println("b[" + dist + "]: " + super.bucketAtDistance(dist).occupancy() +" replacements:"+super.bucketAtDistance(dist).replacements.size()+" +" + removed);
			sum += removed;
		}
		System.out.println("Asked " + sum + " nodes.");
	}
	
	public void dump() {
		System.out.println("Search table:");
		int sum = 0;
		for(int dist = 256; dist > bucketMinDistance ; dist-- ) {
						
			System.out.println("b[" + dist + "]: " + super.bucketAtDistance(dist).neighbours);
			System.out.println("\tb_r[" + dist + "]: " + super.bucketAtDistance(dist).replacements);
			
		}
		System.out.println("Asked " + sum + " nodes.");
	}


}
