package peersim.kademlia;

/**
 * Discv5 Protocol implementation.
 *
 */ 
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.sun.tools.sjavac.Log;

import peersim.kademlia.Topic;
import peersim.kademlia.operations.LookupTicketOperation;
import peersim.kademlia.operations.Operation;
import peersim.kademlia.operations.TicketOperation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import peersim.config.Configuration;
import peersim.core.Cleanable;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;
import peersim.transport.UnreliableTransport;
import peersim.kademlia.KademliaCommonConfig;
import peersim.kademlia.KademliaNode;
import peersim.kademlia.Message;
import peersim.kademlia.TicketTable;

public class Discv5TicketProtocol extends KademliaProtocol implements Cleanable{

    /**
	 * Topic table of this node
	 */
    public Discv5TopicTable topicTable;

    /**
	 * Table to keep track of topic registrations
	 */
    protected HashMap<BigInteger,TicketTable> ticketTables;
    
    /**
	 * Table to search for topics
	 */
    private HashMap<BigInteger,SearchTable> searchTables;
    
	final String PAR_TOPIC_TABLE_CAP = "TOPIC_TABLE_CAP";
	final String PAR_ADS_PER_QUEUE = "ADS_PER_QUEUE";
	final String PAR_AD_LIFE_TIME = "AD_LIFE_TIME";
	final String PAR_TICKET_TABLE_BUCKET_SIZE = "TICKET_TABLE_BUCKET_SIZE";
	final String PAR_SEARCH_TABLE_BUCKET_SIZE = "SEARCH_TABLE_BUCKET_SIZE";
	final String PAR_REFRESH_TICKET_TABLE = "REFRESH_TICKET_TABLE";
	final String PAR_REFRESH_SEARCH_TABLE = "REFRESH_SEARCH_TABLE";
	final String PAR_TICKET_NEIGHBOURS = "TICKET_NEIGHBOURS";
	final String PAR_LOOKUP_BUCKET_ORDER = "LOOKUP_BUCKET_ORDER";
	final String PAR_TICKET_REMOVE_AFTER_REG = "TICKET_REMOVE_AFTER_REG";
	final String PAR_TICKET_TABLE_REPLACEMENTS = "TICKET_TABLE_REPLACEMENTS";
	final String PAR_SEARCH_TABLE_REPLACEMENTS = "SEARCH_TABLE_REPLACEMENTS";
	final String PAR_STOP_REGISTER_WINDOW_SIZE = "STOP_REGISTER_WINDOW_SIZE";
	final String PAR_STOP_REGISTER_MIN_REGS = "STOP_REGISTER_MIN_REGS";

	boolean printSearchTable=true;
	/**
	 * Replicate this object by returning an identical copy.<br>
	 * It is called by the initializer and do not fill any particular field.
	 * 
	 * @return Object
	 */
	public Object clone() {
		Discv5TicketProtocol dolly = new Discv5TicketProtocol(Discv5TicketProtocol.prefix);
		return dolly;
	}
	
    /**
	 * Used only by the initializer when creating the prototype. Every other instance call CLONE to create the new object.
	 * 
	 * @param prefix
	 *            String
	 */
	public Discv5TicketProtocol(String prefix) {
		super(prefix);
        this.topicTable = new Discv5TopicTable();
        ticketTables = new HashMap<BigInteger,TicketTable>();
        searchTables = new HashMap<BigInteger,SearchTable>();
    }
	
