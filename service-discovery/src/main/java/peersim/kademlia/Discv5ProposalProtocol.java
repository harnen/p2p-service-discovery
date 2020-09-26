package peersim.kademlia;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import peersim.core.CommonState;
import peersim.core.Node;
import peersim.core.Network;
import peersim.transport.UnreliableTransport;
import peersim.edsim.EDSimulator;
import peersim.kademlia.Message;
import peersim.kademlia.Timeout;



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
	
	
protected void handleTopicQueryReply(Message m, int myPid) {
		
		LookupOperation lop = (LookupOperation) this.operations.get(m.operationId);
		if (m.src != null) {
			routingTable.addNeighbour(m.src.getId());
			failures.replace(m.src.getId(), 0);
		}
		BigInteger[] neighbours;
		for(int i = 0; i < ((Message.TopicLookupBody) m.body).registrations.length; i++) {
			TopicRegistration registration = ((Message.TopicLookupBody) m.body).registrations[i];
			lop.addDiscovered(registration.getNode().getId());
		}
			
			
		neighbours = ((Message.TopicLookupBody) m.body).neighbours;
				

		if (lop != null) {
			// save received neighbour in the closest Set of fin operation
			try {
				lop.elaborateResponse(neighbours);
				for(BigInteger neighbour: neighbours)
					routingTable.addNeighbour(neighbour);
			} catch (Exception ex) {
				lop.available_requests++;
			}
			if(!lop.finished && Arrays.asList(neighbours).contains(lop.destNode)){
				logger.warning("Found node " + lop.destNode);
				lop.finished = true;
				node.setLookupResult(lop.getNeighboursList());
				KademliaObserver.find_ok.add(1);
				return;
			}

			while ((lop.available_requests > 0)) { // I can send a new find request

				// get an available neighbour
				BigInteger neighbour = lop.getNeighbour();

				if (neighbour != null && lop.discoveredCount() < KademliaCommonConfig.TOPIC_PEER_LIMIT) {
					// send a new request only if we didn't find the node already
					if(!lop.finished){
						Message request = new Message(Message.MSG_REGISTER);
						request.operationId = lop.operationId;
						request.type = Message.MSG_TOPIC_QUERY;
						request.src = this.node;
						request.body = lop.body;

						if(request != null) {
							lop.nrHops++;
							sendMessage(request, neighbour, myPid);
						}
					}

					
				} else if (lop.available_requests == KademliaCommonConfig.ALPHA) { // no new neighbour and no outstanding requests
					// search operation finished
					operations.remove(lop.operationId);
					
					int found = lop.discoveredCount();
					int all = KademliaObserver.topicRegistrationCount(lop.topic.topic);
					logger.warning("Found " + found + " registrations out of " + all + " for topic " + lop.topic.topic);
					if(found != all) { 
						logger.warning("Found only " + found + " registrations out of " + all + " for topic " + lop.topic.topic);
						HashSet<BigInteger> tmp = new HashSet<BigInteger>(KademliaObserver.registeredTopics.get(lop.topic.topic));
						tmp.removeAll(lop.discovered);
						logger.warning("Missing nodes:");
						for(BigInteger id: tmp) {
							logger.warning(id + ", ");
						}
						System.exit(-1);
					}
					KademliaObserver.register_total.add(all);
					KademliaObserver.register_ok.add(found);
									
					node.setLookupResult(lop.getNeighboursList());
					return;
				} else { // no neighbour available but exists oustanding request to wait
					return;
				}
			}
		
		} else {
			System.err.println("Can't find operation " + m.operationId);
			System.exit(-1);
		}
	}
	

	
	private void handleInitTopicLookup(Message m, int myPid) {
		KademliaObserver.lookup_total.add(1);
		
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
			}
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
		
		Node src = nodeIdtoNode(this.node.getId());
		Node dest = nodeIdtoNode(destId);

		logger.info("-> (" + m + "/" + m.id + ") " + destId);

		transport = (UnreliableTransport) (Network.prototype).getProtocol(tid);
		transport.send(src, dest, m, kademliaid);
		KademliaObserver.msg_sent.add(1);

		if ( (m.getType() == Message.MSG_FIND) || (m.getType() == Message.MSG_REGISTER)) { // is a request
			Timeout t = new Timeout(destId, m.id, m.operationId);
			long latency = transport.getLatency(src, dest);

			// add to sent msg
			this.sentMsg.put(m.id, m.timestamp);
			EDSimulator.add(4 * latency, t, src, myPid); // set delay = 2*RTT
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
		
		if(this.node.getId().equals(new BigInteger("41451122193209463269138102959011734520441946334568951533613015137286654629815"))){
			System.err.println(this.node.getId() + " handle init register " + m);
		}

		Topic t = (Topic) m.body;
		TopicRegistration r = new TopicRegistration(this.node, t);
		
		KademliaObserver.addTopicRegistration(t.topic, this.node.getId());
	
	
		RegisterOperation rop = new RegisterOperation(m.timestamp, t, r);
		rop.body = m.body;
		operations.put(rop.operationId, rop);
	
		//BigInteger[] neighbours = this.routingTable.getNeighbours((BigInteger) m.body, this.node.getId());
		BigInteger[] neighbours = this.routingTable.getNeighbours(Util.logDistance((BigInteger) t.getTopicID(), this.node.getId()));
		
		if(neighbours.length<KademliaCommonConfig.K)
			neighbours = this.routingTable.getKClosestNeighbours(KademliaCommonConfig.K);
		
		rop.elaborateResponse(neighbours);
		rop.available_requests = KademliaCommonConfig.ALPHA;
	
		// set message operation id
		m.operationId = rop.operationId;
		m.type = Message.MSG_REGISTER;
		m.src = this.node;
	
		// send ALPHA messages
		for (int i = 0; i < KademliaCommonConfig.ALPHA; i++) {
			BigInteger nextNode = rop.getNeighbour();
			if(this.node.getId().equals(new BigInteger("41451122193209463269138102959011734520441946334568951533613015137286654629815"))){
				System.err.println("Sending to " + nextNode);
			}
			if (nextNode != null) {
				sendMessage(m.copy(), nextNode, myPid);
				rop.nrHops++;
			}else {
				System.err.println("Returned neighbor is NUll !");
				System.exit(-1);
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
		if(this.topicTable.register(r, t)) {
			logger.info(t.topic + " registered on " + this.node.getId() + " by " + m.src.getId());
		}

		handleFind(m, myPid, Util.logDistance(t.getTopicID(), this.node.getId()));
	}

	
	private void handleTopicQuery(Message m, int myPid) {
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
		if(((SimpleEvent) event).getType() == Timeout.TIMEOUT) return;
		Message m = (Message) event;
		m.dest = this.node;
		KademliaObserver.reportMsg(m, false);


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
