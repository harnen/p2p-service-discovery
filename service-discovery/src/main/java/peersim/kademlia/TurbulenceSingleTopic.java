package peersim.kademlia;

import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import peersim.config.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.math3.distribution.ZipfDistribution; 


public class TurbulenceSingleTopic extends Turbulence{
	
	protected ZipfDistribution zipf;
	/**
	 * MSPastry Protocol to act
	 */
	private final static String PAR_TOPICNUM = "topicnum";
	private final static String PAR_ZIPF = "zipf";


	private final int topicNum;
	
	protected final double exp;
	
		public TurbulenceSingleTopic(String prefix) {
			super(prefix);
			exp = Configuration.getDouble(prefix + "." + PAR_ZIPF);
			topicNum = Configuration.getInt(prefix + "." + PAR_TOPICNUM,1);
			zipf = new ZipfDistribution(topicNum,exp);
			// TODO Auto-generated constructor stub
		}

		public boolean add() {

			// Add Node
			Node newNode = (Node) Network.prototype.clone();
			for (int j = 0; j < inits.length; ++j)
				inits[j].initialize(newNode);
			Network.add(newNode);
			
			int count=0;
			for (int i = 0; i < Network.size(); ++i) 
				if(Network.get(i).isUp())count++;
			
			System.out.println(CommonState.getTime() +" Adding node " + count);


			// get kademlia protocol of new node
			KademliaProtocol newKad = (KademliaProtocol) (newNode.getProtocol(kademliaid));
			newNode.setKademliaProtocol(newKad);
			// set node Id
			UniformRandomGenerator urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, CommonState.r);
			KademliaNode node = new KademliaNode(urg.generate(), "127.0.0.1", 0);
			((KademliaProtocol) (newNode.getProtocol(kademliaid))).setNode(node);
			newKad.setProtocolID(kademliaid);  
            if (newKad instanceof Discv5EvilTicketProtocol)
            {
                node.is_evil = true;
            }
            else if (newKad instanceof Discv5TicketProtocol)
            {
                node.is_evil = false;
            }

			System.out.println("Turbulence add "+node.getId());

			// sort network
			sortNet();
			
			addRandomConnections(newNode,50);
			addNearNodes(newNode,50);
			
		    String topic = new String("t"+zipf.sample());
			node.setTopic(topic, newNode);

		    Message initLookupMessage = generateFindNodeMessage(new Topic(topic));
			Message registerMessage = generateRegisterMessage(topic);
		    Message lookupMessage = generateTopicLookupMessage(topic);
		    if(registerMessage != null) {
			    //int time = CommonState.r.nextInt(90000);
			    System.out.println("Topic " + topic + " will be registered by "+node.getId());
			    EDSimulator.add(0, initLookupMessage, newNode, kademliaid);
			    EDSimulator.add(10000, registerMessage, newNode, kademliaid);
			    EDSimulator.add(KademliaCommonConfig.AD_LIFE_TIME, lookupMessage, newNode, kademliaid);

		    }
	    
            //System.out.println();

			return false;
		}

}
