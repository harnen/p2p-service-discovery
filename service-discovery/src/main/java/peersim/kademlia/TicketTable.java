package peersim.kademlia;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
//import java.util.Random;
import java.util.logging.Logger;

import peersim.kademlia.Util;

import peersim.core.CommonState;
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
    
    
	public TicketTable(int nBuckets, int k, int maxReplacements,Discv5TicketProtocol protocol,Topic t, int myPid, boolean refresh) {
		
		super(nBuckets, k, maxReplacements);
		
		pendingTickets = new ArrayList<BigInteger>();

		this.protocol = protocol;
		
		this.t = t;
		
		this.nodeId = t.getTopicID();
		
		this.myPid = myPid;
		
		logger = Logger.getLogger(protocol.getNode().getId().toString());
		
		this.refresh = refresh;
		
		registeredPerDist = new HashMap<Integer, Integer>();

		//System.out.println("New ticket table size "+k+" "+refresh);
		
		// TODO Auto-generated constructor stub
	}
	
	public boolean addNeighbour(BigInteger node) {
		//logger.warning("addNeighbour");
		if(!pendingTickets.contains(node)) {
			if(super.addNeighbour(node)) {
				pendingTickets.add(node);
				//logger.warning("Adding node "+node+" to "+protocol.getNode().getId()+" "+(Util.logDistance(node,nodeId)-bucketMinDistance-1));
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
		//logger.warning("Pending ticket remove "+node+" "+getBucket(node).occupancy());
		pendingTickets.remove(node);
		getBucket(node).removeNeighbour(node);
		
		//logger.warning("Pending ticket remove "+node+" "+getBucket(node).occupancy());

		/*BigInteger[] replacements = new BigInteger[0];
		getBucket(node).replacements.toArray(replacements);
		addNeighbour(replacements);*/
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
		
		//logger.warning("Ticket table "+i+" "+b.occupancy()+" "+b.replacements.size());

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
	
	


}
