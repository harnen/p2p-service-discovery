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
    
	public SearchTable(int nBuckets, int k, int maxReplacements,Discv5TicketProtocol protocol,Topic t, int myPid) {
		
		super(nBuckets, k, maxReplacements);
		
		//pendingTickets = new ArrayList<BigInteger>();

		this.protocol = protocol;
		
		this.t = t;
		
		this.nodeId = t.getTopicID();
		
		this.myPid = myPid;
		
		pendingLookup = true;
		// TODO Auto-generated constructor stub
	}
	
	// add a neighbour to the correct k-bucket
	public void addNeighbour(BigInteger[] node) {
		for(BigInteger dest : node)
			super.addNeighbour(dest);
		
		if(pendingLookup) {
			pendingLookup = false;

        	Message m = new Message(Message.MSG_INIT_TOPIC_LOOKUP, t);
    		m.timestamp = CommonState.getTime();
			protocol.sendTopicLookup(m, t, myPid);
		}

	}


	// remove a neighbour from the correct k-bucket
	public void removeNeighbour(BigInteger node) {
		// get the lenght of the longest common prefix (correspond to the correct k-bucket)
		//pendingTickets.remove(node);
		bucket(node).removeNeighbour(node);
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
		KBucket b = k_buckets[rnd.nextInt(nBuckets)];
		while(b.neighbours.size()<k&&b.replacements.size()>0)
			//protocol.sendLookup(t, myPid);
			b.replace();
		if(b.neighbours.size()>0) {
			b.checkAndReplaceLast();
			//return;
		}
	}
	
	


}