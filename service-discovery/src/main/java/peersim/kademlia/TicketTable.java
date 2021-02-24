package peersim.kademlia;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
//import java.util.Random;
import java.util.logging.Logger;

import peersim.kademlia.Util;

import peersim.core.CommonState;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

public class TicketTable extends RoutingTable {

    /**
	 * Table to keep track of topic registrations
	 */
    private List<BigInteger> pendingTickets;
    
    private Discv5TicketProtocol protocol;
    
    private Topic t;
    
    private int myPid;
    
    Logger logger;
    
    boolean refresh;
    
    HashMap<Integer, Integer> registeredPerDist;
    
    private List<BigInteger> registeredNodes;

    
	public TicketTable(int nBuckets, int k, int maxReplacements,Discv5TicketProtocol protocol,Topic t, int myPid, boolean refresh) {
		
		super(nBuckets,k,maxReplacements);
		
		pendingTickets = new ArrayList<BigInteger>();

		this.protocol = protocol;
		
		this.t = t;
		
		this.nodeId = t.getTopicID();
		
		this.myPid = myPid;
		
		logger = Logger.getLogger(protocol.getNode().getId().toString());
		
		this.refresh = refresh;
		
		registeredPerDist = new HashMap<Integer, Integer>();

		registeredNodes = new ArrayList<BigInteger>();
		
	}

	public TicketTable(int nBuckets,Discv5TicketProtocol protocol,Topic t, int myPid, boolean refresh) {
		
		super(0, 0, 0);
		k_buckets = new KBucket[nBuckets];
		this.nBuckets = nBuckets;
		this.maxReplacements=0;
		bucketMinDistance = KademliaCommonConfig.BITS - nBuckets;
		
		this.k=3;
		int sbucket = 3;
		for (int i = 0; i < k_buckets.length; i++) {
			System.out.println("Creating bucket "+i+" size "+sbucket);
			k_buckets[i] = new KBucket(this,sbucket,maxReplacements);
			if(i>10)sbucket++;
		}
		
		pendingTickets = new ArrayList<BigInteger>();

		this.protocol = protocol;
		
		this.t = t;
		
		this.nodeId = t.getTopicID();
		
		this.myPid = myPid;
		
		logger = Logger.getLogger(protocol.getNode().getId().toString());
		
		this.refresh = refresh;
		
		registeredPerDist = new HashMap<Integer, Integer>();

		registeredNodes = new ArrayList<BigInteger>();
	}
	
	public boolean addNeighbour(BigInteger node) {

		if(!pendingTickets.contains(node) && !registeredNodes.contains(node)) {
			if(super.addNeighbour(node)) {
				pendingTickets.add(node);
				protocol.sendTicketRequest(node,t,myPid);
				return true;
			} 
		}
		return false;
	}
	// add a neighbour to the correct k-bucket
	public void addNeighbour(BigInteger[] nodes) {

		for(BigInteger node : nodes) {
			addNeighbour(node);
		}
	}

	public void addTicket(Message m,Ticket ticket) {

		if(pendingTickets.contains(m.src.getId())) {
			
		    Message register = new Message(Message.MSG_REGISTER, t);
			register.ackId = m.id; 
		    register.dest = new KademliaNode(m.src);
		    register.body = ticket;
		    register.operationId = m.operationId;
			protocol.scheduleSendMessage(register, m.src.getId(), myPid, ticket.getWaitTime());
			
			int dist = Util.logDistance(nodeId,  m.src.getId());
			if(!registeredPerDist.containsKey(dist)){
				registeredPerDist.put(dist, 1);
			}else {
				registeredPerDist.put(dist, registeredPerDist.get(dist) + 1);
			}
		}
			
	}
	
	// remove a neighbour from the correct k-bucket
	public void removeNeighbour(BigInteger node) {
		// get the lenght of the longest common prefix (correspond to the correct k-bucket)
		pendingTickets.remove(node);
		getBucket(node).removeNeighbour(node);
		
		int i = Util.logDistance(nodeId, node) - bucketMinDistance - 1;
		BigInteger randomNode = generateRandomNode(i);
		protocol.refreshBucket(this, randomNode,i);
		
	}	
	
	
	/**
	 * Check nodes and replace buckets with valid nodes from replacement list
	 * 
	 */
	public void refreshBuckets() {
	
		int i = CommonState.r.nextInt(nBuckets);

		KBucket b = k_buckets[i];
		
		List<BigInteger> toRemove = new ArrayList<BigInteger>();
		for(BigInteger n : b.neighbours) {
			Node node = Util.nodeIdtoNode(n);
			if(!node.isUp()){
				toRemove.add(n);
			}
		}
		
		for(BigInteger n : toRemove)
			removeNeighbour(n);
		
		while(b.neighbours.size()<b.k&&b.replacements.size()>0) {
			
			BigInteger n = b.replacements.get(CommonState.r.nextInt(b.replacements.size()));
			
			Node node = Util.nodeIdtoNode(n);

			if(node.isUp())addNeighbour(n);
			b.replacements.remove(n);
		}
		
		BigInteger randomNode = null;

		if(b.replacements.size()==0||b.neighbours.size()<b.k) {
			randomNode = generateRandomNode(i);
			protocol.refreshBucket(this, randomNode,i);
		}
		
		if(b.neighbours.size()==0&&refresh) {
			protocol.sendLookup(randomNode, myPid);
		}
		
	}
	
	public void addRegisteredList(BigInteger node) {
		registeredNodes.add(node);
	}
	
	public void removeRegisteredList(BigInteger node) {
		registeredNodes.remove(node);
	}
	
	public void print() {
		System.out.println("Ticket table:");
		int sum = 0;
		for(int dist = 256; dist > bucketMinDistance ; dist-- ) {
			int removed = 0;
			if(registeredPerDist.containsKey(dist))
				removed = registeredPerDist.get(dist);
			
			System.out.println("b[" + dist + "]: " + super.bucketAtDistance(dist).occupancy() +" replacements:"+super.bucketAtDistance(dist).replacements.size()+" +" + removed);
			sum += removed;
		}
		System.out.println("Asked " + sum + " nodes.");
	}
	
	public BigInteger getTopicId() {
		return nodeId;
	}


}
