package peersim.kademlia;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

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
		
		//System.out.println("New search table size "+k+" "+refresh);

		// TODO Auto-generated constructor stub
	}
	
	// add a neighbour to the correct k-bucket
	public void addNeighbour(BigInteger[] node) {
		//System.out.println("Search table adding node");
		for(BigInteger dest : node)
			super.addNeighbour(dest);
		
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
		//System.out.println("Routingtable refreshBuckkets "+CommonState.getTime());
		Random rnd= new Random();
		//for(int i=0;i<nBuckets;i++) {
		int i = rnd.nextInt(nBuckets);
		KBucket b = k_buckets[i];
		while(b.neighbours.size()<k&&b.replacements.size()>0)
			//protocol.sendLookup(t, myPid);
			b.replace();
		if(b.neighbours.size()>0) {
			b.checkAndReplaceLast();
			//return;
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
		System.out.println("Routing table:");
		int sum = 0;
		for(int dist = 256; dist > bucketMinDistance ; dist-- ) {
			int removed = 0;
			if(removedPerDist.containsKey(dist))
				removed = removedPerDist.get(dist);
			
			System.out.println("b[" + dist + "]: " + super.bucketAtDistance(dist).occupancy() + " +" + removed);
			sum += removed;
		}
		System.out.println("Asked " + sum + " nodes.");
	}


}