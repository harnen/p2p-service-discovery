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
    
    boolean refresh;
    
	public TicketTable(int nBuckets, int k, int maxReplacements,Discv5TicketProtocol protocol,Topic t, int myPid, boolean refresh) {
		
		super(nBuckets, k, maxReplacements);
		
		pendingTickets = new ArrayList<BigInteger>();

		this.protocol = protocol;
		
		this.t = t;
		
		this.nodeId = t.getTopicID();
		
		this.myPid = myPid;
		
		logger = Logger.getLogger(protocol.getNode().getId().toString());
		
		this.refresh = refresh;
		//System.out.println("New ticket table size "+k+" "+refresh);
		
		// TODO Auto-generated constructor stub
	}
	
	// add a neighbour to the correct k-bucket
	public void addNeighbour(BigInteger[] nodes) {
		//logger.warning("Pending tickets "+pendingTickets.size());
		for(BigInteger node : nodes) {
			if(!pendingTickets.contains(node)) {
				if(addNeighbour(node)) {
					pendingTickets.add(node);
					//logger.warning("Adding node "+node+" to "+protocol.getNode().getId()+" "+(Util.logDistance(node,nodeId)-bucketMinDistance-1));
					//pendingTickets.put(node, null);
					//logger.warning("Sending ticket request to "+node+" from "+protocol.getNode().getId());
					protocol.sendTicketRequest(node,t,myPid);
				}/* else {
					logger.warning("Node "+node+" already in "+protocol.getNode().getId());
				}
			}*else {
				logger.warning("Node not added "+node+" to "+protocol.getNode().getId() +" pending tickets");*/
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
		//logger.warning("Pending ticket remove "+node);
		pendingTickets.remove(node);
		getBucket(node).removeNeighbour(node);
		
		BigInteger[] replacements = new BigInteger[0];
		getBucket(node).replacements.toArray(replacements);
		addNeighbour(replacements);
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
		Random rnd= new Random();
		/*for(int i=0;i<nBuckets;i++) {
			logger.warning("Ticket table "+i+" "+k_buckets[i].occupancy());
		}*/	
		int i = rnd.nextInt(nBuckets);
		//logger.warning("Tickettable refreshBuckkets of node "+this.nodeId+" at bucket "+i+" "+k_buckets[i].occupancy());

		//logger.warning("Ticket table "+i+" "+k_buckets[i].occupancy());
		KBucket b = k_buckets[i];
		//if(b.neighbours.size()<k)
		if(b.neighbours.size()<k) {
			BigInteger[] replacements = new BigInteger[0];
			b.replacements.toArray(replacements);
			addNeighbour(replacements);
		}

		if(refresh) {
			if(b.neighbours.size()<k) {
				BigInteger randomNode = generateRandomNode(i);
				protocol.sendLookup(randomNode, myPid);
				//logger.warning("Sending lookup from topic table to dist "+(Util.logDistance(randomNode, this.nodeId)- bucketMinDistance - 1)+" "+i);
			}
		}
		
	}
		
	
	
	


}
