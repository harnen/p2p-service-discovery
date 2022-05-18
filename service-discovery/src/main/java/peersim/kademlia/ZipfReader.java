package peersim.kademlia;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Map.Entry;

import org.apache.commons.math3.distribution.ZipfDistribution;  

public class ZipfReader extends Discv5ZipfTrafficGenerator {

	private final int randomLookups;
	private final int maxtopicNum;
	
	private final static String PAR_LOOKUPS = "randomlookups";
	private final static String PAR_MAXTOPIC = "maxtopicnum";
	
	
	public ZipfReader(String prefix) {
        super(prefix);
        randomLookups = Configuration.getInt(prefix + "." + PAR_LOOKUPS, 0);
        maxtopicNum = Configuration.getInt(prefix + "." + PAR_MAXTOPIC);
        
        zipf = new ZipfDistribution(maxtopicNum, exp);
	}
	
	public boolean execute() {
		//execute it only once
		if(first) {
			try {
				
				
				String filename = "./config/zipf/zipf_" + "exp_" + exp + "topics_" + maxtopicNum + "size_" + (Network.size()-calculateEvil()) + ".csv";
				//if the distribution doesn't exist - create it
				if(! new File(filename).exists()) {
					writeOutZipfDist(filename);
				}
				BufferedReader br = new BufferedReader(new FileReader(filename));	
				String line = "";
				int i = 0;
				br.readLine();//read header
				while ((line = br.readLine()) != null){
					//data should be in format: <node_id>, <topic>
					String[] data = line.split(",");  
					int nodeID = Integer.valueOf(data[0]);
					assert nodeID == i: "incorrect NodeID; should be " + i + " was " + nodeID;
					
					//convert to Integer to make sure the format is correct
					int topicNum = Integer.valueOf(data[1]);
					String topicString = new String("t" + topicNum);
				    Topic topic = new Topic(topicString);
				    
					Node start = Network.get(i);
					KademliaProtocol prot = (KademliaProtocol)start.getKademliaProtocol();
					prot.setTargetTopic(topic);
					
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
				    i++;
				}
				br.close();
			}catch (NumberFormatException e) {
				System.err.println("Incorrect conversion from node ID or topic number");
				e.printStackTrace();
			} catch (IOException e) {
				System.err.println("Error reading the input file");
				e.printStackTrace();
			}
			first=false;

		}  
		return false;
		
	}
	
	public void writeOutZipfDist(String filename) {
		HashMap<Integer, Integer> data = new HashMap<Integer, Integer>();
		Integer [] topicsCounts = new Integer[maxtopicNum];
		System.out.println("maxTopicNum: " + maxtopicNum);
		Arrays.fill(topicsCounts,  Integer.valueOf(0));

		for(int i = 0; i < Network.size(); i++) {
			Node start = Network.get(i);
			KademliaProtocol prot = (KademliaProtocol)start.getKademliaProtocol();
            Topic topic = null;
            String topicString="";
   
            // if the node is malicious, it targets only one topic read from config
            if (prot.getNode().is_evil) {
            	if (attackTopicIndex == -1) {
                	topic = prot.getTargetTopic();
                } else {
                	topicString = new String("t" + attackTopicIndex);
                    topic = new Topic(topicString);
                    prot.setTargetTopic(topic);
                }
            } else {
                int topicIndex = zipf.sample() - 1;
                topicsCounts[topicIndex]++;
                topicString = new String("t" + topicIndex);
                topic = new Topic(topicString);
            }
            data.put(i, Integer.valueOf(topic.topic.substring(1)));
		}
		
		try {
			FileWriter writer = new FileWriter("./" + filename);
			writer.write("id,topic\n");
			for(Entry<Integer, Integer> entry: data.entrySet()) {
				writer.write(entry.getKey() +  "," + entry.getValue() + "\n");
			}
			writer.close();
				
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private int calculateEvil() {
		
		int counter=0;
		for(int i = 0; i < Network.size(); i++) {
			Node start = Network.get(i);
			KademliaProtocol prot = (KademliaProtocol)start.getKademliaProtocol();
            Topic topic = null;
            String topicString="";
   
            // if the node is malicious, it targets only one topic read from config
            if (prot.getNode().is_evil) {
            	counter++;
            }
		}
		return Network.size()-counter;
	}
}
