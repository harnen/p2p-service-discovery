package peersim.kademlia;

/**
 * Discv5 Protocol implementation.
 *
 */ 
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.Arrays;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;
import peersim.transport.UnreliableTransport;
import peersim.kademlia.KademliaNode;

public class Discv5Protocol implements Cloneable, EDProtocol {

	private int kademliaProtocolID;
	private static final String PAR_PROT = "kademliaProtocol";
	private static final String PAR_TRANSPORT = "transport";
	private static String prefix = null;
	
    private UnreliableTransport transport;
	private int tid;
	private int discv5id;
	/**
	 * nodeId of this pastry node
	 */
	//public BigInteger nodeId;


	public KademliaNode kademliaNode;

    /**
     * The node in Network Node[] array
     */
    public Node node;

    /**
     * The node must estimate the topic radius continuously 
     * and use the RegisterOperation to keep track
     */
    public FindOperation fop;

    /* 
     * The topic currently being advertised
     */
    public Topic topic;
	
    /**
	 * routing table of this pastry node
	 */
	public Discv5TopicTable topicTable;

    /**
     * topic radius computer
     */
    public TopicRadius topicRadius;
	
    /**
	 * routing table of this pastry node 
     * this is a pointer to the routing of the KademliaProtocol of the same node
	 */
	public RoutingTable routingTable;
	
	/**
	 * trace message sent for timeout purpose
	 */
	private TreeMap<Long, Long> sentMsg;


	/**
	 * Replicate this object by returning an identical copy.<br>
	 * It is called by the initializer and do not fill any particular field.
	 * 
	 * @return Object
	 */
	public Object clone() {
		Discv5Protocol dolly = new Discv5Protocol(Discv5Protocol.prefix);
		return dolly;
	}
	
    /**
	 * Used only by the initializer when creating the prototype. Every other instance call CLONE to create the new object.
	 * 
	 * @param prefix
	 *            String
	 */
	public Discv5Protocol(String prefix) {
        this.kademliaNode = null;
        Discv5Protocol.prefix = prefix;
		this.kademliaProtocolID = Configuration.getPid(prefix + "." + PAR_PROT);
        this.topicTable = new Discv5TopicTable();
        this.routingTable = null;
        this.fop = null;
		this.tid = Configuration.getPid(prefix + "." + PAR_TRANSPORT);
		this.sentMsg = new TreeMap<Long, Long>();
    }