	/**
	 * This procedure is called only once and allow to inizialize the internal state of KademliaProtocol. Every node shares the
	 * same configuration, so it is sufficient to call this routine once.
	 */
	protected void _init() {
		// execute once
		if (_ALREADY_INSTALLED)
			return;

		
		KademliaCommonConfig.TOPIC_TABLE_CAP = Configuration.getInt(prefix + "." + PAR_TOPIC_TABLE_CAP, KademliaCommonConfig.TOPIC_TABLE_CAP);
		KademliaCommonConfig.ADS_PER_QUEUE = Configuration.getInt(prefix + "." + PAR_ADS_PER_QUEUE, KademliaCommonConfig.ADS_PER_QUEUE);
		KademliaCommonConfig.AD_LIFE_TIME = Configuration.getInt(prefix + "." + PAR_AD_LIFE_TIME, KademliaCommonConfig.AD_LIFE_TIME);
		KademliaCommonConfig.TICKET_BUCKET_SIZE = Configuration.getInt(prefix + "." + PAR_TICKET_TABLE_BUCKET_SIZE, KademliaCommonConfig.TICKET_BUCKET_SIZE);
		KademliaCommonConfig.SEARCH_BUCKET_SIZE = Configuration.getInt(prefix + "." + PAR_SEARCH_TABLE_BUCKET_SIZE, KademliaCommonConfig.SEARCH_BUCKET_SIZE);
		KademliaCommonConfig.TICKET_REFRESH = Configuration.getInt(prefix + "." + PAR_REFRESH_TICKET_TABLE, KademliaCommonConfig.TICKET_REFRESH);
		KademliaCommonConfig.SEARCH_REFRESH = Configuration.getInt(prefix + "." + PAR_REFRESH_SEARCH_TABLE, KademliaCommonConfig.SEARCH_REFRESH);
		KademliaCommonConfig.TICKET_NEIGHBOURS = Configuration.getInt(prefix + "." + PAR_TICKET_NEIGHBOURS, KademliaCommonConfig.TICKET_NEIGHBOURS);
		KademliaCommonConfig.LOOKUP_BUCKET_ORDER = Configuration.getInt(prefix + "." + PAR_LOOKUP_BUCKET_ORDER, KademliaCommonConfig.LOOKUP_BUCKET_ORDER);
		KademliaCommonConfig.TICKET_REMOVE_AFTER_REG = Configuration.getInt(prefix + "." + PAR_TICKET_REMOVE_AFTER_REG, KademliaCommonConfig.TICKET_REMOVE_AFTER_REG);
		KademliaCommonConfig.TICKET_TABLE_REPLACEMENTS = Configuration.getInt(prefix + "." + PAR_TICKET_TABLE_REPLACEMENTS, KademliaCommonConfig.TICKET_TABLE_REPLACEMENTS);
		KademliaCommonConfig.SEARCH_TABLE_REPLACEMENTS = Configuration.getInt(prefix + "." + PAR_SEARCH_TABLE_REPLACEMENTS, KademliaCommonConfig.SEARCH_TABLE_REPLACEMENTS);
		KademliaCommonConfig.STOP_REGISTER_WINDOW_SIZE = Configuration.getInt(prefix + "." + PAR_STOP_REGISTER_WINDOW_SIZE, KademliaCommonConfig.STOP_REGISTER_WINDOW_SIZE);
		KademliaCommonConfig.STOP_REGISTER_MIN_REGS = Configuration.getInt(prefix + "." + PAR_STOP_REGISTER_MIN_REGS, KademliaCommonConfig.STOP_REGISTER_MIN_REGS);

		super._init();
	}


