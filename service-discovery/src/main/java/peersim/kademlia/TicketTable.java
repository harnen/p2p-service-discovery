package peersim.kademlia;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import peersim.kademlia.Util;

import peersim.core.CommonState;

public class TicketTable extends RoutingTable {

    /**
	 * Table to keep track of topic registrations
	 */
    private List<BigInteger> pendingTickets;
    
    private Discv5TicketProtocol protocol;
    
    private Topic t;
    
    private int myPid;
    
    Logger logger;
    
	public TicketTable(int nBuckets, int k, int maxReplacements,Discv5TicketProtocol protocol,Topic t, int myPid) {
		
		super(nBuckets, k, maxReplacements);
		
		pendingTickets = new ArrayList<BigInteger>();

		this.protocol = protocol;
		
		this.t = t;
		
		this.nodeId = t.getTopicID();
		
		this.myPid = myPid;
		
		logger = Logger.getLogger(protocol.getNode().getId().toString());
		
		// TODO Auto-generated constructor stub
	}
	
	// add a neighbour to the correct k-bucket
	public void addNeighbour(BigInteger[] nodes) {
		//logger.warning("Pending tickets "+pendingTickets.size());
		for(BigInteger node : nodes) {
			//System.out.println("Add node "+node+" to "+protocol.getNode().getId());
			if(!pendingTickets.contains(node)) {
				if(super.addNeighbour(node)) {
					pendingTickets.add(node);
					//pendingTickets.put(node, null);
					//logger.warning("Sending ticket request to "+node+" from "+protocol.getNode().getId());
					protocol.sendTicketRequest(node,t,myPid);
				} else {
					//logger.warning("Node "+node+" already in "+protocol.getNode().getId());
				}
			} /*else {
				logger.warning("Node not added "+node+" to "+protocol.getNode().getId() +" pending tickets");
			}*/
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
		//logger.warning("Pending ticket remove "+node);
		pendingTickets.remove(node);
		bucket(node).removeNeighbour(node);
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
	public void refreshBuckets(int kademliaid, int otherProtocolId) {
		Random rnd= new Random();
		/*for(int i=0;i<nBuckets;i++) {
			logger.warning("Ticket table "+i+" "+k_buckets[i].occupancy());
		}*/	
		int i = rnd.nextInt(nBuckets);
		//logger.warning("Tickettable refreshBuckkets of node "+this.nodeId+" at bucket "+i+" "+k_buckets[i].occupancy());

		KBucket b = k_buckets[i];
		//if(b.neighbours.size()<k)
		while(b.neighbours.size()<k&&b.replacements.size()>0)
			b.replace();
			//protocol.sendLookup(t, myPid);
		if(b.neighbours.size()>0) 
			b.checkAndReplaceLast(kademliaid, otherProtocolId);
			//return;
		

		//if(b.replacements.size()==0)
		//	protocol.sendLookup(generateRandomNode(i), myPid);
	
		
	}
		
	private BigInteger generateRandomNode(int b) {
			
			UniformRandomGenerator urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, CommonState.r);
			BigInteger rand = urg.generate();
			String rand2 = Util.put0(rand);
			
			int distance = b + this.bucketMinDistance + 1;
			
			int prefixlen = (KademliaCommonConfig.BITS - distance);
			
			String nodeId2 = Util.put0(nodeId);
			
			BigInteger randNode = new BigInteger(nodeId2.substring(0,prefixlen).concat(rand2.substring(prefixlen,rand2	.length())),2);
			
			return randNode;
		

	}
	
	
	


}
