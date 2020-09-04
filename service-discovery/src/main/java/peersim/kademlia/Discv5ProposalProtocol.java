package peersim.kademlia;

import java.math.BigInteger;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import peersim.core.CommonState;
import peersim.core.Node;

public class Discv5ProposalProtocol extends KademliaProtocol {

	public Discv5ProposalTopicTable topicTable;

	public Discv5ProposalProtocol(String prefix) {
		super(prefix);
		this.topicTable = new Discv5ProposalTopicTable();

		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Replicate this object by returning an identical copy.<br>
	 * It is called by the initializer and do not fill any particular field.
	 * 
	 * @return Object
	 */
	public Object clone() {
		Discv5ProposalProtocol dolly = new Discv5ProposalProtocol(Discv5ProposalProtocol.prefix);
		return dolly;
	}
	

	private void handleInitTopicLookup(Message m, int myPid) {
		KademliaObserver.lookup_total.add(1);
		
		Topic t = (Topic) m.body;
	
	
		LookupOperation lop = new LookupOperation(m.timestamp, t);
		lop.body = m.body;
		operations.put(lop.operationId, lop);
	
		//BigInteger[] neighbours = this.routingTable.getNeighbours((BigInteger) m.body, this.node.getId());
		BigInteger[] neighbours = this.routingTable.getNeighbours(Util.logDistance((BigInteger) t.getTopicID(), this.node.getId()));
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
	private void handleInitRegister(Message m, int myPid) {

		KademliaObserver.register_total.add(1);
	
		Topic t = (Topic) m.body;
		TopicRegistration r = new TopicRegistration(this.node, t);
	
	
		RegisterOperation rop = new RegisterOperation(m.timestamp, t, r);
		rop.body = m.body;
		operations.put(rop.operationId, rop);
	
		//BigInteger[] neighbours = this.routingTable.getNeighbours((BigInteger) m.body, this.node.getId());
		BigInteger[] neighbours = this.routingTable.getNeighbours(Util.logDistance((BigInteger) t.getTopicID(), this.node.getId()));
		rop.elaborateResponse(neighbours);
		rop.available_requests = KademliaCommonConfig.ALPHA;
	
		// set message operation id
		m.operationId = rop.operationId;
		m.type = Message.MSG_REGISTER;
		m.src = this.node;
	
		// send ALPHA messages
		for (int i = 0; i < KademliaCommonConfig.ALPHA; i++) {
			BigInteger nextNode = rop.getNeighbour();
			if (nextNode != null) {
				sendMessage(m.copy(), nextNode, myPid);
				rop.nrHops++;
			}
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
		this.topicTable.register(r, t);

		handleFind(m, myPid, Util.prefixLen(this.node.getId(), t.getTopicID()));
	}

	
	private void handleTopicQuery(Message m, int myPid) {
		Topic t = (Topic) m.body;
		TopicRegistration[] registrations = this.topicTable.getRegistration(t);
		BigInteger[] neighbours = this.routingTable.getNeighbours(Util.prefixLen(this.node.getId(), t.getTopicID()));
		
		Message.TopicLookupBody body = new Message.TopicLookupBody(registrations, neighbours);
		Message response  = new Message(Message.MSG_TOPIC_QUERY_REPLY, body);
		response.operationId = m.operationId;
		response.src = this.node;
		response.ackId = m.id; 
		logger.info(this.node + " will respond with TOPIC QUERY REPLY");
		sendMessage(response, m.src.getId(), myPid);
		
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

		Message m;

		switch (((SimpleEvent) event).getType()) {
			case Message.MSG_TOPIC_QUERY_REPLY:
				m = (Message) event;
				sentMsg.remove(m.ackId);
				find(m, myPid);
				break;
			
			case Message.MSG_REGISTER:
				m = (Message) event;
				handleRegister(m, myPid);
				break;
				
			case Message.MSG_TOPIC_QUERY:
				m = (Message) event;
				handleTopicQuery(m, myPid);
				break;
			case Message.MSG_INIT_TOPIC_LOOKUP:
				m = (Message) event;
				handleInitTopicLookup(m, myPid);
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
		this.node = node;
		this.routingTable.nodeId = node.getId();
		this.topicTable.setHostID(node.getId());
		

		logger = Logger.getLogger(node.getId().toString());
		logger.setUseParentHandlers(false);
		ConsoleHandler handler = new ConsoleHandler();
		logger.setLevel(Level.WARNING);
		//logger.setLevel(Level.ALL);
		  
      	handler.setFormatter(new SimpleFormatter() {
        	private static final String format = "[%d][%s] %3$s %n";

        	@Override
     		public synchronized String format(LogRecord lr) {
				return String.format(format,
						CommonState.getTime(),
                    	logger.getName(),
                    	lr.getMessage()
            	);
        	}
      	});
      	logger.addHandler(handler);

		
	}

}