	/**
	 * schedule sending a message after a given delay  with current transport layer and starting the timeout timer (which is an event) if the message is a request 
	 * 
	 * @param m
	 *            the message to send
	 * @param destId
	 *            the Id of the destination node
	 * @param myPid
	 *            the sender Pid
     * @param delay
     *            the delay to wait before sending           
	 */
	public void scheduleSendMessage(Message m, BigInteger destId, int myPid, long delay) {
		Node src = Util.nodeIdtoNode(this.node.getId());
		Node dest = Util.nodeIdtoNode(destId);
        
        int destpid = dest.getKademliaProtocol().getProtocolID();

        m.src = this.node;
        m.dest = new KademliaNode(destId);     
		
        logger.info("-> (" + m + "/" + m.id + ") " + destId);

        // TODO: remove the assert later
	    //assert(src == this.node);

		transport = (UnreliableTransport) (Network.prototype).getProtocol(tid);
        long network_delay = transport.getLatency(src, dest);
        EDSimulator.add(network_delay+delay, m, dest, destpid); 

		if ( (m.getType() == Message.MSG_FIND) || (m.getType() == Message.MSG_REGISTER)|| (m.getType() == Message.MSG_TICKET_REQUEST) ) { 
			Timeout t = new Timeout(destId, m.id, m.operationId);

			// add to sent msg
			this.sentMsg.put(m.id, m.timestamp);
			EDSimulator.add(delay+4*network_delay, t, src, myPid); 
		}
    }

	
	/**
	 * send a message with current transport layer and starting the timeout timer (which is an event) if the message is a request
	 * 
	 * @param m
	 *            the message to send
	 * @param destId
	 *            the Id of the destination node
	 * @param myPid
	 *            the sender Pid
	 */
	public void sendMessage(Message m, BigInteger destId, int myPid) {
		// add destination to routing table
		this.routingTable.addNeighbour(destId);

        m.src = this.node;
        m.dest = new KademliaNode(destId);     
		Node src = Util.nodeIdtoNode(this.node.getId());
		Node dest = Util.nodeIdtoNode(destId);

        int destpid = dest.getKademliaProtocol().getProtocolID();

		logger.info("-> (" + m + "/" + m.id + ") " + destId);

		transport = (UnreliableTransport) (Network.prototype).getProtocol(tid);
		transport.send(src, dest, m, destpid);
		KademliaObserver.msg_sent.add(1);

		if ( (m.getType() == Message.MSG_FIND) || (m.getType() == Message.MSG_REGISTER)|| (m.getType() == Message.MSG_TICKET_REQUEST) || (m.getType() == Message.MSG_TOPIC_QUERY)) { 
			Timeout t = new Timeout(destId, m.id, m.operationId);
			long latency = transport.getLatency(src, dest);

			// add to sent msg
			this.sentMsg.put(m.id, m.timestamp);
			EDSimulator.add(4 * latency, t, src, myPid); // set delay = 2*RTT
		}
	}

    private void makeRegisterDecision(Topic topic, int myPid) {

        long curr_time = CommonState.getTime();
        Ticket [] tickets = this.topicTable.makeRegisterDecisionForTopic(topic, curr_time);
        
        for (Ticket ticket : tickets) {
            Message m = ticket.getMsg();
            Message response  = new Message(Message.MSG_REGISTER_RESPONSE, ticket);
            response.ackId = m.id;
            response.operationId = m.operationId;
            sendMessage(response, ticket.getSrc().getId(), myPid);
        }
    }
    
    
    /**
     * 
     *
     */
    protected void handleRegister(Message m, int myPid) {
		Ticket ticket = (Ticket) m.body;
        long curr_time = CommonState.getTime();
        boolean add_event = topicTable.register_ticket(ticket, m, curr_time);

        // Setup a timeout event for the registration decision
        if (add_event) {
            Timeout timeout = new Timeout(ticket.getTopic());
            EDSimulator.add(KademliaCommonConfig.ONE_UNIT_OF_TIME, timeout, Util.nodeIdtoNode(this.node.getId()), myPid);
        }
    }
	
	/**
	 * Process a topic query message.<br>
	 * The body should contain a topic. Return a response message containing
     * the registrations for the topic and the neighbors close to the topic.
	 * 
	 * @param m
	 *            Message received (contains the node to find)
	 * @param myPid
	 *            the sender Pid
	 */
    protected void handleTopicQuery(Message m, int myPid) {
		Topic t = (Topic) m.body;
		TopicRegistration[] registrations = this.topicTable.getRegistration(t);
		BigInteger[] neighbours = this.routingTable.getNeighbours(Util.logDistance(t.getTopicID(), this.node.getId()));
		
		Message.TopicLookupBody body = new Message.TopicLookupBody(registrations, neighbours);
		Message response  = new Message(Message.MSG_TOPIC_QUERY_REPLY, body);
		response.operationId = m.operationId;
		response.src = this.node;
		response.ackId = m.id; 
		logger.info(" responds with TOPIC_QUERY_REPLY");
		sendMessage(response, m.src.getId(), myPid);
    
    }
    /**
     * Process a ticket request
     *
     */
    protected void handleTicketRequest(Message m, int myPid) {
        //FIXME add logs
        long curr_time = CommonState.getTime();
		//System.out.println("Ticket request received from " + m.src.getId()+" in node "+this.node.getId());
        Topic topic = (Topic) m.body;
        KademliaNode advertiser = new KademliaNode(m.src); 
        //System.out.println("TicketRequest handle "+topic.getTopic());
		transport = (UnreliableTransport) (Network.prototype).getProtocol(tid);
        long rtt_delay = 2*transport.getLatency(Util.nodeIdtoNode(m.src.getId()), Util.nodeIdtoNode(m.dest.getId()));
        Ticket ticket = topicTable.getTicket(topic, advertiser, rtt_delay, curr_time);
        // Send a response message with a ticket back to advertiser
		BigInteger[] neighbours = this.routingTable.getNeighbours(Util.logDistance(topic.getTopicID(), this.node.getId()));

    	Message.TicketReplyBody body = new Message.TicketReplyBody(ticket, neighbours);
		Message response  = new Message(Message.MSG_TICKET_RESPONSE, body);
	
        //Message response = new Message(Message.MSG_TICKET_RESPONSE, ticket);
		response.ackId = m.id; // set ACK number
		response.operationId = m.operationId;
        
        sendMessage(response, m.src.getId(), myPid);
        
    }