	/**
	 * Search through the network the Node having a specific node Id, by performing binary search (we concern about the ordering
	 * of the network).
	 * 
	 * @param searchNodeId
	 *            BigInteger
	 * @return Node
	 */
	private Node nodeIdtoNode(BigInteger searchNodeId) {
		if (searchNodeId == null)
			return null;

		int inf = 0;
		int sup = Network.size() - 1;
		int m;
        
        //System.out.println("0th node id: " + ( ((Discv5Protocol) Network.get(inf).getProtocol(discv5id)).kademliaNode.getId()).toString() );
        //System.out.println("Last node id: " + ( ((Discv5Protocol) Network.get(sup).getProtocol(discv5id)).kademliaNode.getId()).toString() );

		while (inf <= sup) {
			m = (inf + sup) / 2;

			BigInteger mId = ((Discv5Protocol) Network.get(m).getProtocol(discv5id)).kademliaNode.getId();

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
			mId = ((Discv5Protocol) Network.get(i).getProtocol(discv5id)).kademliaNode.getId();
			if (mId.equals(searchNodeId))
				return Network.get(i);
		}

		return null;
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
		Node src = nodeIdtoNode(this.kademliaNode.getId());
		Node dest = nodeIdtoNode(destId);
        
        m.src = this.kademliaNode;
        m.dest = new KademliaNode(destId);     

        // TODO: remove the assert later
	    assert(src == this.node);

		//System.out.println(this.kademliaNode.getId() + " (" + m + "/" + m.id + ") -> " + destId);

		transport = (UnreliableTransport) (Network.prototype).getProtocol(tid);
        long network_delay = transport.getLatency(src, dest);
        EDSimulator.add(network_delay+delay, m, dest, myPid); 

		if ( (m.getType() == Message.MSG_REGISTER)  || (m.getType() == Message.MSG_TICKET_REQUEST) ) { // is a request
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
        // FIXME: find a way to do this from Discv5
		//this.routingTable.addNeighbour(destId);

        m.src = this.kademliaNode;
        m.dest = new KademliaNode(destId);     
		Node src = nodeIdtoNode(this.kademliaNode.getId());
		Node dest = nodeIdtoNode(destId);

		//System.out.println(this.kademliaNode.getId() + " (" + m + "/" + m.id + ") -> " + destId);

		transport = (UnreliableTransport) (Network.prototype).getProtocol(tid);
		transport.send(src, dest, m, discv5id);

		if ( (m.getType() == Message.MSG_FIND) || (m.getType() == Message.MSG_REGISTER)  || (m.getType() == Message.MSG_TICKET_REQUEST) ) { // is a request
			Timeout t = new Timeout(destId, m.id, m.operationId);
			long latency = transport.getLatency(src, dest);

			// add to sent msg
			this.sentMsg.put(m.id, m.timestamp);
			EDSimulator.add(4*latency, t, src, myPid); 
		}
	}


    /**
     *
     *
     */
    private void handleRegister(Message m, int myPid) {
		//System.out.println("Register received: " + m);
		Ticket ticket = (Ticket) m.body;
        KademliaNode src = new KademliaNode(m.src); 
        Registration reg = new Registration(src, ticket.topic); 
        boolean ret = topicTable.register(reg, null);
        if (ret == false) {
            ticket.wait_time = reg.getTimestamp();
			ticket.cum_wait += reg.getTimestamp();
            ticket.isRegistrationComplete = false;
        }
        else {
            ticket.wait_time = 0;
            ticket.isRegistrationComplete = true;
            ticket.reg_time = CommonState.getTime();
        }

        Message response = new Message(Message.MSG_REGISTER_RESPONSE, ticket);
		response.ackId = m.id; // set ACK number
        sendMessage(response, m.src.getId(), discv5id);
    }
    /**
     *
     *
     */
    private void handleTicketRequest(Message m, int myPid) {
		//System.out.println("Ticket request received: " + m);
        String topic = (String) m.body;
        // FIXME: there needs to be a better way to get kademliaNode from nodeID
        KademliaNode src = new KademliaNode(m.src); 
        Ticket ticket = topicTable.getTicket(topic, src);
        
        Message response = new Message(Message.MSG_TICKET_RESPONSE, ticket);
		response.ackId = m.id; // set ACK number
		response.operationId = m.operationId;
        sendMessage(response, m.src.getId(), discv5id);
    }

    /**
     * Process a ticket response and schedule a register message
     *
     */
    private void handleTicketResponse(Message m, int myPid) {
        Ticket t = (Ticket) m.body;
        Message register = new Message(Message.MSG_REGISTER, t);
		register.ackId = m.id; // set ACK number
        register.dest = new KademliaNode(m.src);
        register.body = m.body;
        register.operationId = m.operationId;
        scheduleSendMessage(register, m.src.getId(), discv5id, t.wait_time);
    }
    /**
     *
     *
     */
    private void handleRegisterResponse(Message m, int myPid) {
        Ticket ticket = (Ticket) m.body;
        if (ticket.isRegistrationComplete == false) {
        	System.out.println("Unsuccessful Registration of topic: " + ticket.topic + " at node: " + m.src.toString() + " wait time: " + ticket.wait_time);
            Message register = new Message(Message.MSG_REGISTER, ticket);
            register.operationId = m.operationId;
            register.body = m.body;
            scheduleSendMessage(register, m.src.getId(), discv5id, ticket.wait_time);
        }
        else {
            //System.out.println("Successful Registration of topic: " + ticket.topic + " at node: " + m.src.toString());
            long curr_time = CommonState.getTime();
			//System.out.println("Adjusting topic radius with time for destination: " + this.fop.destNode.toString() + " wait time: " + ticket.cum_wait);
            //this.topicRadius.adjustWithTicket(curr_time, this.fop.destNode, ticket.req_time, curr_time);
            this.topicRadius.adjustWithTicket(curr_time, this.fop.destNode, ticket.req_time + ticket.cum_wait, ticket.req_time);
			
            BigInteger targetAddr = topicRadius.nextTarget(false).getAddress();
            // Lookup the target address in the routing table
            BigInteger [] neighbours = this.routingTable.getNeighbours(targetAddr, this.kademliaNode.getId());
            this.fop = new FindOperation(targetAddr, 0);
            this.fop.elaborateResponse(neighbours); 
            BigInteger dest = this.fop.getNeighbour();
            // Schedule a ticket request message to be sent immediately
            Message ticket_request = new Message(Message.MSG_TICKET_REQUEST, topic.getTopic());
            ticket_request.operationId = this.fop.operationId;
            scheduleSendMessage(ticket_request, dest, discv5id, 0); 
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
    private void handleInitRegisterTopic(Message m, int myPid) {
        
        if (this.fop != null) {
            // there is already a registration operation going on at this node
            return;
        }

        String t = new String((String) m.body);
        this.topic = new Topic(this.kademliaNode.getId(), t);
        this.topicRadius = new TopicRadius(topic);

        BigInteger targetAddr = topicRadius.nextTarget(false).getAddress();
        this.routingTable = ((KademliaProtocol) (this.node.getProtocol(this.kademliaProtocolID))).routingTable;
        
        // Lookup the target address in the routing table
        BigInteger [] neighbours = this.routingTable.getNeighbours(targetAddr, this.kademliaNode.getId());
        //System.out.println("Neighbors: " + Arrays.toString(neighbours));
        //System.out.println("My id is: " + this.kademliaNode.getId().toString());
        //System.out.println("Target id is: " + targetAddr.toString());

        this.fop = new FindOperation(targetAddr, 0);
        this.fop.elaborateResponse(neighbours); 
        BigInteger dest = this.fop.getNeighbour();

        if (dest == null) {
        	System.out.println("Neighbors: " + Arrays.toString(neighbours));
            System.out.println("Error: destination is null at time: " + CommonState.getTime());
            return;
        }

        // Schedule a ticket request message to be sent immediately
        Message ticket_request = new Message(Message.MSG_TICKET_REQUEST, topic.getTopic());
		ticket_request.operationId = fop.operationId;
        scheduleSendMessage(ticket_request, dest, discv5id, 0); 
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
        
		this.discv5id = myPid;
	    Message m;
		
        switch (((SimpleEvent) event).getType()) {

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
                //handleTopicQuery(m, myPid);
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

			case Timeout.TIMEOUT: // timeout
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
				break;
        }
    }
	/**
	 * set the current NodeId
	 * 
	 * @param tmp
	 *            BigInteger
	 */
	public void setNode(KademliaNode kademliaNode, Node node) {
		this.node = node;
        this.kademliaNode = kademliaNode;
	}
}
