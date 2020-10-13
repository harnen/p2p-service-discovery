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

public class Discv5TicketProtocolEvilRegistrant extends Discv5TicketProtocol {

	/**
	 * Replicate this object by returning an identical copy.<br>
	 * It is called by the initializer and do not fill any particular field.
	 * 
	 * @return Object
	 */
	public Object clone() {
		Discv5TicketProtocolEvilRegistrant dolly = new Discv5TicketProtocolEvilRegistrant(Discv5TicketProtocolEvilRegistrant.prefix);
		return dolly;
	}
    
    /**
	 * Used only by the initializer when creating the prototype. Every other instance call CLONE to create the new object.
	 * 
	 * @param prefix
	 *            String
	 */
	public Discv5TicketProtocolEvilRegistrant(String prefix) {
		super(prefix);
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
        
		super.processEvent(myNode, myPid, event);
        /*
        Message m;
	    SimpleEvent s = (SimpleEvent) event;

        switch (((SimpleEvent) event).getType()) {

            case Message.MSG_INIT_REGISTER:
                m = (Message) event;
                handleInitRegisterTopic(m, myPid);
                break;

        }*/
    }
}
