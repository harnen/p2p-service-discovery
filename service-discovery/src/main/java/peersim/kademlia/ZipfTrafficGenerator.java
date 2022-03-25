package peersim.kademlia;

import org.apache.commons.math3.distribution.ZipfDistribution;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator; 

public class ZipfTrafficGenerator extends Discv5ZipfTrafficGenerator {

	// ______________________________________________________________________________________________
	private final static String PAR_MAXTOPIC = "maxtopicnum";
    private final static String PAR_LOOKUPS = "randomlookups";

	private final int maxtopicNum;
	private final int randomLookups;

	// ______________________________________________________________________________________________
	public ZipfTrafficGenerator(String prefix) {
        super(prefix);
		maxtopicNum = Configuration.getInt(prefix + "." + PAR_MAXTOPIC);
        randomLookups = Configuration.getInt(prefix + "." + PAR_LOOKUPS, 0);

		zipf = new ZipfDistribution(maxtopicNum,exp);
	}

	// ______________________________________________________________________________________________
	/**
	 * every call of this control generates and send a random find node message
	 
	 * @return boolean
	 */
	public boolean execute() {
		//execute it only once
		if(first) {
			for(int i = 0; i<Network.size(); i++) 
			{
				Node start = Network.get(i);
				KademliaProtocol prot = (KademliaProtocol)start.getKademliaProtocol();
                Topic topic = null;
                String topicString="";
                int topicIndex = zipf.sample();
                // if the node is malicious, it targets only one topic read from config
                if (prot.getNode().is_evil) {
                    if (attackTopicIndex == -1) {
                        topic = prot.getTargetTopic();
                    }
                    else {
                        topicString = new String("t" + attackTopicIndex);
                        topic = new Topic(topicString);
                        prot.setTargetTopic(topic);
                    }
                } else {           
                    topicString = new String("t" + topicIndex);
                    topic = new Topic(topicString);
                }
                
                if(randomLookups==1) {
					for(int j = 0;j<3;j++) {
						Node nod = Network.get(i);
						Message lookup = generateFindNodeMessage();
						EDSimulator.add(0, lookup, nod, nod.getKademliaProtocol().getProtocolID());
					}
                	
                }
				
				int time = CommonState.r.nextInt(KademliaCommonConfig.AD_LIFE_TIME);
			    Message registerMessage = generateRegisterMessage(topic.getTopic());
			    Message lookupMessage = generateTopicLookupMessage(topic.getTopic());
				prot.getNode().setTopic(topic.getTopic(), start);

			    if(registerMessage != null) EDSimulator.add(time, registerMessage, start, start.getKademliaProtocol().getProtocolID());
			    //start lookup messages later
			    if(lookupMessage != null)EDSimulator.add(2*KademliaCommonConfig.AD_LIFE_TIME + time, lookupMessage, start, start.getKademliaProtocol().getProtocolID());
                

            }
			first=false;
		}  
		return false;
		
	}
}
