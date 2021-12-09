package src.main.java.peersim.kademlia;


import java.math.BigInteger;
import java.util.HashSet;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import peersim.kademlia.operations.LookupOperation;
import peersim.kademlia.operations.Operation;
import peersim.kademlia.operations.RegisterOperation;
import peersim.transport.UnreliableTransport;




public class Discv5DHTProtocol extends KademliaProtocol {

	public Discv5NoTicketTopicTable topicTable;
	final String PAR_TOPIC_TABLE_CAP = "TOPIC_TABLE_CAP";

	public Discv5DHTProtocol(String prefix) {
		super(prefix);
		this.topicTable = new Discv5NoTicketTopicTable();

		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Replicate this object by returning an identical copy.<br>
	 * It is called by the initializer and do not fill any particular field.
	 * 
	 * @return Object
	 */
	public Object clone() {
		Discv5DHTProtocol dolly = new Discv5DHTProtocol(Discv5DHTProtocol.prefix);
		return dolly;
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
		
		super._init();
	}
	
	
	protected void handleTopicQueryReply(Message m, int myPid) {
		LookupOperation lop = (LookupOperation) this.operations.get(m.operationId);
		if (lop == null) {
			return;
		}
		
		BigInteger[] neighbours = ((Message.TopicLookupBody) m.body).neighbours;
		TopicRegistration[] registrations = ((Message.TopicLookupBody) m.body).registrations;
		lop.elaborateResponse(neighbours);
		for(BigInteger neighbour: neighbours)
			routingTable.addNeighbour(neighbour);
		

		for(TopicRegistration r: registrations) {
			lop.addDiscovered(r.getNode(),m.src.getId());
			KademliaObserver.addDiscovered(lop.topic, this.node.getId(), r.getNode().getId());
		}
		
		lop.increaseReturned(m.src.getId());
		if(!lop.finished)lop.increaseUsed(m.src.getId());

		//System.out.println("Topic query reply received for "+lop.topic.getTopic()+" "+this.getNode().getId()+" "+lop.discoveredCount()+" "+lop.getUsedCount()+" "+lop.getReturnedCount());

		
		int found = lop.discoveredCount();
		int all = KademliaObserver.topicRegistrationCount(lop.topic.topic);
		int required = KademliaCommonConfig.TOPIC_PEER_LIMIT;//Math.min(all, KademliaCommonConfig.TOPIC_PEER_LIMIT);
		if(!lop.finished && found >= required) {
			logger.warning("Found " + found + " registrations out of required " + required + "(" + all + ") for topic " + lop.topic.topic);
			lop.finished = true;
		}

		while ((lop.available_requests > 0)) { // I can send a new find request
			// get an available neighbour
			BigInteger neighbour = lop.getNeighbour();
			if (neighbour != null) {
				if(!lop.finished) {
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
				}else {
					//getNeighbour decreases available_requests, but we didn't send a message
					lop.available_requests++;
				}
					
			} else if (lop.available_requests == KademliaCommonConfig.ALPHA) { // no new neighbour and no outstanding requests
				// search operation finished
				operations.remove(lop.operationId);
				//lop.visualize();
				logger.warning("reporting operation " + lop.operationId);
				KademliaObserver.reportOperation(lop);
				//lop.visualize(); uncomment if you want to see visualization of the operation
				if(!lop.finished) { 
					logger.warning("Found only " + found + " registrations out of " + all + " for topic " + lop.topic.topic);
				}
				//System.out.println("Writing stats");
				KademliaObserver.register_total.add(all);
				KademliaObserver.register_ok.add(found);
									
				node.setLookupResult(lop.getDiscovered(),lop.topic.getTopic());
				return;
			} else { // no neighbour available but exists oustanding request to wait
				return;
			}
		}
	}
	

	
	private void handleInitTopicLookup(Message m, int myPid) {
		KademliaObserver.lookup_total.add(1);
		
		Topic t = (Topic) m.body;
	
		//System.out.println("Send topic lookup for topic "+t.getTopic());

		LookupOperation lop = new LookupOperation(this.node.getId(), m.timestamp, t);
		lop.body = m.body;
		lop.type = Message.MSG_TOPIC_QUERY;
		operations.put(lop.operationId, lop);
	
		int distToTopic = Util.logDistance((BigInteger) t.getTopicID(), this.node.getId());
		BigInteger[] neighbours = this.routingTable.getNeighbours(distToTopic);
		
		
		if(neighbours.length<KademliaCommonConfig.ALPHA)
			neighbours = this.routingTable.getKClosestNeighbours(KademliaCommonConfig.ALPHA, distToTopic);
		
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
				m.dest = new KademliaNode(nextNode);
				sendMessage(m.copy(), nextNode, myPid);
				lop.nrHops++;
			}
		}
		
	}



	/**
	 * Start a register opearation.<br>
	 * Find the ALPHA closest node and send register request to them.
	 * 
	 * @param m
	 *            Message received (contains the node to find)
	 * @param myPid
	 *            the sender Pid
	 */
	protected void handleInitRegister(Message m, int myPid) {
		Topic t = (Topic) m.body;
		TopicRegistration r = new TopicRegistration(this.node, t);
    	logger.info("Sending topic registration for topic "+t.getTopic());

		KademliaObserver.addTopicRegistration(t, this.node.getId());
	
		RegisterOperation rop = new RegisterOperation(this.node.getId(), m.timestamp, t, r);
		rop.body = m.body;
		rop.type = Message.MSG_REGISTER;
		operations.put(rop.operationId, rop);
		
		int distToTopic = Util.logDistance((BigInteger) t.getTopicID(), this.node.getId());
		BigInteger[] neighbours = this.routingTable.getNeighbours(distToTopic);
		
		if(neighbours.length < KademliaCommonConfig.ALPHA)
			neighbours = this.routingTable.getKClosestNeighbours(KademliaCommonConfig.ALPHA, distToTopic);

		rop.elaborateResponse(neighbours);
		rop.available_requests = KademliaCommonConfig.ALPHA;
	
		m.operationId = rop.operationId;
		m.type = Message.MSG_REGISTER;
		m.src = this.node;
	
		// send ALPHA messages
		for (int i = 0; i < KademliaCommonConfig.ALPHA; i++) {
			BigInteger nextNode = rop.getNeighbour();
			//System.out.println("Nextnode "+nextNode);
			if (nextNode != null) {
				m.dest = new KademliaNode(nextNode);
				sendMessage(m.copy(), nextNode, myPid);
				rop.nrHops++;
			}//nextNode may be null, if the node has less than ALPHA neighbours
		}
	}
	

	/**
	 * Response to a register request.<br>
	 * Tries to register the requesting node under the
	 * specified topic
	 * 
	 * @param m
	 *            Message
	 * @param myPid
	 *            the sender Pid
	 */
	private void handleRegister(Message m, int myPid) {
		Topic t = (Topic) m.body;
		TopicRegistration r = new TopicRegistration(m.src, t);
        Message response; 

		if(this.topicTable.register(r, t)) {
			logger.info(t.topic + " registered on " + this.node.getId() + " by " + m.src.getId());
            response = new Message(Message.MSG_REGISTER_RESPONSE, t);
            response.ackId = m.id;
    		response.operationId = m.operationId;
            response.dest = m.src;
            response.src = this.node;
	    	assert m.src != null;
    		logger.info(" responds with REGISTER_RESPONSE");
            sendMessage(response, m.src.getId(), myPid);
		}

		handleFind(m, myPid, Util.logDistance(t.getTopicID(), this.node.getId()));
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
        Topic t = (Topic) m.body;

        KademliaObserver.reportActiveRegistration(t,this.node.is_evil);
        
        KademliaObserver.addAcceptedRegistration(t, this.node.getId(),m.src.getId(),CommonState.getTime());

    }   

	protected void handleTopicQuery(Message m, int myPid) {
		
		Topic t = (Topic) m.body;
		TopicRegistration[] registrations = this.topicTable.getRegistration(t);
		BigInteger[] neighbours = this.routingTable.getNeighbours(Util.logDistance(t.getTopicID(), this.node.getId()));
	
		//System.out.println("Topic query received at node "+this.node.getId()+" "+registrations.length+" "+neighbours.length);

		Message.TopicLookupBody body = new Message.TopicLookupBody(registrations, neighbours);
		Message response  = new Message(Message.MSG_TOPIC_QUERY_REPLY, body);
		response.operationId = m.operationId;
		response.src = this.node;
		assert m.src != null;
		response.dest = m.src;
		response.ackId = m.id; 
		logger.info(" responds with TOPIC_QUERY_REPLY");
		sendMessage(response, m.src.getId(), myPid);
		
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
		// Parse message content Activate the correct event manager fot the particular event
		super.processEvent(myNode, myPid, event);
		
		
		if(((SimpleEvent) event).getType() == Timeout.TIMEOUT) {
			handleTimeout((Timeout) event, myPid);
			return;
		}
		Message m = (Message) event;
		m.dest = this.node;
		
		if (m.src != null) {
			routingTable.addNeighbour(m.src.getId());
		}


		switch (((SimpleEvent) event).getType()) {
			case Message.MSG_TOPIC_QUERY_REPLY:
				sentMsg.remove(m.ackId);
				handleTopicQueryReply(m, myPid);
				break;
			
			case Message.MSG_REGISTER:
				handleRegister(m, myPid);
				break;
				
			case Message.MSG_INIT_REGISTER:
				handleInitRegister(m, myPid);
				break;			
				
			case Message.MSG_TOPIC_QUERY:
				handleTopicQuery(m, myPid);
				break;
				
			case Message.MSG_INIT_TOPIC_LOOKUP:
				handleInitTopicLookup(m, myPid);
				break;
            case Message.MSG_REGISTER_RESPONSE:
                handleRegisterResponse(m, myPid);
                break;
	
			case Message.MSG_EMPTY:
				// TO DO
				break;
	
			case Message.MSG_STORE:
				// TO DO
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

}