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
		System.out.println("Handleregister "+t.getTopic());
	
		TopicRegistration r = new TopicRegistration(m.src, t);
		this.topicTable.register(r, t);
	
		handleFind(m, myPid, Util.prefixLen(this.node.getId(), t.getTopicID()));
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
		System.out.println("handleInitRegister "+t.getTopic());
	
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
		this.kademliaid = myPid;
		if(((SimpleEvent) event).getType() != Timeout.TIMEOUT){
			Message m = (Message) event;
			logger.info("<- " +  m + " " + m.src);
			//don't include controller commands in stats
			if(((SimpleEvent) event).getType() != Message.MSG_INIT_FIND) KademliaObserver.msg_deliv.add(1);
		}

		Message m;

		switch (((SimpleEvent) event).getType()) {
			case Message.MSG_RESPONSE:
				m = (Message) event;
				sentMsg.remove(m.ackId);
				find(m, myPid);
				break;

			case Message.MSG_INIT_FIND:
				m = (Message) event;
				handleInitFind(m, myPid);
				break;

			case Message.MSG_FIND:
				m = (Message) event;
				handleFind(m, myPid, (int) m.body);
				break;
			
			case Message.MSG_INIT_REGISTER:
				m = (Message) event;
				handleInitRegister(m, myPid);
				//System.err.println("Error MSG_INIT_REGISTER");
				//System.exit(0);
				break;
				
			case Message.MSG_EMPTY:
				// TO DO
				break;

			case Message.MSG_STORE:
				// TO DO
				break;
					
			case Message.MSG_REGISTER:
				m = (Message) event;
				handleRegister(m, myPid);
				break;

			case Timeout.TIMEOUT: // timeout
				Timeout t = (Timeout) event;
				if (sentMsg.containsKey(t.msgID)) { // the response msg didn't arrived
					int fails=0;
					if (failures.containsKey(t.node)) { 
			            fails = failures.get(t.node); 
					}
					fails++;
					failures.replace(t.node, fails);
					logger.log(Level.WARNING, " <- timeout" + t.msgID + " from: " + t.node);
					// remove form sentMsg
					sentMsg.remove(t.msgID);
					// remove node from my routing table
					if(fails >= KademliaCommonConfig.MAXFINDNODEFAILURES){
						this.routingTable.removeNeighbour(t.node);
					}
					// remove from closestSet of find operation
					this.operations.get(t.opID).closestSet.remove(t.node);
					// try another node
					Message m1 = new Message();
					m1.operationId = t.opID;
					m1.src = this.node;
					m1.body = new BigInteger[0];
					this.find(m1, myPid);
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
