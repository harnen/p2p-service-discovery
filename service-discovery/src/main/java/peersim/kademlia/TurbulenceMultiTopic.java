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


public class TurbulenceMultiTopic extends Turbulence{
	
	protected ZipfDistribution zipf;
	/**
	 * MSPastry Protocol to act
	 */
	private final static String PAR_MINTOPIC = "mintopicnum";
	private final static String PAR_MAXTOPIC = "maxtopicnum";
	private final static String PAR_ZIPF = "zipf";

	/**
	 * MSPastry Protocol ID to act
	 */
	private final int mintopicNum;
	private final int maxtopicNum;
	
	protected final double exp;
	
		public TurbulenceMultiTopic(String prefix) {
			super(prefix);
			exp = Configuration.getDouble(prefix + "." + PAR_ZIPF);
			mintopicNum = Configuration.getInt(prefix + "." + PAR_MINTOPIC,1);
			maxtopicNum = Configuration.getInt(prefix + "." + PAR_MAXTOPIC);
			zipf = new ZipfDistribution(maxtopicNum,exp);
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
			
			System.out.println("Adding node " + count);

			
			//System.out.println("Adding node " + Network.size());


			// get kademlia protocol of new node
			KademliaProtocol newKad = (KademliaProtocol) (newNode.getProtocol(kademliaid));
			newNode.setKademliaProtocol(newKad);
			// set node Id
			UniformRandomGenerator urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, CommonState.r);
			KademliaNode node = new KademliaNode(urg.generate(), "127.0.0.1", 0);
			((KademliaProtocol) (newNode.getProtocol(kademliaid))).setNode(node);


			// sort network
			sortNet();
			
			/*for (int k = 0; k < 10; k++) {
                int index = CommonState.r.nextInt(Network.size());
                Node n  = Network.get(index);
                if(n.getFailState()==Node.DOWN) {
                	k--;
                	break;
                }
				KademliaProtocol jKad =(KademliaProtocol) n.getProtocol(kademliaid);
				//System.out.println("Adding node "+jKad.getNode().getId());
				newKad.routingTable.addNeighbour(jKad.getNode().getId());
			}*/
			
			// select one random bootstrap node
			Node start;
			do {
				start = Network.get(CommonState.r.nextInt(Network.size()));
			} while ((start == null) || (!start.isUp()));
			newKad.routingTable.addNeighbour(((KademliaProtocol) (start.getProtocol(kademliaid))).node.getId());


            int numTopics;
            
            int num_topics;
            List<String> topicList = new ArrayList<String>(); //references to topics of a node

            numTopics = zipf.sample();
            if (numTopics < mintopicNum)
                numTopics = mintopicNum;
            for (int i=0; i < numTopics; i++) {
            	String t="";
            	do {
            		int num=CommonState.r.nextInt(numTopics)+1;
            		t = new String("t"+num);
            	}while(topicList.contains(t));
            	topicList.add(t);
            }
            //System.out.print("Assigning node to " + numTopics + " topics"+" "+topicList.size()+" ");
            for (int i = 0; i < 1; i++) {
                //System.out.print(topicList.get(i)+" ");
			    Message registerMessage = generateRegisterMessage(topicList.get(i));
			    Message lookupMessage = generateTopicLookupMessage(topicList.get(i));
			    if(registerMessage != null) {
				    int time = CommonState.r.nextInt(90000);
				    //System.out.println("Topic " + topicList.get(i) + " will be registered by "+kademliaid+" "+newNode.getKademliaProtocol().getProtocolID());
				    EDSimulator.add(0+i, registerMessage, newNode, kademliaid);
				    //EDSimulator.add(0, lookupMessage, newNode, kademliaid);

			    }
		    }
            //System.out.println();

			return false;
		}

}