    /**
     * Process a ticket response and schedule a register message
     *
     */
    private void handleTicketResponse(Message m, int myPid) {
		Message.TicketReplyBody body = (Message.TicketReplyBody) m.body;
        Ticket ticket = body.ticket;
        System.out.println("Got response! Is topic queue full?" + ticket.topicOccupancy);
        Topic topic = ticket.getTopic();
        TicketTable tt = ticketTables.get(topic.getTopicID());
        tt.reportResponse(ticket);
        if (ticket.getWaitTime() == -1) 
        {   
            logger.warning("Attempted to re-register topic on the same node");
            tt.removeNeighbour(m.src.getId());
            return;
        }
        if(KademliaCommonConfig.TICKET_NEIGHBOURS==1) {  
        	
        	for(BigInteger node: body.neighbours)
        		routingTable.addNeighbour(node);
        	
        	
        	if(tt!=null) {
        		tt.addNeighbour(body.neighbours);
        	}
         	SearchTable st = searchTables.get(topic.getTopicID());
        	if(st != null) st.addNeighbour(body.neighbours);
        	
   	
        }
     
        tt.addTicket(m,ticket);
        tt.available_requests++;
        
        if(tt.available_requests > 0) {
        	BigInteger nextNode = tt.getNeighbour();
			if (nextNode != null) {
				sendTicketRequest(nextNode, topic,myPid);
			}
        	
        }
    }

	/**
	 * Process a register response message.<br>
	 * The body should contain a ticket, which indicates whether registration is 
     * complete. In case it is not, schedule sending a new register request
	 * 
	 * @param m
	 *            Message received (contains the node to find)
	 * @param myPid
	 *            the sender Pid
	 */
    protected void handleRegisterResponse(Message m, int myPid) {
        Ticket ticket = (Ticket) m.body;
        Topic topic = ticket.getTopic();
        if (!ticket.isRegistrationComplete()) {
        	logger.info("Unsuccessful Registration of topic: " + ticket.getTopic().getTopic() + " at node: " + m.src.toString() + " wait time: " + ticket.getWaitTime());
            Message register = new Message(Message.MSG_REGISTER, ticket);
            register.operationId = m.operationId;
            register.body = m.body;
            scheduleSendMessage(register, m.src.getId(), myPid, ticket.getWaitTime());
        }
        else {
            logger.info("Registration succesful for topic "+ticket.getTopic().topic+" at node " + m.src.getId() + " at dist "+ Util.logDistance(m.src.getId(), ticket.getTopic().getTopicID())+" "+ticket.getCumWaitTime());
            KademliaObserver.addAcceptedRegistration(topic, this.node.getId(),m.src.getId(),ticket.getCumWaitTime());
            KademliaObserver.reportActiveRegistration(ticket.getTopic(), this.node.is_evil);

        	if(KademliaCommonConfig.TICKET_REMOVE_AFTER_REG==0) {
        		Timeout timeout = new Timeout(ticket.getTopic(),m.src.getId());
            	EDSimulator.add(KademliaCommonConfig.AD_LIFE_TIME, timeout, Util.nodeIdtoNode(this.node.getId()), myPid);
        	} else {
            	ticketTables.get(ticket.getTopic().getTopicID()).removeNeighbour(m.src.getId());
            	ticketTables.get(ticket.getTopic().getTopicID()).addRegisteredList(m.src.getId());
         		Timeout timeout = new Timeout(ticket.getTopic(),m.src.getId());
            	EDSimulator.add(KademliaCommonConfig.AD_LIFE_TIME, timeout, Util.nodeIdtoNode(this.node.getId()), myPid);
   
        	}
        }
        
    }
    
