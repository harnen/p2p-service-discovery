package peersim.kademlia;


import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.HashMap;

import peersim.config.Configuration;
import peersim.core.Cleanable;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import peersim.kademlia.operations.FindOperation;
import peersim.kademlia.operations.LookupOperation;
import peersim.kademlia.operations.Operation;
import peersim.kademlia.operations.RegisterOperation;
import peersim.kademlia.operations.TicketOperation;
import peersim.transport.UnreliableTransport;




public class Discv4Protocol extends KademliaProtocol implements Cleanable  {

	//public TopicTable topicTable;

	/**
	 * Table to keep track of topic registrations
	 */
	protected HashSet<String> activeTopics;
	
	private HashMap<Long,Long> registrationMap;


	public Discv4Protocol(String prefix) {
		
		super(prefix);

		activeTopics = new HashSet<String>();
		this.registrationMap = new HashMap<>();


		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Replicate this object by returning an identical copy.<br>
	 * It is called by the initializer and do not fill any particular field.
	 * 
	 * @return Object
	 */
	public Object clone() {
		Discv4Protocol dolly = new Discv4Protocol(Discv4Protocol.prefix);
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


		
		super._init();
	}
	
	
	public List<String> getRegisteringTopics() {


		return new ArrayList<String>(activeTopics);
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
    	//logger.warning("Sending topic registration for topic "+t.getTopic());

		activeTopics.add(t.getTopic());

		
		logger.warning("handleInitRegisterTopic " + t.getTopic() + " " + t.getTopicID());
		
		KademliaObserver.addTopicRegistration(t, this.node.getId());
		
	}
	
	// ______________________________________________________________________________________________
	/**
	 * generates a random find node message, by selecting randomly the destination.
	 * 
	 * @return Message
	 */
	private Message generateFindNodeMessage() {
		// existing active destination node

		UniformRandomGenerator urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, CommonState.r);
		BigInteger rand = urg.generate();
		
		Message m = Message.makeInitFindNode(rand);
		m.timestamp = CommonState.getTime();

		return m;
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
	/*protected void handleResponse(Message m, int myPid) {
		
		// add message source to my routing table

		Operation op = (Operation)	 this.operations.get(m.operationId);
		if (op == null) {
			return;
		}

		
		
		BigInteger[] neighbours = (BigInteger[]) m.body;
		op.elaborateResponse(neighbours);
		for(BigInteger neighbour: neighbours)
			routingTable.addNeighbour(neighbour);
		
		if(!op.finished && Arrays.asList(neighbours).contains(op.destNode)){
			logger.warning("Found node " + op.destNode);
			op.finished = true;
			operations.remove(op.operationId);
			
			KademliaObserver.find_ok.add(1);
			return;
		}
		
		op.increaseReturned(m.src.getId());
		if(!op.finished)op.increaseUsed(m.src.getId());
		

		if(registrationMap.get(op.operationId)!=null) {
			
			HashMap<KademliaNode,BigInteger> results = new HashMap();
			for(BigInteger id : neighbours)
				results.put(Util.nodeIdtoNode(id).getKademliaProtocol().getNode(),m.src.getId());
			
			RegisterOperation rop = (RegisterOperation) operations.get(registrationMap.get(op.operationId));
			node.setLookupResult(results,rop.getTopic().getTopic());
		}
		
		logger.warning("Discv4 handle response "+op.available_requests);


		while ((op.available_requests > 0)) { // I can send a new find request
			BigInteger neighbour = op.getNeighbour();

			if (neighbour != null ) {
				if(!op.finished) {
					// send a new request only if we didn't find the node already
					Message request = null;
					if(op.type == Message.MSG_FIND) {
						request = new Message(Message.MSG_FIND);
						//request.body = Util.prefixLen(op.destNode, neighbour);
						//System.out.println("Request body distance "+Util.prefixLen(op.destNode, neighbour)+" "+Util.logDistance(op.destNode, neighbour));
						request.body = Util.logDistance(op.destNode, neighbour);
					}else if(op.type == Message.MSG_REGISTER) {
						request = new Message(Message.MSG_REGISTER);
						request.body = op.body;
					}else if(op.type == Message.MSG_TICKET_REQUEST) {
						request = new Message(Message.MSG_TICKET_REQUEST);
						request.body = op.body;
					}
							
					if(request != null) {
						op.nrHops++;
						request.operationId = m.operationId;
						request.src = this.node;
						request.dest = Util.nodeIdtoNode(neighbour).getKademliaProtocol().getNode();//new KademliaNode(neighbour);
						sendMessage(request, neighbour, myPid);
					}
				}
							
			} else if (op.available_requests == KademliaCommonConfig.ALPHA) { // no new neighbour and no outstanding requests
				operations.remove(op.operationId);

				logger.warning("Finished lookup node " + op.getUsedCount());

				logger.warning("Registration operation id "+registrationMap.get(op.operationId)+" "+op.operationId);

		
			
				return;

			} else { // no neighbour available but exists outstanding request to wait for
				return;
			}
		}
	}*/
	
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
		
		// add message source to my routing table


		Operation op = (Operation)	 this.operations.get(m.operationId);
		if (op == null) {
			return;
		}
		
		BigInteger[] neighbours = (BigInteger[]) m.body;
		op.elaborateResponse(neighbours);
		for(BigInteger neighbour: neighbours)
			routingTable.addNeighbour(neighbour);
		

		if(registrationMap.get(op.operationId)!=null) {
			LookupOperation lop = (LookupOperation) operations.get(registrationMap.get(op.operationId));
			lop.increaseReturned(m.src.getId());
		}
		
		if(!op.finished && Arrays.asList(neighbours).contains(op.destNode)){
			logger.warning("Found node " + op.destNode);
			op.finished = true;
			/*if(discv4) {
				for(String t: this.node.topicQuerying()) {
					logger.warning("Querying topic "+t);
					((FindOperation)op).setTopic(t);
					KademliaObserver.reportOperation(op);

				}

				node.setLookupResult(op.getNeighboursList());
			}*/
			KademliaObserver.find_ok.add(1);
			
			if(registrationMap.get(op.operationId)!=null) {
				
				LookupOperation lop = (LookupOperation) operations.get(registrationMap.get(op.operationId));

				for (BigInteger id : neighbours) {
					if(Util.nodeIdtoNode(id).getKademliaProtocol().getNode().hasTopic(lop.getTopic().getTopic())){
						lop.addDiscovered(Util.nodeIdtoNode(id).getKademliaProtocol().getNode(),m.src.getId());
						KademliaObserver.addDiscovered(lop.topic, m.src.getId(), id);
					}
				}
				KademliaObserver.reportOperation(lop);

				node.setLookupResult(lop.getDiscovered(),lop.getTopic().getTopic());
				logger.warning("Handle response topic "+lop.getTopic().getTopic());
			}
			
			return;
		}
		
		op.increaseReturned(m.src.getId());
		if(!op.finished)op.increaseUsed(m.src.getId());

		while ((op.available_requests > 0)) { // I can send a new find request
			BigInteger neighbour = op.getNeighbour();

			if (neighbour != null ) {
				if(!op.finished) {
					// send a new request only if we didn't find the node already
					Message request = null;
					if(op.type == Message.MSG_FIND) {
						request = new Message(Message.MSG_FIND);
						//request.body = Util.prefixLen(op.destNode, neighbour);
						//System.out.println("Request body distance "+Util.prefixLen(op.destNode, neighbour)+" "+Util.logDistance(op.destNode, neighbour));
						request.body = Util.logDistance(op.destNode, neighbour);
					}else if(op.type == Message.MSG_REGISTER) {
						request = new Message(Message.MSG_REGISTER);
						request.body = op.body;
					}else if(op.type == Message.MSG_TICKET_REQUEST) {
						request = new Message(Message.MSG_TICKET_REQUEST);
						request.body = op.body;
					}
							
					if(request != null) {
						op.nrHops++;
						request.operationId = m.operationId;
						request.src = this.node;
						request.dest = Util.nodeIdtoNode(neighbour).getKademliaProtocol().getNode();//new KademliaNode(neighbour);
						sendMessage(request, neighbour, myPid);
					}
				}
							
			} else if (op.available_requests == KademliaCommonConfig.ALPHA) { // no new neighbour and no outstanding requests
				operations.remove(op.operationId);
				//op.visualize();
				/*System.out.println("###################Operaration  finished");
				if(!op.finished && op.type == Message.MSG_FIND){
					logger.warning("Couldn't find node " + op.destNode);
				}*/
				logger.info("Finished lookup node " + op.getUsedCount());
				
				if(registrationMap.get(op.operationId)!=null) {
					
					LookupOperation lop = (LookupOperation) operations.get(registrationMap.get(op.operationId));

					for (BigInteger id : neighbours) {
						if(Util.nodeIdtoNode(id).getKademliaProtocol().getNode().hasTopic(lop.getTopic().getTopic())){
							lop.addDiscovered(Util.nodeIdtoNode(id).getKademliaProtocol().getNode(),m.src.getId());
							KademliaObserver.addDiscovered(lop.topic, m.src.getId(), id);
						}

					}
					KademliaObserver.reportOperation(lop);

					node.setLookupResult(lop.getDiscovered(),lop.getTopic().getTopic());
					logger.warning("Handle response topic "+lop.getTopic().getTopic());
				}
				/*if(discv4) {
					for(String t: this.node.topicQuerying()) {
						logger.warning("Querying topic "+t);

						((FindOperation)op).setTopic(t);
						KademliaObserver.reportOperation(op);

					}
					//logger.warning("Topic query "+((FindOperation)op).getTopics().get(0));
					//KademliaObserver.reportOperation(op);

					node.setLookupResult(op.getNeighboursList());
				}*/
				KademliaObserver.reportOperation(op);
				if(!op.finished && op.type == Message.MSG_FIND){
					logger.warning("Couldn't find node " + op.destNode);
				}
				return;

			} else { // no neighbour available but exists outstanding request to wait for
				return;
			}
		}
	}
	
