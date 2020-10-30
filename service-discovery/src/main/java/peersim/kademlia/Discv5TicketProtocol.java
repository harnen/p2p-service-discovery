package peersim.kademlia;

/**
 * Discv5 Protocol implementation.
 *
 */ 
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.TreeMap;

import peersim.kademlia.Topic;
import peersim.kademlia.operations.LookupTicketOperation;
import peersim.kademlia.operations.TicketOperation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import peersim.config.Configuration;
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

public class Discv5TicketProtocol extends KademliaProtocol {

    /**
	 * Topic table of this node
	 */
    public Discv5TopicTable topicTable;

    /**
	 * Table to keep track of topic registrations
	 */
    protected HashMap<BigInteger,TicketTable> ticketTable;
    
    /**
	 * Table to search for topics
	 */
    private HashMap<BigInteger,SearchTable> searchTable;
    
	final String PAR_TOPIC_TABLE_CAP = "TOPIC_TABLE_CAP";
	final String PAR_ADS_PER_QUEUE = "ADS_PER_QUEUE";
	final String PAR_AD_LIFE_TIME = "AD_LIFE_TIME";
	
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
        ticketTable = new HashMap<BigInteger,TicketTable>();
        searchTable = new HashMap<BigInteger,SearchTable>();
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

		//System.out.println(this.kademliaNode.getId() + " (" + m + "/" + m.id + ") -> " + destId);

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

