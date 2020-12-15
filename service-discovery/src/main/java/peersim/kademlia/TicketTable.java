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
		/*super(0, 0, 0);
		
		k_buckets = new KBucket[nBuckets];
		this.nBuckets = nBuckets;
		this.k=k;
		this.maxReplacements=maxReplacements;
		bucketMinDistance = KademliaCommonConfig.BITS - nBuckets;
		
		int sbucket = k;
		for (int i = 0; i < k_buckets.length; i++) {
			//System.out.println("Creating bucket "+i+" size "+sbucket);
			k_buckets[i] = new KBucket(this,sbucket,maxReplacements);
			sbucket++;
		}*/
		
		pendingTickets = new ArrayList<BigInteger>();

		this.protocol = protocol;
		
		this.t = t;
		
		this.nodeId = t.getTopicID();
		
		this.myPid = myPid;
		
		logger = Logger.getLogger(protocol.getNode().getId().toString());
		
		this.refresh = refresh;
		
		registeredPerDist = new HashMap<Integer, Integer>();

		registeredNodes = new ArrayList<BigInteger>();
		//System.out.println("New ticket table size "+k+" "+refresh);
		
		// TODO Auto-generated constructor stub
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
		//logger.warning("addNeighbour");
		if(!pendingTickets.contains(node)&&!registeredNodes.contains(node)) {
			if(super.addNeighbour(node)) {
				pendingTickets.add(node);
				//logger.warning("Adding node "+node+" to bucket "+getBucketNum(node)+" "+getBucket(node).occupancy());
				//pendingTickets.put(node, null);
				//logger.warning("Sending ticket request to "+node+" from "+protocol.getNode().getId());
				protocol.sendTicketRequest(node,t,myPid);
				return true;
			} 
		}
		return false;
	}
	// add a neighbour to the correct k-bucket
	public void addNeighbour(BigInteger[] nodes) {
		//logger.warning("addNeighbours");
		for(BigInteger node : nodes) {
			addNeighbour(node);
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
		//logger.warning("Pending ticket Remove "+node+" "+getBucket(node).occupancy()+" "+getBucket(node).replacements.size()+" "+getBucketNum(node));

		//logger.warning("Pending ticket remove "+node+" "+getBucket(node).occupancy());

		//for(BigInteger b : getBucket(node).replacements)
		//	addNeighbour(b);
		//logger.warning("Node "+node+" removed at "+protocol.getNode().getId());
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
	public void refreshBuckets() {
		//Random rnd= new Random();
		/*for(int i=0;i<nBuckets;i++) {
			logger.warning("Ticket table "+i+" "+k_buckets[i].occupancy());
		}*/	
		int i = CommonState.r.nextInt(nBuckets);
		//logger.warning("Tickettable refreshBuckkets of node "+this.nodeId+" at bucket "+i+" "+k_buckets[i].occupancy());

		KBucket b = k_buckets[i];
		//if(b.neighbours.size()<k)
		
		//logger.warning("Refreshing bucket "+i+" "+b.occupancy()+" "+b.replacements.size());

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
			//protocol.sendLookup(t, myPid);
			//b.replace();
			//Random rand = new Random();
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
			//BigInteger randomNode = generateRandomNode(i);
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
