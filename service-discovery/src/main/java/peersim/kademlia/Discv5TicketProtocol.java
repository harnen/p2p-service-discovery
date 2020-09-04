package peersim.kademlia;

/**
 * Discv5 Protocol implementation.
 *
 */ 
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.TreeMap;

import peersim.kademlia.Topic;

import java.util.Arrays;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;
import peersim.transport.UnreliableTransport;
import peersim.kademlia.KademliaNode;

public class Discv5TicketProtocol extends KademliaProtocol {

    public FindOperation fop;

    /* 
     * The topic currently being advertised
     */
    private Topic topic;
	
    /**
	 * routing table of this pastry node
	 */
    private Discv5TopicTable topicTable;

    /**
     * topic radius computer
     */
    private TopicRadius topicRadius;
	

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
		Node src = nodeIdtoNode(this.node.getId());
		Node dest = nodeIdtoNode(destId);
        
        m.src = this.node;
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
		this.routingTable.addNeighbour(destId);

		Node src = nodeIdtoNode(this.node.getId());
		Node dest = nodeIdtoNode(destId);

		logger.info("-> (" + m + "/" + m.id + ") " + destId);

		transport = (UnreliableTransport) (Network.prototype).getProtocol(tid);
		transport.send(src, dest, m, kademliaid);
		KademliaObserver.msg_sent.add(1);

		if ( (m.getType() == Message.MSG_FIND) || (m.getType() == Message.MSG_REGISTER)|| (m.getType() == Message.MSG_TICKET_REQUEST) ) { 
			Timeout t = new Timeout(destId, m.id, m.operationId);
			long latency = transport.getLatency(src, dest);

			// add to sent msg
			this.sentMsg.put(m.id, m.timestamp);
			EDSimulator.add(4 * latency, t, src, myPid); // set delay = 2*RTT
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
        TopicRegistration reg = new TopicRegistration(src, ticket.getTopic()); 
        //System.out.println("Register "+ticket.getTopic().getTopic());
        boolean ret = topicTable.register(reg, null);
        if (ret == false) {
            ticket.setWaitTime(reg.getTimestamp());
            ticket.setCumWaitTime(reg.getTimestamp());
            ticket.setRegistrationComplete(false);
        }
        else {
        	ticket.setWaitTime(0);
        	ticket.setRegistrationComplete(true);
        	ticket.setRegTime(CommonState.getTime());
        }

        Message response = new Message(Message.MSG_REGISTER_RESPONSE, ticket);
		response.ackId = m.id; // set ACK number
        sendMessage(response, m.src.getId(), myPid);
    }
    
    /**
     *
     *
     */
    private void handleTicketRequest(Message m, int myPid) {
		//System.out.println("Ticket request received: " + m);
        Topic t = (Topic) m.body;
        // FIXME: there needs to be a better way to get kademliaNode from nodeID
        KademliaNode src = new KademliaNode(m.src); 
        //System.out.println("TicketRequest handle "+t.getTopic());
        Ticket ticket = topicTable.getTicket(t, src);
        
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
        Message register = new Message(Message.MSG_REGISTER, t);
		register.ackId = m.id; // set ACK number
        register.dest = new KademliaNode(m.src);
        register.body = m.body;
        register.operationId = m.operationId;
        scheduleSendMessage(register, m.src.getId(), myPid, t.getWaitTime());
    }
    /**
     *
     *
     */
    private void handleRegisterResponse(Message m, int myPid) {
        Ticket ticket = (Ticket) m.body;
        if (ticket.isRegistrationComplete() == false) {
        	System.out.println("Unsuccessful Registration of topic: " + ticket.getTopic() + " at node: " + m.src.toString() + " wait time: " + ticket.getWaitTime());
            Message register = new Message(Message.MSG_REGISTER, ticket);
            register.operationId = m.operationId;
            register.body = m.body;
            scheduleSendMessage(register, m.src.getId(), myPid, ticket.getWaitTime());
        }
        else {
            //System.out.println("Successful Registration of topic: " + ticket.topic + " at node: " + m.src.toString());
            long curr_time = CommonState.getTime();
			//System.out.println("Adjusting topic radius with time for destination: " + this.fop.destNode.toString() + " wait time: " + ticket.cum_wait);
            //this.topicRadius.adjustWithTicket(curr_time, this.fop.destNode, ticket.req_time, curr_time);
            this.topicRadius.adjustWithTicket(curr_time, this.fop.destNode, ticket.getReqTime() + ticket.getCumWaitTime(), ticket.getRegTime());
			
            BigInteger targetAddr = topicRadius.nextTarget(false).getAddress();
            // Lookup the target address in the routing table
            //BigInteger [] neighbours = this.routingTable.getNeighbours(targetAddr, this.kademliaNode.getId());
        	BigInteger[] neighbours = this.routingTable.getNeighbours(Util.logDistance(targetAddr, this.node.getId()));

            this.fop = new FindOperation(targetAddr, 0);
            this.fop.elaborateResponse(neighbours); 
            BigInteger dest = this.fop.getNeighbour();
            // Schedule a ticket request message to be sent immediately
            
            if (dest == null) {
            	System.out.println("Neighbors: " + Arrays.toString(neighbours));
                System.out.println("Error: destination is null at time: " + CommonState.getTime());
                return;
            }

            Message ticket_request = new Message(Message.MSG_TICKET_REQUEST, topic);
            ticket_request.operationId = this.fop.operationId;
            scheduleSendMessage(ticket_request, dest, myPid, 0); 
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

        Topic t = (Topic) m.body;
        t.setHostID(this.node.getId());
        this.topic = t;

        this.topicRadius = new TopicRadius(t);

        BigInteger targetAddr = topicRadius.nextTarget(false).getAddress();
        
        // Lookup the target address in the routing table
        //BigInteger [] neighbours = this.routingTable.getNeighbours(targetAddr, this.kademliaNode.getId());
    	BigInteger[] neighbours = this.routingTable.getNeighbours(Util.logDistance(targetAddr, this.node.getId()));

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
        Message ticket_request = new Message(Message.MSG_TICKET_REQUEST, t);
		ticket_request.operationId = fop.operationId;
        scheduleSendMessage(ticket_request, dest, myPid, 0); 
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

}