	protected void handleTopicQueryReply(Message m, int myPid) {
		LookupTicketOperation lop = (LookupTicketOperation) this.operations.get(m.operationId);
		if (lop == null) {
			return;
		}

		Message.TopicLookupBody lookupBody = (Message.TopicLookupBody) m.body;
		BigInteger[] neighbours = lookupBody.neighbours;
		TopicRegistration[]  registrations = lookupBody.registrations;
		//System.out.println("Topic query reply for "+lop.operationId +" with " + registrations.length+ " replies "+lop.available_requests);
		logger.info(" Asked node from dist:"+Util.logDistance(lop.topic.topicID, m.src.getId()) + ": " +  m.src.getId() + " regs:" + registrations.length+ " neigh:"+neighbours.length + "-> ");
		for(BigInteger neighbour: neighbours) {
			logger.info(Util.logDistance(neighbour, lop.topic.topicID) + ", ");
		}
		
		lop.elaborateResponse(neighbours);
		lop.increaseReturned(m.src.getId());
		if(!lop.finished)lop.increaseUsed(m.src.getId());

		for(SearchTable st : searchTables.values()) {
			st.addNeighbour(neighbours);
		}
		
		
		for(BigInteger neighbour: neighbours) {
			routingTable.addNeighbour(neighbour);
			
			for(TicketTable tt : ticketTables.values())
				tt.addNeighbour(neighbour);
		}
	
		for(TopicRegistration r: registrations) {
			lop.addDiscovered(r.getNode());
			KademliaObserver.addDiscovered(lop.topic, m.src.getId(), r.getNode().getId());

		}

		int found = lop.discoveredCount();
		
		int all = KademliaObserver.topicRegistrationCount(lop.topic.getTopic());		
		//int required = Math.min(all, KademliaCommonConfig.TOPIC_PEER_LIMIT);
		int required = KademliaCommonConfig.TOPIC_PEER_LIMIT;

		if(!lop.finished && found >= required) {
			logger.warning("Found " + found + " registrations out of required " + required + "(" + all + ") for topic " + lop.topic.topic + " after consulting " + lop.getUsedCount() + " nodes.");
			//We should never use less than 2 hops
			//assert lop.getUsedCount() > 1;
			lop.finished = true;
		}
		
		
		while ((lop.available_requests > 0)) { // I can send a new find request
			// get an available neighbour
			if(lop.available_requests < KademliaCommonConfig.ALPHA) {
				return;
			} else if ((lop.finished&&lop.available_requests==KademliaCommonConfig.ALPHA)||KademliaCommonConfig.MAX_SEARCH_HOPS==lop.nrHops) { // no new neighbour and no outstanding requests
				// search operation finished
				operations.remove(lop.operationId);
				KademliaObserver.reportOperation(lop);
				if(!lop.finished) { 
					logger.warning("Found only " + found + " registrations out of " + all + " for topic " + lop.topic.topic + " after consulting " + lop.getUsedCount() + " nodes.");
				} 
				KademliaObserver.register_total.add(all);
				KademliaObserver.register_ok.add(found);
				node.setLookupResult(lop.getDiscoveredArray());
				if(printSearchTable)searchTables.get(lop.topic.getTopicID()).print();
				
				return;
			} else {
				BigInteger neighbour = lop.getNeighbour();
				if (neighbour != null) {

					// send a new request only if we didn't find the node already
					Message request = new Message(Message.MSG_TOPIC_QUERY);
					request.operationId = lop.operationId;
					request.src = this.node;
					request.body = lop.body;
					request.dest = new KademliaNode(neighbour);
	
					if(request != null) {
						lop.nrHops++;
						sendMessage(request, neighbour, myPid);
					}
				} else {
						lop.available_requests++;
					return;
				}
					
			} 
		}
	
	}
	
	

