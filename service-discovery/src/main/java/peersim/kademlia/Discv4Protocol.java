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
	

	public Discv4Protocol(String prefix) {
		
		super(prefix);

		activeTopics = new HashSet<String>();


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
}