package peersim.kademlia;

/**
 * A Kademlia implementation for PeerSim extending the EDProtocol class.<br>
 * See the Kademlia bibliografy for more information about the protocol.
 *
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.Arrays;
import java.util.logging.Level;
//logging
import java.util.logging.Logger;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.Date;
import java.util.HashMap;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;
import peersim.transport.UnreliableTransport;
import peersim.kademlia.KademliaNode;

//__________________________________________________________________________________________________
public class KademliaProtocol implements Cloneable, EDProtocol {

	// VARIABLE PARAMETERS
	final String PAR_K = "K";
	final String PAR_ALPHA = "ALPHA";
	final String PAR_BITS = "BITS";
	final String PAR_NBUCKETS = "NBUCKETS";
	final String PAR_REFRESHTIME = "REFRESH";

	private static final String PAR_TRANSPORT = "transport";
	protected static String prefix = null;
	protected UnreliableTransport transport;
	protected int tid;
	protected int kademliaid;
	protected HashMap<BigInteger,Integer> failures;
	//private EthClient client;

	/**
	 * allow to call the service initializer only once
	 */
	protected static boolean _ALREADY_INSTALLED = false;

	/**
	 * nodeId of this pastry node
	 */
	//public BigInteger nodeId;

	protected KademliaNode node;

	/**
	 * routing table of this pastry node
	 */
	protected RoutingTable routingTable;

	protected Discv5ProposalTopicTable topicTable;

	protected Logger logger;


	/**
	 * trace message sent for timeout purpose
	 */
	protected TreeMap<Long, Long> sentMsg;

	/**
	 * find operations set
	 */
	protected LinkedHashMap<Long, Operation> operations;

	/**
	 * Replicate this object by returning an identical copy.<br>
	 * It is called by the initializer and do not fill any particular field.
	 * 
	 * @return Object
	 */
	public Object clone() {
		KademliaProtocol dolly = new KademliaProtocol(KademliaProtocol.prefix);
		return dolly;
	}

	/**
	 * Used only by the initializer when creating the prototype. Every other instance call CLONE to create the new object.
	 * 
	 * @param prefix
	 *            String
	 */
	public KademliaProtocol(String prefix) {
		this.node = null; // empty nodeId
		KademliaProtocol.prefix = prefix;
		failures = new HashMap();
		_init();

		this.routingTable = new RoutingTable();

		this.topicTable = new Discv5ProposalTopicTable();

		sentMsg = new TreeMap<Long, Long>();

		operations = new LinkedHashMap<Long, Operation>();

		tid = Configuration.getPid(prefix + "." + PAR_TRANSPORT);
	}

	/**
	 * This procedure is called only once and allow to inizialize the internal state of KademliaProtocol. Every node shares the
	 * same configuration, so it is sufficient to call this routine once.
	 */
	private void _init() {
		// execute once
		if (_ALREADY_INSTALLED)
			return;

		// read paramaters
		KademliaCommonConfig.K = Configuration.getInt(prefix + "." + PAR_K, KademliaCommonConfig.K);
		KademliaCommonConfig.ALPHA = Configuration.getInt(prefix + "." + PAR_ALPHA, KademliaCommonConfig.ALPHA);
		KademliaCommonConfig.BITS = Configuration.getInt(prefix + "." + PAR_BITS, KademliaCommonConfig.BITS);
		KademliaCommonConfig.NBUCKETS = Configuration.getInt(prefix + "." + PAR_NBUCKETS, KademliaCommonConfig.NBUCKETS);
		KademliaCommonConfig.REFRESHTIME = Configuration.getInt(prefix + "." + PAR_REFRESHTIME, KademliaCommonConfig.REFRESHTIME);

		_ALREADY_INSTALLED = true;
	}

	/**
	 * Search through the network the Node having a specific node Id, by performing binary search (we concern about the ordering
	 * of the network).
	 * 
	 * @param searchNodeId
	 *            BigInteger
	 * @return Node
	 */
	public Node nodeIdtoNode(BigInteger searchNodeId) {
		if (searchNodeId == null)
			return null;

		int inf = 0;
		int sup = Network.size() - 1;
		int m;
		
		//System.out.println("nodeIdtoNode "+kademliaid);
		
		while (inf <= sup) {
			m = (inf + sup) / 2;

			BigInteger mId = ((KademliaProtocol) Network.get(m).getProtocol(kademliaid)).node.getId();

			if (mId.equals(searchNodeId))
				return Network.get(m);

			if (mId.compareTo(searchNodeId) < 0)
				inf = m + 1;
			else
				sup = m - 1;
		}

		// perform a traditional search for more reliability (maybe the network is not ordered)
		BigInteger mId;
		for (int i = Network.size() - 1; i >= 0; i--) {
			mId = ((KademliaProtocol) Network.get(i).getProtocol(kademliaid)).node.getId();
			if (mId.equals(searchNodeId))
				return Network.get(i);
		}

		return null;
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
	protected void find(Message m, int myPid) {
		// add message source to my routing table
		Operation fop = (Operation)	 this.operations.get(m.operationId);
		if (m.src != null) {
			routingTable.addNeighbour(m.src.getId());
			failures.replace(m.src.getId(), 0);
		}
		BigInteger[] neighbours;
		if(m.getType() == Message.MSG_TOPIC_QUERY_REPLY) {
			LookupOperation lop = (LookupOperation) fop;
			logger.info("Received " + ((Message.TopicLookupBody) m.body).registrations.length  +  " registrations from" + m.src.getId());
			
			for(int i = 0; i < ((Message.TopicLookupBody) m.body).registrations.length; i++) {
				TopicRegistration registration = ((Message.TopicLookupBody) m.body).registrations[i];
				lop.addDiscovered(registration.getNode().getId());
			}
			
			
			neighbours = ((Message.TopicLookupBody) m.body).neighbours;
		}else {
			neighbours = (BigInteger[]) m.body;
		}
		
		//System.out.println("find node response at "+this.node.getId()+" "+neighbours.length); 

		/*System.out.print("Received neigbours: [");
		for(BigInteger n : neighbours){
			System.out.print(", " + n);
		}
		System.out.println("]");*/
		// get corresponding find operation (using the message field operationId)
		
		

		if (fop != null) {
			// save received neighbour in the closest Set of fin operation
			try {
				fop.elaborateResponse(neighbours);
				for(BigInteger neighbour: neighbours)
					routingTable.addNeighbour(neighbour);
			} catch (Exception ex) {
				fop.available_requests++;
			}
			if(!fop.finished && Arrays.asList(neighbours).contains(fop.destNode)){
				logger.warning("Found node " + fop.destNode);
				fop.finished = true;
				node.setLookupResult(fop.getNeighboursList());
				KademliaObserver.find_ok.add(1);
				return;
			}

			while ((fop.available_requests > 0)) { // I can send a new find request

				// get an available neighbour
				BigInteger neighbour = fop.getNeighbour();

				if (neighbour != null) {
					// send a new request only if we didn't find the node already
					if(!fop.finished){
						Message request = null;
						if(fop.type == Message.MSG_FIND) {
							request = new Message(Message.MSG_FIND);
							request.operationId = m.operationId;
							request.src = this.node;
							request.body = Util.prefixLen(fop.destNode, neighbour);
						}else if(fop.type == Message.MSG_REGISTER) {
							request = new Message(Message.MSG_REGISTER);
							request.operationId = fop.operationId;
							request.type = Message.MSG_REGISTER;
							request.src = this.node;
							request.body = fop.body;
						}else if(fop.type == Message.MSG_TOPIC_QUERY) {
							request = new Message(Message.MSG_REGISTER);
							request.operationId = fop.operationId;
							request.type = Message.MSG_TOPIC_QUERY;
							request.src = this.node;
							request.body = fop.body;
						}

						
						if(request != null) {
							fop.nrHops++;
							sendMessage(request, neighbour, myPid);
						}
					}

					
				} else if (fop.available_requests == KademliaCommonConfig.ALPHA) { // no new neighbour and no outstanding requests
					// search operation finished
					operations.remove(fop.operationId);
					if(!fop.finished && fop.type == Message.MSG_FIND){
						logger.warning("Couldn't find node " + fop.destNode);
					}
					if(fop.type == Message.MSG_TOPIC_QUERY) {
						LookupOperation lop = (LookupOperation) fop;
						int found = lop.discoveredCount();
						int all = KademliaObserver.topicRegistrationCount(lop.topic.topic);
						System.out.println("Node " + this.node.getId() + " found " + found + " registrations out of " + all + " for topic " + lop.topic.topic);
						KademliaObserver.register_total.add(all);
						KademliaObserver.register_ok.add(found);
					}
					//logger.warning("available_requests == KademliaCommonConfig.ALPHA");
					//TODO We use body for other purposes now - need to reconfigure this
					/*if (fop.body.equals("Automatically Generated Traffic") && fop.closestSet.containsKey(fop.destNode)) {
						// update statistics
						long timeInterval = (CommonState.getTime()) - (fop.timestamp);
						KademliaObserver.timeStore.add(timeInterval);
						KademliaObserver.hopStore.add(fop.nrHops);
						KademliaObserver.msg_deliv.add(1);
					}*/
					
					node.setLookupResult(fop.getNeighboursList());
					return;

				} else { // no neighbour available but exists oustanding request to wait
					//logger.warning("else");
					//node.setLookupResult(fop.getNeighboursList());
					return;
				}
			}
		} else {
			System.err.println("Can't with operation " + m.operationId);
			System.exit(-1);
		}
	}

	/**
	 * Response to a route request.<br>
	 * Respond with the peers in your k-bucket closest to the key that is being looked for
	 * 
	 * @param m
	 *            Message
	 * @param myPid
	 *            the sender Pid
	 */
	protected void handleFind(Message m, int myPid, int dist) {
		// get the ALPHA closest node to destNode
		BigInteger[] neighbours = this.routingTable.getNeighbours(dist);
		//System.out.println("find node received at "+this.node.getId()+" distance "+(int) m.body); 

		/*System.out.print("Including neigbours: [");
		for(BigInteger n : neighbours){
			System.out.println(", " + n);
		}
		System.out.println("]");*/


		// create a response message containing the neighbours (with the same id of the request)
		Message response = new Message(Message.MSG_RESPONSE, neighbours);
		response.operationId = m.operationId;
		//response.body = m.body;
		response.src = this.node;
		response.ackId = m.id; // set ACK number

		// send back the neighbours to the source of the message
		sendMessage(response, m.src.getId(), myPid);
	}


	/**
	 * Start a find node opearation.<br>
	 * Find the ALPHA closest node and send find request to them.
	 * 
	 * @param m
	 *            Message received (contains the node to find)
	 * @param myPid
	 *            the sender Pid
	 */
	protected void handleInitFind(Message m, int myPid) {

		KademliaObserver.find_total.add(1);

		//System.out.println("InitFind from "+this.node.getId()+" to "+(BigInteger) m.body+" at "+CommonState.getTime());
		// create find operation and add to operations array
		FindOperation fop = new FindOperation((BigInteger)m.body, m.timestamp);
		fop.destNode = (BigInteger) m.body;
		operations.put(fop.operationId, fop);


		//BigInteger[] neighbours = this.routingTable.getNeighbours((BigInteger) m.body, this.node.getId());	
		BigInteger[] neighbours = this.routingTable.getNeighbours(Util.logDistance((BigInteger) m.body, this.node.getId()));

		fop.elaborateResponse(neighbours);
		fop.available_requests = KademliaCommonConfig.ALPHA;

		// set message operation id
		m.operationId = fop.operationId;
		m.type = Message.MSG_FIND;
		m.src = this.node;

		// send ALPHA messages
		for (int i = 0; i < KademliaCommonConfig.ALPHA; i++) {
			BigInteger nextNode = fop.getNeighbour();
			if (nextNode != null) {
				//m.body = Util.prefixLen(nextNode, fop.destNode);
				m.body = Util.logDistance(nextNode, fop.destNode);
				//System.out.println("Send find message "+Util.logDistance(nextNode, fop.destNode));
				sendMessage(m.copy(), nextNode, myPid);
				fop.nrHops++;
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
			
	
		//	default:
		//		logger.warning("Bad message received "+((SimpleEvent) event).getType());
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
	
	public KademliaNode getNode() {
		return this.node;
	}
	
	/**
	 * Check nodes and replace buckets with valid nodes from replacement list
	 * 
	 */
	public void refreshBuckets(int kademliaid) {
		routingTable.refreshBuckets(kademliaid);
	}
	
	/*public void setClient (EthClient client) {
		this.client = client;
	}*/

}