	private void handleInitTopicLookup(Message m, int myPid) {
		

		KademliaObserver.lookup_total.add(1);
		Topic t = (Topic) m.body;
		
		logger.warning("disv4 Send init lookup for topic " + this.node.getId() + " " + t.getTopic());



		LookupOperation lop = new LookupOperation(this.node.getId(), m.timestamp, t);
		lop.body = m.body;
		lop.type = Message.MSG_TOPIC_QUERY;
		operations.put(lop.operationId, lop);
		
		// send message
		Message mFind = generateFindNodeMessage();
		//mFind.type = Message.MSG_INIT_FIND;
		long op = handleInitFind(mFind,myPid);
		

		registrationMap.put(op,lop.operationId);
		
		logger.warning("Lookup operation id "+lop.operationId+" "+op);
		

		
	}
	
	/**
	 * set the current NodeId
	 * 
	 * @param tmp
	 *            BigInteger
	 */
	public void setNode(KademliaNode node) {
		super.setNode(node);
		
	}
	
	public void onKill() {
		// System.out.println("Node removed");
		//topicTable = null;
	}
	
	/*private void handleTimeout(Timeout t, int myPid){
		logger.warning("Handletimeout");
		
	}*/
	
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
	/**
	 * manage the peersim receiving of the events
	 * 
	 * @param myNode Node
	 * @param myPid  int
	 * @param event  Object
	 */
	public void processEvent(Node myNode, int myPid, Object event) {

		//logger.warning("Discv4 process event");
		super.processEvent(myNode, myPid, event);
		Message m;


		SimpleEvent s = (SimpleEvent) event;
		if (s instanceof Message) {
			m = (Message) event;
			m.dest = this.node;
		}

		switch (((SimpleEvent) event).getType()) {

	
		case Message.MSG_INIT_TOPIC_LOOKUP:
			m = (Message) event;
			handleInitTopicLookup(m, myPid);
			break;

		case Message.MSG_INIT_REGISTER:
			m = (Message) event;
			handleInitRegister(m, myPid);
			break;
			
		/*case Timeout.REG_TIMEOUT:

			String topic = ((Timeout) event).topic.getTopic();
			Message message= generateRegisterMessage(topic);
			logger.warning("Timeout "+topic);
		    EDSimulator.add(0, message, Util.nodeIdtoNode(this.node.getId()),myPid);

			break;*/
	
		case Timeout.TIMEOUT: // timeout
			Timeout timeout = (Timeout) event;
			if (sentMsg.containsKey(timeout.msgID)) { // the response msg didn't arrived
				logger.warning("Node " + this.node.getId() + " received a timeout: " + timeout.msgID + " from: "
						+ timeout.node);
				// remove form sentMsg
				sentMsg.remove(timeout.msgID);
			}
			break;
		}
	}
	
}