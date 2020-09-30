	package peersim.kademlia;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import peersim.core.CommonState;

public class TicketTable extends RoutingTable {

    /**
	 * Table to keep track of topic registrations
	 */
    private List<BigInteger> pendingTickets;
    
    private Discv5TicketProtocol protocol;
    
    private Topic t;
    
    private int myPid;
    
	public TicketTable(int nBuckets, int k, int maxReplacements,Discv5TicketProtocol protocol,Topic t, int myPid) {
		
		super(nBuckets, k, maxReplacements);
		
		pendingTickets = new ArrayList<BigInteger>();

		this.protocol = protocol;
		
		this.t = t;
		
		this.nodeId = t.getTopicID();
		
		this.myPid = myPid;
		// TODO Auto-generated constructor stub
	}
	
	// add a neighbour to the correct k-bucket
	public void addNeighbour(BigInteger[] nodes) {
		for(BigInteger node : nodes) {
			//System.out.println("Add node "+node+" to bucket "+Util.logDistance(node, nodeId));
			if(!pendingTickets.contains(node)) {
				if(super.addNeighbour(node)) {
					pendingTickets.add(node);
					//pendingTickets.put(node, null);
					protocol.sendTicketRequest(node,t,myPid);
				}
			}
		}
	}

	public void addTicket(Message m,Ticket ticket) {
		//System.out.println("Ticket added for topic "+t.getTopic());
		if(pendingTickets.contains(m.src.getId())) {
			//System.out.println("Ticket added for topic "+t.getTopic());
			//pendingTickets.put(m.src.getId(), ticket);
			
		    Message register = new Message(Message.MSG_REGISTER, t);
			register.ackId = m.id; // set ACK number
		    register.dest = new KademliaNode(m.src);
		    register.body = m.body;
		    register.operationId = m.operationId;
			protocol.scheduleSendMessage(register, m.src.getId(), myPid, ticket.getWaitTime());
		}
			
	}
	
	// remove a neighbour from the correct k-bucket
	public void removeNeighbour(BigInteger node) {
		// get the lenght of the longest common prefix (correspond to the correct k-bucket)
		pendingTickets.remove(node);
		bucket(node).removeNeighbour(node);
		//System.out.println("Bucket remove "+bucket(node).occupancy());
		
	}	
	/**
	 * Check nodes and replace buckets with valid nodes from replacement list
	 * 
	 */
	/**
	 * Check nodes and replace buckets with valid nodes from replacement list
	 * 
	 */
	public void refreshBuckets(int kademliaid) {
		Random rnd= new Random();
		//for(int i=0;i<nBuckets;i++) {
		//System.out.println(CommonState.getTime()+" Routingtable refreshBuckkets of node "+this.nodeId+" at bucket "+rnd.nextInt(nBuckets)+" "+k_buckets[rnd.nextInt(nBuckets)].occupancy());

		KBucket b = k_buckets[rnd.nextInt(nBuckets)];
		if(b.neighbours.size()<k)
			protocol.sendLookup(t, myPid);
		if(b.neighbours.size()>0) 
			b.checkAndReplaceLast(kademliaid);
			//return;

	}
	
	
	


}
