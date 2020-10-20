package peersim.kademlia;

/**
 * Discv5 Ticket Evil Protocol implementation.
 *
 */ 
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.TreeMap;

import peersim.kademlia.Topic;

import java.util.Arrays;
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
import peersim.kademlia.Message;
import peersim.kademlia.TicketOperation;
import peersim.kademlia.TicketTable;

public class Discv5EvilTicketProtocol extends Discv5TicketProtocol {

	// VARIABLE PARAMETERS
    final String PAR_ATTACK_TYPE = "attackType";
    final String PAR_NUMBER_OF_REGISTRATIONS = "numberOfRegistrations";
	
    // type of attack (TopicSpam)
    private String attackType;
    // number of registrations to make
    private int numOfRegistrations;
    private int targetNumOfRegistrations;

	/**
	 * Replicate this object by returning an identical copy.<br>
	 * It is called by the initializer and do not fill any particular field.
	 * 
	 * @return Object
	 */
	public Object clone() {
		Discv5EvilTicketProtocol dolly = new Discv5EvilTicketProtocol(Discv5EvilTicketProtocol.prefix);
		return dolly;
	}
    
    /**
	 * Used only by the initializer when creating the prototype. Every other instance call CLONE to create the new object.
	 * 
	 * @param prefix
	 *            String
	 */
	public Discv5EvilTicketProtocol(String prefix) {
		super(prefix);
        this.is_evil = true;
        this.attackType = Configuration.getString(prefix + "." + PAR_ATTACK_TYPE);
        this.numOfRegistrations = 0;
        this.targetNumOfRegistrations = Configuration.getInt(prefix + "." + PAR_NUMBER_OF_REGISTRATIONS, 0);
    }
	
    /**
	 * Start a register topic operation.<br>
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
    	Topic t = (Topic) m.body;
        //t.setHostID(this.node.getId());
		
        if (this.attackType.equals(KademliaCommonConfig.ATTACK_TYPE_TOPIC_SPAM)) {
    	    System.out.println("Spamming registration for topic "+t.getTopic());
    	
            KademliaObserver.addTopicRegistration(t.getTopic(), this.node.getId());

            if(!ticketTable.containsKey(t.getTopicID())) {
            	TicketTable rou = new TicketTable(KademliaCommonConfig.NBUCKETS, this.numOfRegistrations, this.numOfRegistrations,this,t,myPid);
        	    rou.setNodeId(t.getTopicID());
            	ticketTable.put(t.getTopicID(),rou);
            }
        
            sendLookup(t,myPid);
        }
        else {
            logger.warning("Invalid attack type: " + this.attackType);
            System.exit(-1);
        }
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
        Ticket ticket = (Ticket) m.body;
        Topic topic = ticket.getTopic();
        if (ticket.isRegistrationComplete() == true) {
            logger.warning("Registration succesful for topic "+ticket.getTopic().getTopicID()+" at node "+m.src.getId());
            this.numOfRegistrations += 1;
            if (this.numOfRegistrations < this.targetNumOfRegistrations) {
        	    ticketTable.get(ticket.getTopic().topicID).removeNeighbour(m.src.getId());
            }
        }
        else
            super.handleRegisterResponse(m, myPid);
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

        System.out.println("Evil node received an event");
        
		//super.processEvent(myNode, myPid, event);
        Message m;
	    
        SimpleEvent s = (SimpleEvent) event;
        if (s instanceof Message) {
	        m = (Message) event;
            m.dest = this.node;
        }
        
        switch (((SimpleEvent) event).getType()) {
            
                case Message.MSG_INIT_REGISTER:
                    m = (Message) event;
                    handleInitRegisterTopic(m, myPid);

                    break;

                case Message.MSG_REGISTER_RESPONSE:
                    m = (Message) event;
                    handleRegisterResponse(m, myPid);
                    break;


                default :
		            super.processEvent(myNode, myPid, event);
        }
    }
}
