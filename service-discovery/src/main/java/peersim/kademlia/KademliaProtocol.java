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

import com.sun.tools.sjavac.Log;

import java.util.Date;
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
import peersim.kademlia.RoutingTable;
import peersim.kademlia.Timeout;
import peersim.kademlia.operations.FindOperation;
import peersim.kademlia.operations.Operation;

//__________________________________________________________________________________________________
public class KademliaProtocol implements Cloneable, EDProtocol {

	// VARIABLE PARAMETERS
	final String PAR_K = "K";
	final String PAR_ALPHA = "ALPHA";
	final String PAR_BITS = "BITS";
	final String PAR_NBUCKETS = "NBUCKETS";
	final String PAR_REFRESHTIME = "REFRESH";
	final String PAR_REPORT_MSG = "REPORT_MSG";
	
	private static final String PAR_TRANSPORT = "transport";
	protected static String prefix = null;
	protected UnreliableTransport transport;
	protected int tid;
	protected int kademliaid;
	//private EthClient client;

	/**
	 * allow to call the service initializer only once
	 */
	protected static boolean _ALREADY_INSTALLED = false;

	/**
	 * nodeId of this pastry node
	 */
	//public BigInteger nodeId;

	public KademliaNode node;

	/**
	 * routing table of this pastry node
	 */
	protected RoutingTable routingTable;

	protected Logger logger;


	/**
	 * trace message sent for timeout purpose
	 */
	protected TreeMap<Long, Long> sentMsg;

	/**
	 * find operations set
	 */
	protected LinkedHashMap<Long, Operation> operations;

    //Target topic for attackers
    protected Topic targetTopic=null;

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
		_init();

		this.routingTable = new RoutingTable(KademliaCommonConfig.NBUCKETS,KademliaCommonConfig.K,KademliaCommonConfig.MAXREPLACEMENT);


		sentMsg = new TreeMap<Long, Long>();

		operations = new LinkedHashMap<Long, Operation>();

		tid = Configuration.getPid(prefix + "." + PAR_TRANSPORT);
	}

	/**
	 * This procedure is called only once and allow to inizialize the internal state of KademliaProtocol. Every node shares the
	 * same configuration, so it is sufficient to call this routine once.
	 */
	protected void _init() {
		// execute once
		if (_ALREADY_INSTALLED)
			return;

		// read paramaters
		KademliaCommonConfig.K = Configuration.getInt(prefix + "." + PAR_K, KademliaCommonConfig.K);
		KademliaCommonConfig.ALPHA = Configuration.getInt(prefix + "." + PAR_ALPHA, KademliaCommonConfig.ALPHA);
		KademliaCommonConfig.BITS = Configuration.getInt(prefix + "." + PAR_BITS, KademliaCommonConfig.BITS);
		KademliaCommonConfig.NBUCKETS = Configuration.getInt(prefix + "." + PAR_NBUCKETS, KademliaCommonConfig.NBUCKETS);
		KademliaCommonConfig.REFRESHTIME = Configuration.getInt(prefix + "." + PAR_REFRESHTIME, KademliaCommonConfig.REFRESHTIME);
		KademliaCommonConfig.REPORT_MSG_ACTIVATED = Configuration.getInt(prefix +"."+ PAR_REPORT_MSG,0);

		_ALREADY_INSTALLED = true;
	}
    /** 
    * Getter for target topic when a protocol is malicious 
    */
    protected Topic getTargetTopic() {
        return this.targetTopic;
    }

    /** 
    * Setter for target topic when a protocol is malicious 
    * 
    * @param t 
    *           Topic that is target for attack by this protocol instance
    */
    protected void setTargetTopic(Topic t) {
        this.targetTopic = t;
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
			//node.setLookupResult(op.getNeighboursList());
			KademliaObserver.find_ok.add(1);
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
						request.dest = new KademliaNode(neighbour);
						sendMessage(request, neighbour, myPid);
					}
				}
							
			} else if (op.available_requests == KademliaCommonConfig.ALPHA) { // no new neighbour and no outstanding requests
				operations.remove(op.operationId);
				//op.visualize();
		//		System.out.println("###################Operaration  finished");
				KademliaObserver.reportOperation(op);
				if(!op.finished && op.type == Message.MSG_FIND){
					//logger.warning("Couldn't find node " + op.destNode);
				}
					
				//node.setLookupResult(op.getNeighboursList());
				return;

			} else { // no neighbour available but exists outstanding request to wait for
				return;
			}
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
		response.dest = m.src; 
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

		logger.warning("InitFind from "+this.node.getId()+" to "+(BigInteger) m.body+" at "+CommonState.getTime());
		// create find operation and add to operations array
		FindOperation fop = new FindOperation(this.node.getId(), (BigInteger)m.body, m.timestamp);
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
				m.dest = new KademliaNode(nextNode);
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
	    int destpid;
	    /*System.out.println("M:" + m);
		System.out.println("m.src:" + m.src);
		System.out.println("m.dest:" + m.dest);*/
	    assert m.src != null;
	    assert m.dest != null;
	    

		Node src = Util.nodeIdtoNode(this.node.getId());
		Node dest = Util.nodeIdtoNode(destId);
		
        destpid = dest.getKademliaProtocol().getProtocolID();

		logger.info("-> (" + m + "/" + m.id + ") " + destId);

		transport = (UnreliableTransport) (Network.prototype).getProtocol(tid);
		transport.send(src, dest, m, destpid);
		KademliaObserver.msg_sent.add(1);

		if ((m.getType() == Message.MSG_FIND) || (m.getType() == Message.MSG_REGISTER)) { // is a request
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
		if(((SimpleEvent) event).getType() != Timeout.TIMEOUT && ((SimpleEvent) event).getType() != Timeout.TICKET_TIMEOUT
				&& ((SimpleEvent) event).getType() != Timeout.REG_TIMEOUT){
			Message m = (Message) event;
			if(m.src != null)
				logger.info("<- " +  m + " " + m.src.getId());
			else
				logger.info("<- " +  m);
			//don't include controller commands in stats
			if(m.getType() != Message.MSG_INIT_FIND && 
				m.getType() != Message.MSG_INIT_TOPIC_LOOKUP &&
				m.getType() != Message.MSG_INIT_REGISTER) {
					//if(KademliaCommonConfig.REPORT_MSG_ACTIVATED==1)
                    KademliaObserver.reportMsg(m, false);
					//;//KademliaObserver.registerMsgReceived(this.node.getId(), m);
			}
		}

		Message m;

		switch (((SimpleEvent) event).getType()) {
			case Message.MSG_RESPONSE:
				m = (Message) event;
				sentMsg.remove(m.ackId);
				handleResponse(m, myPid);
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
        this.routingTable.setNodeId(node.getId());

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
	 * Set the protocol ID for this node.
	 */
    public void setProtocolID(int protocolID) {
        this.kademliaid = protocolID;
    }
    
    /**
	 * Get the protocol ID for this node.
	 */
    public int getProtocolID() {
        return this.kademliaid;
    }
	
	/**
	 * Check nodes and replace buckets with valid nodes from replacement list
	 * 
	 */
	public void refreshBuckets() {
		routingTable.refreshBuckets();
	}
	
	/*public void setClient (EthClient client) {
		this.client = client;
	}*/

}
