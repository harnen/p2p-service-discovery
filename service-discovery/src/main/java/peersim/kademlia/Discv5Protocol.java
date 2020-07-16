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

	private UnreliableTransport transport;
	private int tid;
	private int discv5id;
	/**
	 * nodeId of this pastry node
	 */
	//public BigInteger nodeId;

	public KademliaNode node;
	
    /**
	 * routing table of this pastry node
	 */
	public Discv5TopicTable topicTable;
	
    /**
	 * routing table of this pastry node
	 */
	public RoutingTable routingTable;

	/**
	 * trace message sent for timeout purpose
	 */
	private TreeMap<Long, Long> sentMsg;

	private static String prefix = null;

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
        this.node = null;
        Discv5Protocol.prefix = prefix;
        // TODO add a topic table, operations, etc.


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

		while (inf <= sup) {
			m = (inf + sup) / 2;

			BigInteger mId = ((Discv5Protocol) Network.get(m).getProtocol(discv5id)).node.getId();

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
			mId = ((Discv5Protocol) Network.get(i).getProtocol(discv5id)).node.getId();
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
		Node src = nodeIdtoNode(this.node.getId());
		Node dest = nodeIdtoNode(destId);
        
        m.src = this.node.getId();
        m.dest = destId;     

		System.out.println(this.node.getId() + " (" + m + "/" + m.id + ") -> " + destId);

		transport = (UnreliableTransport) (Network.prototype).getProtocol(tid);
        long network_delay = transport.getLatency(src, dest);
        EDSimulator.add(network_delay+delay, m, dest, myPid); 

		if ( (m.getType() == Message.MSG_FIND) || (m.getType() == Message.MSG_REGISTER)  || (m.getType() == Message.MSG_TICKET_REQUEST) ) { // is a request
			Timeout t = new Timeout(destId, m.id, m.operationId);
			long latency = transport.getLatency(src, dest);

			// add to sent msg
			this.sentMsg.put(m.id, m.timestamp);
			EDSimulator.add(latency, t, src, myPid); 
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

        m.src = this.node.getId();
        m.dest = destId;     
		Node src = nodeIdtoNode(this.node.getId());
		Node dest = nodeIdtoNode(destId);

		System.out.println(this.node.getId() + " (" + m + "/" + m.id + ") -> " + destId);

		transport = (UnreliableTransport) (Network.prototype).getProtocol(tid);
		transport.send(src, dest, m, discv5id);

		if ( (m.getType() == Message.MSG_FIND) || (m.getType() == Message.MSG_REGISTER)  || (m.getType() == Message.MSG_TICKET_REQUEST) ) { // is a request
			Timeout t = new Timeout(destId, m.id, m.operationId);
			long latency = transport.getLatency(src, dest);

			// add to sent msg
			this.sentMsg.put(m.id, m.timestamp);
			EDSimulator.add(5*latency, t, src, myPid); 
		}
	}


    /**
     *
     *
     */
    private void handleRegister(Message m, int myPid) {
        Ticket ticket = (Ticket) m.body;
        KademliaNode src = new KademliaNode(m.src, "127.0.0.1", 0); 
        Registration reg = new Registration(src, ticket.topic); 
        boolean ret = topicTable.register(reg, null);
        if (ret == false) {
            ticket.wait_time = reg.getTimestamp();
        }
        else
            ticket = null;

        Message response = new Message(Message.MSG_RESPONSE, ticket);
        sendMessage(response, m.src, discv5id);

    }
    /**
     *
     *
     */
    private void handleTicketRequest(Message m, int myPid) {
        String topic = (String) m.body;
        // FIXME: there needs to be a better way to get kademliaNode from nodeID
        KademliaNode src = new KademliaNode(m.src, "127.0.0.1", 0); 
        Ticket ticket = topicTable.getTicket(topic, src);
        
        Message response = new Message(Message.MSG_TICKET_RESPONSE, ticket);
        sendMessage(response, m.src, discv5id);
    
    }

    /**
     *
     *
     */
    private void handleTicketResponse(Message m, int myPid) {
        Ticket t = (Ticket) m.body;
        Message register = new Message(Message.MSG_REGISTER, t);
        register.dest = m.src;
        register.body = m.body;
        scheduleSendMessage(register, m.src, discv5id, t.wait_time);
    }
    /**
     *
     *
     */
    private void handleRegisterResponse(Message m, int myPid) {
        Ticket t = (Ticket) m.body;
        if (t != null) {
            Message register = new Message(Message.MSG_REGISTER, t);
            register.body = m.body;
            scheduleSendMessage(register, m.src, discv5id, t.wait_time);
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
	    //TODO   
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

            case Message.MSG_REGISTER_RESPONSE:
                m = (Message) event;
                handleRegisterResponse(m, myPid);

            case Message.MSG_TOPIC_QUERY:
                m = (Message) event;
                //handleTopicQuery(m, myPid);

            case Message.MSG_INIT_REGISTER:
                m = (Message) event;
                handleInitRegisterTopic(m, myPid);
            
            case Message.MSG_TICKET_REQUEST:
                m = (Message) event;
                handleTicketRequest(m, myPid);
            
            case Message.MSG_TICKET_RESPONSE:
                m = (Message) event;
				sentMsg.remove(m.ackId);
                handleTicketResponse(m, myPid);

        }
    }
}