		if ( (m.getType() == Message.MSG_FIND) || (m.getType() == Message.MSG_REGISTER)|| (m.getType() == Message.MSG_TICKET_REQUEST) ) { 
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
            /*if (ticket.isRegistrationComplete()) {
                //handleFind(m, myPid, Util.logDistance(ticket.getTopic().getTopicID(), this.node.getId()));
            }
            else {*/
                Message response  = new Message(Message.MSG_REGISTER_RESPONSE, ticket);
                response.ackId = m.id;
                response.operationId = m.operationId;
                sendMessage(response, ticket.getSrc().getId(), myPid);
           // }
        }
    }
    
    
    /**
     * 
     *
     */
    private void handleRegister(Message m, int myPid) {
		Ticket ticket = (Ticket) m.body;
        topicTable.register_ticket(ticket, m);
    	//System.out.println("Register ticket at "+this.node.getId()+" for topic "+ticket.getTopic().getTopic());

		
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
		
		//System.out.println("Topic query received at node "+this.node.getId()+" "+registrations.length+" "+neighbours.length);

		Message.TopicLookupBody body = new Message.TopicLookupBody(registrations, neighbours);
		Message response  = new Message(Message.MSG_TOPIC_QUERY_REPLY, body);
		response.operationId = m.operationId;
		response.src = this.node;
		response.ackId = m.id; 
		logger.info(" responds with TOPIC_QUERY_REPLY");
		//System.out.println(" responds with TOPIC_QUERY_REPLY");
		sendMessage(response, m.src.getId(), myPid);
    
    }
    /**
     * Process a ticket request
     *
     */
    private void handleTicketRequest(Message m, int myPid) {
        //FIXME add logs
        long curr_time = CommonState.getTime();
		//System.out.println("Ticket request received from " + m.src.getId()+" in node "+this.node.getId());
        Topic topic = (Topic) m.body;
        KademliaNode advertiser = new KademliaNode(m.src); 
        //System.out.println("TicketRequest handle "+topic.getTopic());
		transport = (UnreliableTransport) (Network.prototype).getProtocol(tid);
        long rtt_delay = 2*transport.getLatency(Util.nodeIdtoNode(m.src.getId()), Util.nodeIdtoNode(m.dest.getId()));
        Ticket ticket = topicTable.getTicket(topic, advertiser, rtt_delay, curr_time);

        // Setup a timeout event for the registration decision
        if (ticket.getWaitTime() >= 0) {
            Timeout timeout = new Timeout(topic);
            EDSimulator.add(rtt_delay + ticket.getWaitTime() + KademliaCommonConfig.ONE_UNIT_OF_TIME, timeout, Util.nodeIdtoNode(this.node.getId()), myPid);
        }
        // Send a response message with a ticket back to advertiser
        Message response = new Message(Message.MSG_TICKET_RESPONSE, ticket);
		response.ackId = m.id; // set ACK number
		response.operationId = m.operationId;
        sendMessage(response, m.src.getId(), myPid);
    }

    /**
     * Process a ticket response and schedule a register message
     *
     */
    private void handleTicketResponse(Message m, int myPid) {
        Ticket t = (Ticket) m.body;
        if (t.getWaitTime() == -1) 
        {   
            logger.warning("Attempted to re-register topic on the same node");
            ticketTable.get(t.getTopic().getTopicID()).removeNeighbour(m.src.getId());
            return;
        }
    	//System.out.println("handleTicketResponse from " + m.src.getId()+" waiting time "+t.getWaitTime()+" "+this.node.getId());

    	//System.out.println("handleTicketResponse from " + m.src.getId()+" waiting time "+t.getWaitTime()+" "+this.node.getId());

        ticketTable.get(t.getTopic().getTopicID()).addTicket(m,t);
        //scheduleSendMessage(register, m.src.getId(), myPid, t.getWaitTime());
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
        if (ticket.isRegistrationComplete() == false) {
        	logger.warning("Unsuccessful Registration of topic: " + ticket.getTopic().getTopicID() + " at node: " + m.src.toString() + " wait time: " + ticket.getWaitTime());
            Message register = new Message(Message.MSG_REGISTER, ticket);
            register.operationId = m.operationId;
            register.body = m.body;
            scheduleSendMessage(register, m.src.getId(), myPid, ticket.getWaitTime());
        }
        else {
            logger.warning("Registration succesful for topic "+ticket.getTopic().getTopicID()+" at node "+m.src.getId());
            KademliaObserver.addTopicRegistration(topic.getTopic(), this.node.getId());

            Timeout timeout = new Timeout(ticket.getTopic(),m.src.getId());
            EDSimulator.add(KademliaCommonConfig.AD_LIFE_TIME, timeout, Util.nodeIdtoNode(this.node.getId()), myPid);
            
        }
    }
    
	protected void handleTopicQueryReply(Message m, int myPid) {
		//System.out.println("Topic query reply");
		LookupTicketOperation lop = (LookupTicketOperation) this.operations.get(m.operationId);
		if (lop == null) {
			return;
		}

		Message.TopicLookupBody lookupBody = (Message.TopicLookupBody) m.body;
		BigInteger[] neighbours = lookupBody.neighbours;
		TopicRegistration[]  registrations = lookupBody.registrations;
		//System.out.println("Topic query reply for "+lop.operationId +" with " + registrations.length+ " replies "+lop.available_requests);

		lop.elaborateResponse(neighbours);
		
		lop.increaseReturned(m.src.getId());
		if(!lop.finished)lop.increaseUsed(m.src.getId());

		for(BigInteger neighbour: neighbours) {
			routingTable.addNeighbour(neighbour);
			for(SearchTable st : searchTable.values())
				st.addNeighbour(neighbour);
			
			for(TicketTable tt : ticketTable.values())
				tt.addNeighbour(neighbour);
		}
	
		//lop.decreaseRequests();	
		for(TopicRegistration r: registrations) {
			lop.addDiscovered(r.getNode());
		}

		//System.out.println("Topic query reply received for "+lop.topic.getTopic()+" "+this.getNode().getId()+" "+lop.discoveredCount()+" "+lop.getUsedCount()+" "+lop.getReturnedCount());
		//if(registrations.length==0) searchTable.get(lop.topic.getTopicID()).removeNeighbour(m.src.getId());

		
		int found = lop.discoveredCount();
		
		//logger.warning("Topic query reply "+ m.src.getId()+" "+lop.available_requests+" found "+found);

		int all = KademliaObserver.topicRegistrationCount(lop.topic.getTopic());		
		int required = Math.min(all, KademliaCommonConfig.TOPIC_PEER_LIMIT);
		

		if(!lop.finished && found >= required) {
			logger.warning("Found " + found + " registrations out of required " + required + "(" + all + ") for topic " + lop.topic.topic);
			lop.finished = true;
		}
		
		while ((lop.available_requests > 0)) { // I can send a new find request
			// get an available neighbour
			//logger.warning("Topic query reply loop "+ m.src.getId()+" "+lop.available_requests+" found "+found);
			if(lop.finished&&lop.available_requests< KademliaCommonConfig.ALPHA) {
				//logger.warning("Topic query reply return"+ m.src.getId()+" "+lop.available_requests+" found "+found);
				return;
			} else if (lop.available_requests == KademliaCommonConfig.ALPHA) { // no new neighbour and no outstanding requests
				// search operation finished
				//logger.warning("Topic query reply alpha"+ m.src.getId()+" "+lop.available_requests+" found "+found);
				operations.remove(lop.operationId);
				//logger.warning("reporting operation " + lop.operationId);
				KademliaObserver.reportOperation(lop);
				//lop.visualize(); uncomment if you want to see visualization of the operation
				if(!lop.finished) { 
					logger.warning("Found only " + found + " registrations out of " + all + " for topic " + lop.topic.topic);
				} 
				//System.out.println("Writing stats");
				KademliaObserver.register_total.add(all);
				KademliaObserver.register_ok.add(found);
									
				node.setLookupResult(lop.getDiscoveredArray());
				return;
			} else {
				BigInteger neighbour = lop.getNeighbour();
				if (neighbour != null) {
					//logger.warning("Topic query reply neighbour not null "+ m.src.getId()+" "+lop.available_requests+" found "+found);
					//if(!lop.finished) {
					// send a new request only if we didn't find the node already
					Message request = new Message(Message.MSG_REGISTER);
					request.operationId = lop.operationId;
					request.type = Message.MSG_TOPIC_QUERY;
					request.src = this.node;
					request.body = lop.body;
					request.dest = new KademliaNode(neighbour);
	
					if(request != null) {
						lop.nrHops++;
						sendMessage(request, neighbour, myPid);
					}
				} else {
					//logger.warning("Topic query reply neighbour not null finished "+ this.node.getId()+" "+lop.available_requests+" found "+found);

					//getNeighbour decreases available_requests, but we didn't send a message
					lop.available_requests++;
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
        
        /*Topic t = (Topic) m.body;
        t.setHostID(this.node.getId());
		
        KademliaObserver.addTopicRegistration(t.getTopic(), this.node.getId());


        //System.out.println("Neighbors: " + Arrays.toString(neighbours));
        //System.out.println("My id is: " + this.kademliaNode.getId().toString());
        //System.out.println("Target id is: " + targetAddr.toString());

        TicketOperation top = new TicketOperation(m.timestamp, t);
		top.body = m.body;
		operations.put(top.operationId, top);
        
        // Lookup the target address in the routing table
		BigInteger[] neighbours = this.routingTable.getNeighbours(Util.logDistance((BigInteger) t.getTopicID(), this.node.getId()));
		
        if(neighbours.length<KademliaCommonConfig.K)
			neighbours = this.routingTable.getKClosestNeighbours(KademliaCommonConfig.K);

        top.elaborateResponse(neighbours); 
		top.available_requests = KademliaCommonConfig.ALPHA;
		
        // set message operation id
		m.operationId = top.operationId;
		m.type = Message.MSG_TICKET_REQUEST;
		m.src = this.node;

		// send ALPHA messages
		for (int i = 0; i < KademliaCommonConfig.ALPHA; i++) {
			BigInteger nextNode = top.getNeighbour();
			if (nextNode != null) {
                Message ticket_request = m.copy();
                scheduleSendMessage(ticket_request, nextNode, myPid, 0); 
				top.nrHops++;
			}
			else {
				System.err.println("In register Returned neighbor is NUll !");
				//System.exit(-1);
			}
		}*/
    	
    	Topic t = (Topic) m.body;
        //t.setHostID(this.node.getId());
		
    	//logger.warning("Sending topic registration for topic "+t.getTopic());
    	
       // KademliaObserver.addTopicRegistration(t.getTopic(), this.node.getId());

        
        if(!ticketTable.containsKey(t.getTopicID())) {
        	//TicketTable rou = new TicketTable(KademliaCommonConfig.NBUCKETS,3,10,this,t,myPid);
        	TicketTable rou = new TicketTable(KademliaCommonConfig.NBUCKETS,3,10,this,t,myPid);
        	rou.setNodeId(t.getTopicID());
        	ticketTable.put(t.getTopicID(),rou);
        	
        	for(int i = 0; i<= KademliaCommonConfig.BITS;i++) {
        		BigInteger[] neighbours = routingTable.getNeighbours(i);
        		//if(neighbours.length!=0)logger.warning("Bucket at distance "+i+" with "+neighbours.length+" nodes");
        		//else logger.warning("Bucket at distance "+i+" empty");
        		rou.addNeighbour(neighbours);
        	}
        }
    	//logger.warning("Sending topic registration for topic "+t.getTopic()+" done");

        //sendLookup(t,myPid);
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
		/*KademliaObserver.lookup_total.add(1);

		Topic t = (Topic) m.body;
		
        LookupOperation lop = new LookupOperation(m.timestamp, t);
		lop.body = m.body;
		operations.put(lop.operationId, lop);
	
		//BigInteger[] neighbours = this.routingTable.getNeighbours((BigInteger) m.body, this.node.getId());
		BigInteger[] neighbours = this.routingTable.getNeighbours(Util.logDistance((BigInteger) t.getTopicID(), this.node.getId()));
		
        if(neighbours.length<KademliaCommonConfig.K)
			neighbours = this.routingTable.getKClosestNeighbours(KademliaCommonConfig.K);
		lop.elaborateResponse(neighbours);
		lop.available_requests = KademliaCommonConfig.ALPHA;
	
		// set message operation id
		m.operationId = lop.operationId;
		m.type = Message.MSG_TOPIC_QUERY;
		m.src = this.node;
	
		// send ALPHA messages
		for (int i = 0; i < KademliaCommonConfig.ALPHA; i++) {
			BigInteger nextNode = lop.getNeighbour();
			if (nextNode != null) {
				sendMessage(m.copy(), nextNode, myPid);
				lop.nrHops++;
                //System.out.println("Topic lookup for: " + t);
			}
			else {
				System.err.println("In Topic Lookup Returned neighbor is NUll !");
				//System.exit(-1);
			}
		}*/
    	
    	//KademliaObserver.lookup_total.add(1);

		Topic t = (Topic) m.body;
    	
		logger.warning("Send init lookup for topic "+this.node.getId()+" "+t.getTopic());

        if(!searchTable.containsKey(t.getTopicID())) {
        	SearchTable rou = new SearchTable(KademliaCommonConfig.NBUCKETS,KademliaCommonConfig.K,10,this,t,myPid);
        	rou.setNodeId(t.getTopicID());
        	searchTable.put(t.getTopicID(),rou);
        	
            
        	Message message = Message.makeInitFindNode(t.getTopicID());
    		message.timestamp = CommonState.getTime();
    		
    		EDSimulator.add(0, message, Util.nodeIdtoNode(this.node.getId()), myPid);
        } else {
        	sendTopicLookup(m,t,myPid);
        }
  
 
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
		
		//Topic t = (Topic) m.body;
		//logger.warning("Send topic lookup for topic "+this.node.getId()+" "+t.getTopic());

		LookupTicketOperation lop = new LookupTicketOperation(this.node.getId(), this.searchTable.get(t.getTopicID()), m.timestamp, t);
		lop.body = m.body;
		operations.put(lop.operationId, lop);
	
		/*int distToTopic = Util.logDistance((BigInteger) t.getTopicID(), this.node.getId());
		BigInteger[] neighbours = this.routingTable.getNeighbours(distToTopic);
		
		
		if(neighbours.length<KademliaCommonConfig.ALPHA)
			neighbours = this.routingTable.getKClosestNeighbours(KademliaCommonConfig.ALPHA, distToTopic);
		
		lop.elaborateResponse(neighbours);
		lop.available_requests = KademliaCommonConfig.ALPHA;*/
	
		// set message operation id
		m.operationId = lop.operationId;
		m.type = Message.MSG_TOPIC_QUERY;
		m.src = this.node;
	
		// send ALPHA messages
		for (int i = 0; i < KademliaCommonConfig.ALPHA; i++) {
			BigInteger nextNode = lop.getNeighbour();
			if (nextNode != null) {
				m.dest = new KademliaNode(nextNode);
				sendMessage(m.copy(), nextNode, myPid);
				//System.out.println("Send topic lookup "+ this.node.getId()+" "+lop.available_requests);
				lop.nrHops++;
			}
		}
		
    	/*KademliaObserver.lookup_total.add(1);
		
        LookupOperation lop = new LookupOperation(this.node.getId(), m.timestamp, t);
		operations.put(lop.operationId, lop);

		//BigInteger[] neighbours = this.routingTable.getNeighbours((BigInteger) m.body, this.node.getId());
		//BigInteger[] neighbours = this.routingTable.getNeighbours(Util.logDistance((BigInteger) t.getTopicID(), this.node.getId()));
		
        //if(neighbours.length<KademliaCommonConfig.K)
		BigInteger[] neighbours = new BigInteger[0];
		int tried=0,sent=0;
		//System.out.println("Neighbours for distance "+neighbours.length);
		
		
		// set message operation id
		m.operationId = lop.operationId;
		m.type = Message.MSG_TOPIC_QUERY;
		m.src = this.node;
		//lop.available_requests = KademliaCommonConfig.ALPHA;

		//lop.elaborateResponse(neighbours);
	
		//lop.available_requests = 0;
		while(tried<KademliaCommonConfig.NBUCKETS&&sent<KademliaCommonConfig.ALPHA) {
			int distance = CommonState.r.nextInt(KademliaCommonConfig.NBUCKETS);
			//System.out.println("Distance "+distance);
			int tries=0;
			while((neighbours.length==0)&&(tries<KademliaCommonConfig.NBUCKETS)) {
				//System.out.println("Distance "+distance);
				distance = CommonState.r.nextInt(KademliaCommonConfig.NBUCKETS);
				tries++;
				if(lop.isUsed(distance))continue;
				//System.out.println("Distance "+distance);
				neighbours = this.searchTable.get(t.getTopicID()).getNeighbours(distance);
				//System.out.println("Distance "+distance+" "+neighbours.length);
			}
			lop.addUsed(distance);
			lop.elaborateResponse(neighbours);
			//lop.addUsed(distance);
			//System.out.println("Neighbours for distance "+distance+" "+neighbours.length);
			tried++;
			if(neighbours.length!=0) {
				BigInteger node = neighbours[CommonState.r.nextInt(neighbours.length)];
				sendMessage(m.copy(), node, myPid);
				lop.increaseRequests();
				this.searchTable.get(t.getTopicID()).removeNeighbour(node);
				//System.out.println("Send topic lookup id "+lop.operationId+" "+lop.availableRequests());
				sent++;
			}
	
		}*/
		
		/*if(sent<KademliaCommonConfig.ALPHA) {
			System.out.println("Sent only "+sent +" lookups");
		} else {
			System.out.println("Sent "+sent +" lookups");	
		}*/
		
    	
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
			for(SearchTable table : searchTable.values())
	    		table.addNeighbour(neighbours);
			
			for(TicketTable table : ticketTable.values())
	    		table.addNeighbour(neighbours);

		}
		super.handleResponse(m, myPid);
	}
	
   public void sendTicketRequest(BigInteger dest,Topic t,int myPid) {
    	
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

 		// send ALPHA messages
 		/*for (int i = 0; i < KademliaCommonConfig.ALPHA; i++) {
 			BigInteger nextNode = top.getNeighbour();
 			if (nextNode != null) {
                 Message ticket_request = m.copy();
                 sendMessage(ticket_request,nextNode,myPid);
                 //scheduleSendMessage(ticket_request, nextNode, myPid, 0); 
 				top.nrHops++;
 			}
 			else {
 				System.err.println("In register Returned neighbor is NUll !");
 				//System.exit(-1);
 			}
 		}*/
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
		super.processEvent(myNode, myPid, event);
        Message m;

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
            	logger.warning("Remove ticket table "+((Timeout)event).nodeSrc);
            	ticketTable.get(((Timeout)event).topic.topicID).removeNeighbour(((Timeout)event).nodeSrc);
            	break;

			/*case Timeout.TIMEOUT: // timeout
				Timeout t = (Timeout) event;
				if (sentMsg.containsKey(t.msgID)) { // the response msg didn't arrived
					System.out.println("Node " + this.kademliaNode.getId() + " received a timeout: " + t.msgID + " from: " + t.node);
					// remove form sentMsg
					sentMsg.remove(t.msgID);
					// remove node from my routing table
					this.routingTable.removeNeighbour(t.node);
					// remove from closestSet of find operation
					this.fop.closestSet.remove(t.node);
				}
				break;*/
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
		//System.out.print(topicTable.dumpRegistrations());
		for(TicketTable ttable : ticketTable.values())
			ttable.refreshBuckets();
		for(SearchTable stable : searchTable.values())
			stable.refreshBuckets();
			//stable.refreshBuckets(kademliaid, otherProtocolId);
		
		this.routingTable.refreshBuckets();
		//this.routingTable.refreshBuckets(kademliaid, otherProtocolId);

	}
	
	
}