	/**
	 * Start a register topic opearation.<br>
	 * If this is an on-going register operation with a previously obtained 
     * ticket, then send a REGTOPIC message; otherwise,
     * Find the ALPHA closest node and send REGTOPIC message to them
	 * 
	 * @param m
	 *            Message received (contains the node to find)
	 * @param myPid
	 *            the sender Pid
	 */
    protected void handleInitRegisterTopic(Message m, int myPid) {
    	Topic t = (Topic) m.body;
    	
    	logger.warning("handleInitRegisterTopic "+t.getTopic()+" "+t.getTopicID()+" "+KademliaCommonConfig.TICKET_BUCKET_SIZE);
    	//restore the IF statement
    	KademliaObserver.addTopicRegistration(t, this.node.getId());

        TicketTable tt;
        if(KademliaCommonConfig.TICKET_BUCKET_SIZE==0)
        	tt = new TicketTable(KademliaCommonConfig.NBUCKETS,this,t,myPid,KademliaCommonConfig.TICKET_REFRESH==1);
        else
        	tt = new TicketTable(KademliaCommonConfig.NBUCKETS,KademliaCommonConfig.TICKET_BUCKET_SIZE,KademliaCommonConfig.TICKET_TABLE_REPLACEMENTS,this,t,myPid,KademliaCommonConfig.TICKET_REFRESH==1);
        tt.setNodeId(t.getTopicID());
        ticketTables.put(t.getTopicID(),tt);
        	
        for(int i = 0; i<= KademliaCommonConfig.BITS;i++) {
        	BigInteger[] neighbours = routingTable.getNeighbours(i);
        	tt.addNeighbour(neighbours);
        }
        if(printSearchTable) tt.print();
        
        
        for (int i = 0; i < KademliaCommonConfig.ALPHA; i++) {
			BigInteger nextNode = tt.getNeighbour();
			if (nextNode != null) {
				sendTicketRequest(nextNode, t,myPid);
			}
		}
        
        //Register messages are automatically sent when adding Neighbours
        

  }
	
    /**
	 * Start a topic query opearation.<br>
	 * 
	 * @param m
	 *            Message received (contains the node to find)
	 * @param myPid
	 *            the sender Pid
	 */
    
    private void handleInitTopicLookup(Message m, int myPid) {
		Topic t = (Topic) m.body;
	
		logger.warning("Send init lookup for topic "+this.node.getId()+" "+t.getTopic());
      	SearchTable rou = new SearchTable(KademliaCommonConfig.NBUCKETS,KademliaCommonConfig.SEARCH_BUCKET_SIZE,KademliaCommonConfig.SEARCH_TABLE_REPLACEMENTS,this,t,myPid,KademliaCommonConfig.SEARCH_BUCKET_SIZE==1);
       	rou.setNodeId(t.getTopicID());
       	searchTables.put(t.getTopicID(),rou);
        	
       	for(int i = 0; i<= KademliaCommonConfig.BITS;i++) {
       		BigInteger[] neighbours = routingTable.getNeighbours(i);
       		if(neighbours.length!=0) rou.addNeighbour(neighbours);
       	}
       	
       	/*Message message = Message.makeInitFindNode(t.getTopicID());
   		message.timestamp = CommonState.getTime();
    		
   		EDSimulator.add(0, message, Util.nodeIdtoNode(this.node.getId()), myPid);*/
       	
        if(printSearchTable)rou.print();
        sendTopicLookup(m,t,myPid);
        
 
    }
    
    public void sendLookup(BigInteger node, int myPid)
    {
		Message message = Message.makeInitFindNode(node);
		message.timestamp = CommonState.getTime();
		
		EDSimulator.add(0, message, Util.nodeIdtoNode(this.node.getId()), myPid);
		//System.out.println("Send init lookup to node "+Util.logDistance(node, this.getNode().getId()));

  
    }
    
    public void sendTopicLookup(Message m,Topic t,int myPid) {
    	
		KademliaObserver.lookup_total.add(1);

		LookupTicketOperation lop = new LookupTicketOperation(this.node.getId(), this.searchTables.get(t.getTopicID()), m.timestamp, t);
		lop.body = m.body;
		operations.put(lop.operationId, lop);
	
		m.operationId = lop.operationId;
		m.type = Message.MSG_TOPIC_QUERY;
		m.src = this.node;
	
		// send ALPHA messages
		for (int i = 0; i < KademliaCommonConfig.ALPHA; i++) {
			BigInteger nextNode = lop.getNeighbour();
			if (nextNode != null) {
				m.dest = new KademliaNode(nextNode);
				sendMessage(m.copy(), nextNode, myPid);
				//System.out.println("Send topic lookup to: " + nextNode +" at distance:"+Util.logDistance(lop.topic.topicID, nextNode));
				lop.nrHops++;
			}
		}
    }

    
	/**
	 * Perform the required operation upon receiving a message in response to a ROUTE message.<br>
	 * Update the find operation record with the closest set of neighbour received. Then, send as many ROUTE request I can
	 * (according to the ALPHA parameter).<br>
	 * If no closest neighbour available and no outstanding messages stop the find operation.
	 * 
	 * @param m
	 *            Message
	 * @param myPid
	 *            the sender Pid
	 */
	protected void handleResponse(Message m, int myPid) {
		if(m.getType() == Message.MSG_RESPONSE) {
			BigInteger[] neighbours = (BigInteger[]) m.body;
			//if(neighbours.length!=0)logger.warning("Find response received at "+this.node.getId()+" from "+m.src.getId()+" with "+neighbours.length+" neighbours");
			for(SearchTable table : searchTables.values())
	    		table.addNeighbour(neighbours);
			
			for(TicketTable table : ticketTables.values())
	    		table.addNeighbour(neighbours);

		}
		super.handleResponse(m, myPid);
	}
	
   public void sendTicketRequest(BigInteger dest,Topic t,int myPid) {
    	logger.warning("Sending ticket request to " + dest + " for topic " + t.topic);
    	TicketTable tt = ticketTables.get(t.topicID);
        TicketOperation top = new TicketOperation(this.node.getId(), CommonState.getTime(), t);
 		top.body = t;
 		operations.put(top.operationId, top);
         
         // Lookup the target address in the routing table
 		BigInteger[] neighbours = new BigInteger[] {dest};
 		
        top.elaborateResponse(neighbours); 
 		top.available_requests = KademliaCommonConfig.ALPHA;
 		
 		Message m = new Message(Message.MSG_TICKET_REQUEST, t);
		m.timestamp = CommonState.getTime();
         // set message operation id
 		m.operationId = top.operationId;
 		m.src = this.node;
 		
 		//System.out.println("Send ticket request to "+dest+" for topic "+t.getTopic());
 		sendMessage(m,top.getNeighbour(),myPid);
 		tt.available_requests--;
 		System.out.println("available_requests:" + tt.available_requests);


   }

	private void handleTimeout(Timeout t, int myPid){
		Operation op = this.operations.get(t.opID);
		if(op!=null) {	
			//logger.warning("Timeout "+t.getType());
			BigInteger unavailableNode = t.node;
			if(op.type == Message.MSG_TOPIC_QUERY) {
				Message m = new Message();
				m.operationId = op.operationId;
				m.type = Message.MSG_TOPIC_QUERY_REPLY;
				m.src = new KademliaNode (unavailableNode);
				m.dest = this.node;
				m.ackId = t.msgID; 
				m.body=  new Message.TopicLookupBody(new TopicRegistration [0], new BigInteger[0]);
				handleTopicQueryReply(m, myPid);
			}
		}
	}
	
	
	/**
	 * manage the peersim receiving of the events
	 * 
	 * @param myNode
	 *            Node
	 * @param myPid
	 *            int
	 * @param event
	 *            Object
	 */
    public void processEvent(Node myNode, int myPid, Object event) {
        
		//this.discv5id = myPid;
    	if(topicTable==null)return;
		super.processEvent(myNode, myPid, event);
        Message m;
        
        if(!myNode.isUp()) {
            System.out.println("Removed nodes are receiving traffic");
            System.exit(1);
        }
		
		if(((SimpleEvent) event).getType() == Timeout.TIMEOUT) {
			handleTimeout((Timeout) event, myPid);
			return;
		}

	    SimpleEvent s = (SimpleEvent) event;
        if (s instanceof Message) {
	        m = (Message) event;
            m.dest = this.node;
        }
		
        switch (((SimpleEvent) event).getType()) {

			case Message.MSG_TOPIC_QUERY_REPLY:
				m = (Message) event;
				sentMsg.remove(m.ackId);
				handleTopicQueryReply(m, myPid);
				break;

            case Message.MSG_REGISTER:
                m = (Message) event;
                handleRegister(m, myPid);
                break;

            case Message.MSG_REGISTER_RESPONSE:
                m = (Message) event;
				sentMsg.remove(m.ackId);
                handleRegisterResponse(m, myPid);
                break;

            case Message.MSG_TOPIC_QUERY:
                m = (Message) event;
                handleTopicQuery(m, myPid);
                break;
			
            case Message.MSG_INIT_TOPIC_LOOKUP:
				m = (Message) event;
				handleInitTopicLookup(m, myPid);
				break;

            case Message.MSG_INIT_REGISTER:
                m = (Message) event;
                handleInitRegisterTopic(m, myPid);
                break;
            
            case Message.MSG_TICKET_REQUEST:
                m = (Message) event;
                handleTicketRequest(m, myPid);
                break;
            
            case Message.MSG_TICKET_RESPONSE:
                m = (Message) event;
				sentMsg.remove(m.ackId);
                handleTicketResponse(m, myPid);
                break;

            case Timeout.TICKET_TIMEOUT:
                Topic t = ((Timeout)event).topic;
                makeRegisterDecision(t, myPid);
                break;

            case Timeout.REG_TIMEOUT:
            	//logger.warning("Remove ticket table "+((Timeout)event).nodeSrc);
            	KademliaObserver.reportExpiredRegistration(((Timeout)event).topic, this.node.is_evil);
            	if(KademliaCommonConfig.TICKET_REMOVE_AFTER_REG==0) {
            		ticketTables.get(((Timeout)event).topic.getTopicID()).removeNeighbour(((Timeout)event).nodeSrc);
            	} else {
            		ticketTables.get(((Timeout)event).topic.getTopicID()).removeRegisteredList(((Timeout)event).nodeSrc);
            	}
            	break;

			case Timeout.TIMEOUT: // timeout
				Timeout timeout = (Timeout) event;
				if (sentMsg.containsKey(timeout.msgID)) { // the response msg didn't arrived
					logger.warning("Node " + this.node.getId() + " received a timeout: " + timeout.msgID + " from: " + timeout.node);
					// remove form sentMsg
					sentMsg.remove(timeout.msgID);
					
				}
				break;
        }
    }
	
    /**
	 * set the current NodeId
	 * 
	 * @param tmp
	 *            BigInteger
	 */
	public void setNode(KademliaNode node) {
		this.topicTable.setHostID(node.getId());
		super.setNode(node);
	}
	
	/**
	 * Check nodes and replace buckets with valid nodes from replacement list
	 * 
	 */
	public void refreshBuckets() {

		if(topicTable==null)return;
		for(TicketTable ttable : ticketTables.values())
			ttable.refreshBuckets();
		for(SearchTable stable : searchTables.values())
			stable.refreshBuckets();
			//stable.refreshBuckets(kademliaid, otherProtocolId);
		
		this.routingTable.refreshBuckets();
		//this.routingTable.refreshBuckets(kademliaid, otherProtocolId);

	}
	
	public void refreshBucket(TicketTable rou, BigInteger node, int distance) {
	

	      BigInteger[] neighbours = routingTable.getNeighbours(Util.logDistance(node, this.getNode().getId()));
	      rou.addNeighbour(neighbours);
	        
       
	}
	
	public void onKill()
	{
		//System.out.println("Node removed");
		topicTable = null;
		ticketTables=null;
		searchTables=null;
		routingTable=null;
		operations=null;
	}
	
	
}
