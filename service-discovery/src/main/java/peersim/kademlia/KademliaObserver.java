package peersim.kademlia;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Arrays;
import java.math.BigInteger;

import com.google.common.collect.HashBasedTable; 
import com.google.common.collect.Table; 

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.kademlia.operations.LookupOperation;
import peersim.kademlia.operations.RegisterOperation;
import peersim.kademlia.operations.LookupTicketOperation;
import peersim.kademlia.operations.Operation;
import peersim.util.IncrementalStats;

/**
 * This class implements a simple observer of search time and hop average in finding a node in the network
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class KademliaObserver implements Control {
    
    private static final String PAR_RANGE_EXPERIMENT = "rangeExperiment";

    private String logFolderName; 
    private String parameterName;
    private double parameterValue; 

    /**
     * keep statistics of the number of hops of every message delivered.
     */
    public static IncrementalStats hopStore = new IncrementalStats();

    /**
     * keep statistics of the time every message delivered.
     */
    public static IncrementalStats timeStore = new IncrementalStats();

    /**
     * keep statistic of number of message delivered
     */
    private static IncrementalStats msg_deliv = new IncrementalStats();

    /**
     * keep statistic of number of message sent
     */
    public static IncrementalStats msg_sent = new IncrementalStats();

    /**
     * keep statistic of number of find operation
     */
    public static IncrementalStats find_total = new IncrementalStats();

    /**
     * Successfull find operations
     */
    public static IncrementalStats find_ok = new IncrementalStats();

    /**
     * keep statistic of number of register operation
     */
    public static IncrementalStats register_total = new IncrementalStats();
    
    /**
     * keep statistic of number of register operation
     */
    public static IncrementalStats lookup_total = new IncrementalStats();

    /**
     * keep statistic of number of register operation
     */
    public static IncrementalStats register_ok = new IncrementalStats();
    
    //public static HashMap<String, Set<BigInteger>> registeredTopics = new HashMap<String, Set<BigInteger>>();
    
    public static HashMap<String, HashMap<BigInteger,RegistrationLog>> registeredTopics = new HashMap<String, HashMap<BigInteger,RegistrationLog>>();
    
    public static TreeMap<String, Integer> activeRegistrations = new TreeMap<String, Integer>();

    public static TreeMap<String, Integer> activeRegistrationsByMalicious = new TreeMap<String, Integer>();
    
    private static HashMap<BigInteger, Integer> nodeMsgReceived = new HashMap<BigInteger, Integer>();
    
    private static HashMap<BigInteger, Integer> nodeTopicStored = new HashMap<BigInteger, Integer>();
    
    private static HashMap<BigInteger, BigInteger> nodeInfo = new HashMap<BigInteger, BigInteger>();
    private static HashSet<BigInteger> writtenNodeIDs = new HashSet<BigInteger>();

    private static FileWriter msgWriter;
    private static FileWriter opWriter; 
    
    private static HashMap<Topic,Integer> regByTopic;
    private static HashMap<BigInteger,Integer> regByRegistrant;
    private static HashMap<BigInteger,Integer> regByRegistrar;

    //Waiting times
    private static HashMap<String, Long> waitingTimes = new HashMap<String, Long>();
    private static HashMap<String, Integer> numOfReportedWaitingTimes = new HashMap<String, Integer>();
    private static HashMap<String, Long> cumWaitingTimes = new HashMap<String, Long>();
    private static HashMap<String, Integer> numOfReportedCumWaitingTimes = new HashMap<String, Integer>();
    private static HashMap<String, Integer> numOfRejectedRegistrations = new HashMap<String, Integer> ();
    String [] all_topics;
    

    private static HashMap<Integer, Integer> msgSent = new HashMap<Integer, Integer>();

    private static int avgCounter=0;

    /** Prefix to be printed in output */
    private String prefix;

    private static KademliaProtocol kadProtocol;

	public KademliaObserver(String prefix) {
		this.prefix = prefix;
        this.parameterName = Configuration.getString(prefix + "." + PAR_RANGE_EXPERIMENT, "");
        if (!this.parameterName.isEmpty())
            this.parameterValue = Configuration.getDouble(prefix + "." + PAR_RANGE_EXPERIMENT, -1);
        
        if (this.parameterName.isEmpty())
            this.logFolderName = "./logs";
        else
            this.logFolderName = this.parameterName + "-" + String.valueOf(this.parameterValue);

        File directory = new File(this.logFolderName);
        if (! directory.exists()){
            directory.mkdir();
        }
        
		try {
			msgWriter = new FileWriter(this.logFolderName + "/" + "messages.csv");
			msgWriter.write("id,type,src,dst,topic,sent/received\n");
			opWriter = new FileWriter(this.logFolderName + "/" + "operations.csv");
			//opWriter.write("id,type,src,dst,hops,malicious,discovered,discovered_list,topic\n");
            opWriter.write("id,type,src,dst,used_hops,returned_hops,malicious,discovered,discovered_list,topic\n");
            regByTopic = new HashMap<Topic,Integer>();
            regByRegistrant = new HashMap<BigInteger,Integer>();
            regByRegistrar = new HashMap<BigInteger,Integer>();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addTopicRegistration(Topic t, BigInteger registrant) {
    	
         String topic = t.getTopic();
         if(!registeredTopics.containsKey(topic)){
                //System.out.println("addTopicRegistration "+topic);

                HashMap<BigInteger,RegistrationLog> set = new HashMap<BigInteger,RegistrationLog>();
                RegistrationLog reg = new RegistrationLog(registrant,CommonState.getTime());
                set.put(registrant,reg);
                registeredTopics.put(topic, set);
         }else{
        	 	HashMap<BigInteger,RegistrationLog> set = registeredTopics.get(topic);
             	RegistrationLog reg = new RegistrationLog(registrant,CommonState.getTime());
             	set.put(registrant, reg);
                registeredTopics.put(topic,set);
                //System.out.println("addTopicRegistration "+topic+" "+registeredTopics.get(topic).size());

         }
    	

        if(!nodeInfo.containsKey(registrant)) 
        {
            nodeInfo.put(registrant, t.getTopicID());
        }
    }
    
    public static void addAcceptedRegistration(Topic t, BigInteger registrant,  BigInteger registrar) {
    	String topic = t.getTopic();
        if(registeredTopics.containsKey(topic)){
        	HashMap<BigInteger,RegistrationLog> set = registeredTopics.get(topic);
        	if(set.containsKey(registrant)) {
        		set.get(registrant).addRegistrar(registrar, CommonState.getTime());
        	}	
        }

   }
    
    public static void addDiscovered(Topic t, BigInteger requesting,  BigInteger discovered) {
    	String topic = t.getTopic();
        if(registeredTopics.containsKey(topic)){
        	HashMap<BigInteger,RegistrationLog> set = registeredTopics.get(topic);
        	if(set.containsKey(discovered)) {
        		set.get(discovered).addDiscovered(requesting, CommonState.getTime());
        		//set.get(discovered).addRegistrar(requesting, CommonState.getTime());
        	}	
        }

   }
    
    
    public static int topicRegistrationCount(String topic) {
        if(registeredTopics.containsKey(topic)){
            return registeredTopics.get(topic).size();
        }
        return 0;
    }
    
    public static void reportOperation(Operation op) {
        try {
            //System.out.println("Report operation "+op.getClass().getSimpleName());
            String result = "";
            String type = "";

            if (op instanceof LookupOperation || op instanceof LookupTicketOperation) { 
                result += op.operationId + "," + op.getClass().getSimpleName() + ","  + op.srcNode +"," + op.destNode + "," + op.getUsedCount() + "," +op.getReturnedCount()+ ","+((LookupOperation) op).maliciousDiscoveredCount()   + "," + ((LookupOperation)op).discoveredCount() +","+ ((LookupOperation)op).discoveredToString() + "," + ((LookupOperation)op).topic.topic+ "\n";
            } else if (op instanceof RegisterOperation) {
                result += op.operationId + "," + op.getClass().getSimpleName() + ","  + op.srcNode +"," + op.destNode + "," + op.getUsedCount() + "," +op.getReturnedCount() + "," + ","  + "," + "," + ((RegisterOperation)op).topic.topic+ "\n";
            } else {
            	;//result += op.operationId + "," + op.getClass().getSimpleName() + ","  + op.srcNode +"," + op.destNode + "," + op.getUsedCount() + "," +op.getReturnedCount() + "," + ","  + "," + "," + "\n";
            }
            opWriter.write(result);
            opWriter.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
 
	
	 /*public static void reportOperation(Operation op) {
	        try {
	            //System.out.println("Report operation "+op.getClass().getSimpleName());
	            String result = "";     
	            String type = "";

	            if (op instanceof LookupOperation || op instanceof LookupTicketOperation) {
	                result += op.operationId + "," + op.getClass().getSimpleName() + ","  + op.srcNode +"," + op.destNode + "," + op.returned.size() + "," +op.used.size()+ ","+((LookupOperation) op).maliciousDiscoveredCount()   + "," + ((LookupOperation)op).discoveredCount() +","+ ((LookupOperation)op).discoveredToString() + "," + ((LookupOperation)op).topic.topic+ "\n";
	            //else
	            //    result += op.operationId + "," + op.getClass().getSimpleName() + ","  + op.srcNode +"," + op.destNode + "," + op.returned.size() + "\n";
	                opWriter.write(result);
	                opWriter.flush();
	            }

	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        
	}*/
	public static void reportActiveRegistration(Topic t, boolean is_evil) {
        if (is_evil) {
            if (!activeRegistrationsByMalicious.containsKey(t.getTopic())) {
                activeRegistrationsByMalicious.put(t.getTopic(), new Integer(1));
            } 
            else {
                Integer num = activeRegistrationsByMalicious.get(t.getTopic());
                num += 1;
                activeRegistrationsByMalicious.put(t.getTopic(), num);
            }
        }
        else {
            if (!activeRegistrations.containsKey(t.getTopic())) {
                activeRegistrations.put(t.getTopic(), new Integer(1));
            } 
            else {
                Integer num = activeRegistrations.get(t.getTopic());
                num += 1;
                activeRegistrations.put(t.getTopic(), num);
            }
        }
    }

    // Report the cumulative waiting time for accepted tickets
    public static void reportCumulativeTime(Topic topic, long time) {
        Long totalCumWaitTime = cumWaitingTimes.get(topic.getTopic());
        if (totalCumWaitTime == null)
            cumWaitingTimes.put(topic.getTopic(), time);
        else
            cumWaitingTimes.put(topic.getTopic(), totalCumWaitTime+time);

        Integer count = numOfReportedCumWaitingTimes.get(topic.getTopic());
        if (count == null)
            numOfReportedCumWaitingTimes.put(topic.getTopic(), 1);
        else
            numOfReportedCumWaitingTimes.put(topic.getTopic(), count+1);
    }

    public static void reportWaitingTime(Topic topic, long time) {
        if (time == -1)
        {
            Integer rejected = numOfRejectedRegistrations.get(topic.getTopic());
            if (rejected == null) 
                numOfRejectedRegistrations.put(topic.getTopic(), 1);
            else 
                numOfRejectedRegistrations.put(topic.getTopic(), rejected + 1);
            return;
        }

        Long totalWaitTime = waitingTimes.get(topic.getTopic());
        Integer count = numOfReportedWaitingTimes.get(topic.getTopic());
        if (count == null) {
            waitingTimes.put(topic.getTopic(), time);
            numOfReportedWaitingTimes.put(topic.getTopic(), 1);
        }
        else {
            waitingTimes.put(topic.getTopic(), time+totalWaitTime);
            numOfReportedWaitingTimes.put(topic.getTopic(), count+1);
        }
    }

    public static void reportExpiredRegistration(Topic t, boolean is_evil) {
        if (is_evil) {
            Integer num = activeRegistrationsByMalicious.get(t.getTopic());
            num -= 1;
            activeRegistrationsByMalicious.put(t.getTopic(), num);
        }
        else {
            Integer num = activeRegistrations.get(t.getTopic());
            num -= 1;
            activeRegistrations.put(t.getTopic(), num);
        }
    }

    private static void accountMsg(Message m) {
        Integer numMsg = msgSent.get(m.getType());
        if (numMsg == null)
            msgSent.put(m.getType(), 1);
        else {
            msgSent.put(m.getType(), numMsg+1);
        }
    }

    private void write_waiting_times() {

        if (waitingTimes.size() == 0)
        {
            return;
        }
        try {
            String filename = this.logFolderName + "/" + "waiting_times.csv";
            File myFile = new File(filename);
            FileWriter writer;
            if (!myFile.exists()) {
                all_topics = (String []) waitingTimes.keySet().toArray(new String[waitingTimes.size()]);
                Arrays.sort(all_topics);
                myFile.createNewFile();
                writer = new FileWriter(myFile, true);
                String title = "time";
                for (String topic: all_topics) {
                    title += "," + topic + "_wait";
                }
                for (String topic: all_topics) {
                    title += "," + topic + "_cumWait";
                }
                for (String topic: all_topics) {
                    title += "," + topic + "_reject";
                }
                title += "\n";
                writer.write(title);
            }
            else {
                writer = new FileWriter(myFile, true);
            }
            writer.write("" + CommonState.getTime());
            for (String topic: all_topics) {
                Long totalWaitTime = waitingTimes.get(topic);
                Integer numOfReported = numOfReportedWaitingTimes.get(topic);
                if (numOfReported == null)
                    writer.write(",0.0");   
                else
                {
                    writer.write("," + totalWaitTime.doubleValue()/numOfReported.intValue());
                }
            }
            for (String topic: all_topics) {
                Long totalWaitTime = cumWaitingTimes.get(topic);
                Integer numOfReported = numOfReportedCumWaitingTimes.get(topic);
                if (numOfReported == null)
                    writer.write(",0.0");   
                else
                {
                    writer.write("," + totalWaitTime.doubleValue()/numOfReported.intValue());
                }
            }
            for (String topic: all_topics) {
                Integer rejected = numOfRejectedRegistrations.get(topic);
                if (rejected == null)
                    writer.write(",0");
                else
                    writer.write("," + rejected);
            }
            writer.write("\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        numOfReportedWaitingTimes.clear();
        waitingTimes.clear();
        numOfRejectedRegistrations.clear();
        cumWaitingTimes.clear();
        numOfReportedCumWaitingTimes.clear();
    }

    private void write_exchanged_msg_stats_over_time() {
        int numMsgTypes = 12;
        try {
            String filename = this.logFolderName + "/" + "msg_stats.csv";
            File myFile = new File(filename);
            FileWriter writer;
            if (!myFile.exists()) {
                myFile.createNewFile();
                writer = new FileWriter(myFile, true);
                String title = "time";
                for (int msgType=0; msgType<numMsgTypes; msgType++) {
                    Message m = new Message(msgType);
                    title += "," + m.messageTypetoString();
                }
                title += "\n";
                writer.write(title);
            }
            else {
                writer = new FileWriter(myFile, true);
            }
            writer.write("" + CommonState.getTime());
            for (int msgType=0; msgType<numMsgTypes; msgType++) {
                Integer count = msgSent.get(msgType);
                if (count == null)
                    count = 0;
                writer.write("," + count);
            }
            writer.write("\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        msgSent.clear(); // = new HashMap<Integer, Integer>();
    }
	
	public static void reportMsg(Message m, boolean sent) {
        ///* 
        accountMsg(m);
		if (kadProtocol instanceof Discv5ProposalProtocol) {
            try {
                String result = "";
                if(m.src == null) return; //ignore init messages
                
                result += m.id + "," + m.messageTypetoString() +"," + m.src.getId() + "," + m.dest.getId() + ",";
                if(m.getType() == Message.MSG_REGISTER ||
                   m.getType() == Message.MSG_TOPIC_QUERY) {
                    result += ((Topic) m.body).topic +"," ;
                }else {
                    result += ",";
                }
                if(sent) {
                    result += "sent\n";
                }
                else {
                    result += "received\n";
                }
                msgWriter.write(result);
                msgWriter.flush();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        
        }else if (kadProtocol instanceof Discv5TicketProtocol) {
            try {
                String result = "";
                if(m.src == null) return; //ignore init messages
                result += m.id + "," + m.messageTypetoString() +"," + m.src.getId() + "," + m.dest.getId() + ",";
                if(m.getType() == Message.MSG_TOPIC_QUERY) {
                    result += ((Topic) m.body).topic +"," ;
                }
                else if(m.getType() == Message.MSG_REGISTER) {
                    result += ((Ticket) m.body).getTopic() + ",";
                }else {
                    result += ",";
                }
                if(sent) {
                    result += "sent\n";
                }
                else {
                    result += "received\n";
                }
                msgWriter.write(result);
                msgWriter.flush();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        } //*/  
    }

    private void write_registration_stats() {
        if (activeRegistrations.size() == 0)
            return;
        try {
            String filename = this.logFolderName + "/" + "registration_stats.csv";
            File myFile = new File(filename);
            FileWriter writer;
            if (!myFile.exists()) {
                myFile.createNewFile();
                writer = new FileWriter(myFile, true);
                String title = "time";
                for (String topic : activeRegistrations.keySet()) {
                    title += "," + topic + "-normal";
                    title += "," + topic + "-evil";
                }
                title += "\n";
                writer.write(title);
            }
            else {
                writer = new FileWriter(myFile, true);
            }
            writer.write("" + CommonState.getTime());
            for (String topic : activeRegistrations.keySet()) {
                writer.write("," + activeRegistrations.get(topic));
                Integer maliciousRegistrants = activeRegistrationsByMalicious.get(topic);
                if (maliciousRegistrants == null) 
                    writer.write(",0");
                else
                {
                    writer.write("," + activeRegistrationsByMalicious.get(topic));
                    //activeRegistrationsByMalicious.put(topic, 0);
                }
                //activeRegistrations.put(topic, 0);
            }
            writer.write("\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void write_node_info() {
        try {
            String filename = this.logFolderName + "/" + "node_information.csv";
            File myFile = new File(filename);
            FileWriter writer;
            if (!myFile.exists()) {
                //if (nodeInfo.size() < Network.size())
                //    return;
                myFile.createNewFile();
                writer = new FileWriter(myFile, true);
                writer.write("nodeID,topicID,is_evil?,numInConnections,numOutConnections,numEvilOutConnections,numEvilInConnections\n");
            }
            else {
                
                writer = new FileWriter(myFile, true);
            }

            for(int i = 0; i < Network.size(); i++) {
                Node node = Network.get(i);
                kadProtocol = node.getKademliaProtocol();
                BigInteger id = kadProtocol.getNode().getId();
                if (writtenNodeIDs.contains(id))
                    continue;
                int is_evil = kadProtocol.getNode().is_evil ? 1 : 0;
                writer.write(id + "," + nodeInfo.get(id) + "," + is_evil + ",");
                writer.write(kadProtocol.getNode().getIncomingConnections().size() + ",");
                writer.write(kadProtocol.getNode().getOutgoingConnections().size() + ",");
                int numEvil = 0;
                for(KademliaNode n : kadProtocol.getNode().getIncomingConnections()) {
                    if (n.is_evil) 
                        numEvil++;
                }
                writer.write(numEvil + ",");
                numEvil = 0;
                for(KademliaNode n : kadProtocol.getNode().getOutgoingConnections()) {
                    if (n.is_evil)
                        numEvil++;
                }
                writer.write(numEvil + "\n");

                writtenNodeIDs.add(id);
            }
            writer.close();
        } catch (IOException e) {

            e.printStackTrace();
        }
    }
                

    private void write_registered_topics_timing() {
    	
    	try {
	        String filename = this.logFolderName + "/" + "registeredTopicsTime.csv";
	        File myFile = new File(filename);
	        FileWriter writer;
	        if (myFile.exists())myFile.delete();
	        myFile.createNewFile();
	        writer = new FileWriter(myFile, true);
	        writer.write("topic,registrant,times_registered,min_registration_time,average_registration_time,min_discovery_time,average_discovery_time\n");
	        for(String t : registeredTopics.keySet()) {
            	//System.out.println("RegisteredTopics "+t+" "+registeredTopics.get(t).size());
            	for(RegistrationLog reg : registeredTopics.get(t).values()) {
            		writer.write(t);
            		writer.write(",");
            		writer.write(String.valueOf(reg.getRegistrant()));
            		writer.write(",");
            		writer.write(String.valueOf(reg.getRegistered().size()));
            		writer.write(",");
            		writer.write(String.valueOf(reg.getMinRegisterTime()));
            		writer.write(",");
            		writer.write(String.valueOf(reg.getAvgRegisterTime()));
            		writer.write(",");
            		writer.write(String.valueOf(reg.getMinDiscoveryTime()));
            		writer.write(",");
            		writer.write(String.valueOf(reg.getAvgDiscoveryTime()));           		
    	        	writer.write("\n");
            	}
            	
	        }
	    	writer.close();

    	}catch(IOException e) {
    		//e.printStackTrace();
    	}
    }
    
    private void write_registered_topics_average() {
    	try {
	        String filename = this.logFolderName + "/" + "registeredTopics.csv";
	        File myFile = new File(filename);
	        FileWriter writer;
	        if (myFile.exists())myFile.delete();
	        myFile.createNewFile();
	        writer = new FileWriter(myFile, true);
	        writer.write("topic,count\n");
	        for(Topic t : regByTopic.keySet()) {
	        	//System.out.println("Topic "+t.getTopic()+" "+regByTopic.get(t)/avgCounter);
	        	writer.write(t.topic);
	        	writer.write(",");
	        	writer.write(String.valueOf(regByTopic.get(t).intValue()/avgCounter));
	        	writer.write("\n");
	        }
	    	writer.close();

    	}catch(IOException e) {
    		//e.printStackTrace();
    	}


    }
    
    private void write_registered_registrar_average() {
    	try {
	        String filename = this.logFolderName + "/" + "registeredRegistrar.csv";
	        File myFile = new File(filename);
	        FileWriter writer;
	        if (myFile.exists())myFile.delete();
	        myFile.createNewFile();
	        writer = new FileWriter(myFile, true);
	        writer.write("nodeId,count\n");

	        for(BigInteger t : regByRegistrar.keySet()) {
	        	//System.out.println("Re "+t.getTopic()+" "+regByRegistrar.get(t)/avgCounter);
	        	writer.write(String.valueOf(t));
	        	writer.write(",");
	        	writer.write(String.valueOf(regByRegistrar.get(t).intValue()/avgCounter));
	        	writer.write("\n");
	        }
	    	writer.close();

    	}catch(IOException e) {
    		e.printStackTrace();
    	}

    }
    
    private void write_registered_registrant_average() {
    	try {
	        String filename = this.logFolderName + "/" + "registeredRegistrant.csv";
	        File myFile = new File(filename);
	        FileWriter writer;
	        if (myFile.exists())myFile.delete();
	        myFile.createNewFile();
	        writer = new FileWriter(myFile, true);
	        writer.write("nodeId,count\n");

	        for(BigInteger t : regByRegistrant.keySet()) {
	        	//System.out.println("Re "+t.getTopic()+" "+regByRegistrar.get(t)/avgCounter);
	        	writer.write(String.valueOf(t));
	        	writer.write(",");
	        	writer.write(String.valueOf(regByRegistrant.get(t).intValue()/avgCounter));
	        	writer.write("\n");
	        }
	    	writer.close();

    	}catch(IOException e) {
    		e.printStackTrace();
    	}

    }


    private void write_eclipsing_results() {

        int num_eclipsed_nodes = 0;
        HashSet<BigInteger> eclipsed_nodes = new HashSet<BigInteger>();
        HashSet<BigInteger> uneclipsed_nodes = new HashSet<BigInteger>();
        HashSet<BigInteger> evil_nodes = new HashSet<BigInteger>();
        try {
            String filename = this.logFolderName + "/" + "eclipse_counts.csv";
            File myFile = new File(filename);
            FileWriter writer;
            if (!myFile.exists()) {
                myFile.createNewFile();
                writer = new FileWriter(myFile, true);
                writer.write("time,numberOfNodes,eclipsedNodes,UnEclipsedNodes,EvilNodes\n");
            }
            else {
                writer = new FileWriter(myFile, true);
            }

            for(int i = 0; i < Network.size(); i++) {
				if( !(Network.get(i).isUp()) ) {
                    continue;   
                }
                Node node = Network.get(i);
                kadProtocol = node.getKademliaProtocol();

                if (kadProtocol.getNode().is_evil) {
                    evil_nodes.add(kadProtocol.getNode().getId());
                }
                else {
                    if (is_eclipsed(kadProtocol.getNode())) {
                        eclipsed_nodes.add(kadProtocol.getNode().getId());
                        num_eclipsed_nodes += 1;
                    }
                    else {
                        uneclipsed_nodes.add(kadProtocol.getNode().getId());
                    }
                }
            }
            writer.write(CommonState.getTime() + "," + String.valueOf(num_eclipsed_nodes));
            writer.write("," + Util.bigIntegetSetToString(eclipsed_nodes));
            writer.write("," + Util.bigIntegetSetToString(uneclipsed_nodes));
            writer.write("," + Util.bigIntegetSetToString(evil_nodes) + "\n");
            writer.close();
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    private boolean is_eclipsed(KademliaNode node) {
        if (node.is_evil)
            //Don't include malicious nodes in the count
            return false;

        if (node.getOutgoingConnections().size() == 0)
            return false;

        for (KademliaNode outConn : node.getOutgoingConnections())
            if (!outConn.is_evil)
                return false;
        /*
        for (KademliaNode inConn : node.getIncomingConnections())
            if (!inConn.is_evil)
                return false;
        */

        return true;
    }

    /**
     * write the snapshot of average storage utilisation in the topic tables
     * for each topic. 
     * 
     */
    private void write_average_storage_utilisation_per_topic() {
        
        HashMap<String, Double> utilisations = new HashMap<String,Double>();
        HashMap<String, Integer> ticketQLength = new HashMap<String,Integer>();
        HashMap<Topic,Integer> topics;
        HashMap<Topic, Integer> waitingTickets;

        int numUpNodes = 0;
        int num_all_registrations = 0;
        for(int i = 0; i < Network.size(); i++) {
            Node node = Network.get(i);
            if (!(node.isUp()))
                continue;
            numUpNodes += 1;
            kadProtocol = node.getKademliaProtocol();
            topics = ((Discv5TicketProtocol) kadProtocol).topicTable.getRegbyTopic();
            //waitingTickets = ((Discv5TicketProtocol) kadProtocol).topicTable.getCompetingTicketsbyTopic();

            int total_occupancy_topic_table = 0;
            for (Topic t: topics.keySet()) {
                total_occupancy_topic_table += topics.get(t);
                num_all_registrations += topics.get(t);
            }

            for (Topic t: topics.keySet()) {
                int count = topics.get(t);
                double util;
                if (total_occupancy_topic_table == KademliaCommonConfig.TOPIC_TABLE_CAP)
                    util = 1.0;
                else
                    util = ((double) count) / KademliaCommonConfig.ADS_PER_QUEUE;
                if (utilisations.get(t.getTopic()) != null) {
                    double total_util_so_far = utilisations.get(t.getTopic());
                    utilisations.put(t.getTopic(), total_util_so_far + util);
                }
                else 
                    utilisations.put(t.getTopic(), util);
            }
            /*
            for (Topic t: topics.keySet()) {
                int count = waitingTickets.get(t);
                if (ticketQLength.get(t.getTopic()) != null) {
                    int total_ticketQLength_so_far = ticketQLength.get(t.getTopic());
                    ticketQLength.put(t.getTopic(), total_ticketQLength_so_far + count);
                }
                else 
                    ticketQLength.put(t.getTopic(), count);
            }*/
        }
        if (utilisations.size() == 0)
            return;

        try {
            String filename = this.logFolderName + "/" + "storage_utilisation.csv";
            File myFile = new File(filename);
            FileWriter writer;
            String[] keys = (String []) utilisations.keySet().toArray(new String[utilisations.size()]);
            Arrays.sort(keys);
            if (!myFile.exists()) {
                myFile.createNewFile();
                writer = new FileWriter(myFile, true);
                String title = "time";
                for (String topic: keys) {
                    title += "," + topic;
                }
                /*
                for (String topic: keys) {
                    title += "," + topic+ "CompetingTickets";
                }
                */
                title += ",overallUtil";
                title += "\n";
                writer.write(title);
            }
            else {
                writer = new FileWriter(myFile, true);
            }
            writer.write("" + CommonState.getTime());
            for (String topic: keys) {
                double util = utilisations.get(topic) / numUpNodes;
                writer.write("," + util);
            }
            writer.write("," + ((double)num_all_registrations) / (numUpNodes*KademliaCommonConfig.TOPIC_TABLE_CAP));
            /*
            for (String topic: keys) {
                //double averageQLength= ((double) ticketQLength.get(topic)) / numUpNodes;
                writer.write("," + (int) ticketQLength.get(topic));
            }
            */
            writer.write("\n");
            writer.close();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
            
    }
    

    /**
     * print the statistical snapshot of the current situation
     * 
     * @return boolean always false
     */
    public boolean execute() {
    	//System.out.println(CommonState.getTime()+" execute");
    	avgCounter++;
        try {
            FileWriter writer = new FileWriter(this.logFolderName + "/" + CommonState.getTime() +  "_registrations.csv");
            writer.write("host,topic,registrant,timestamp\n");

            
            for(int i = 0; i < Network.size(); i++) {
            	
                Node node = Network.get(i);
                if(!node.isUp())continue;
                kadProtocol = node.getKademliaProtocol();
                
                HashMap<Topic,Integer> topics = new HashMap<Topic,Integer>();
                
                if(kadProtocol instanceof Discv5ProposalProtocol) {
                	topics = ((Discv5ProposalProtocol) kadProtocol).topicTable.getRegbyTopic();
                } else if(kadProtocol instanceof Discv5TicketProtocol) {
                	topics = ((Discv5TicketProtocol) kadProtocol).topicTable.getRegbyTopic();
                }
                
                for(Topic t: topics.keySet()) {
            		int count = 0;
            		if(regByTopic.get(t)!=null)count=regByTopic.get(t);
            		count+=topics.get(t);
            		regByTopic.put(t, count);
                }
                
                if(kadProtocol instanceof Discv5ProposalProtocol) {
                    String registrations = ((Discv5ProposalProtocol) kadProtocol).topicTable.dumpRegistrations();
                    writer.write(registrations);
                    
                    //topic table registrations by registrar
                    int count=0;
                    if(regByRegistrar.get(kadProtocol.getNode().getId())!=null) {
                    	count = regByRegistrar.get(kadProtocol.getNode().getId());
                    	count+=((Discv5ProposalProtocol) kadProtocol).topicTable.getRegbyRegistrar();
                    }
                    regByRegistrar.put(kadProtocol.getNode().getId(), count);
                    
                    //topic table registrations by registrant
                    HashMap<BigInteger,Integer> tmpReg = ((Discv5ProposalProtocol) kadProtocol).topicTable.getRegbyRegistrant();
                    for(BigInteger id : tmpReg.keySet())
                    {             
                    	count=0;
                    	if(regByRegistrant.get(id)!=null) {
                    		count = regByRegistrant.get(id);
                    	}
                    	count+=tmpReg.get(id);
                    	regByRegistrant.put(id,count);
                    }
                    
                }
                if(kadProtocol instanceof Discv5TicketProtocol) {
                    String registrations = ((Discv5TicketProtocol) kadProtocol).topicTable.dumpRegistrations();
                    writer.write(registrations);
                    
                    //topic table registrations by registrar
                    int count=0;
                    if(regByRegistrar.get(kadProtocol.getNode().getId())!=null) {
                    	count = regByRegistrar.get(kadProtocol.getNode().getId());
                    	count+=((Discv5TicketProtocol) kadProtocol).topicTable.getRegbyRegistrar();
                    }
                    regByRegistrar.put(kadProtocol.getNode().getId(), count);
                    
                    //topic table registrations by registrant
                    HashMap<BigInteger,Integer> tmpReg = ((Discv5TicketProtocol) kadProtocol).topicTable.getRegbyRegistrant();
                    for(BigInteger id : tmpReg.keySet())
                    {             
                    	count=0;
                    	if(regByRegistrant.get(id)!=null) {
                    		count = regByRegistrant.get(id);
                    	}
                    	count+=tmpReg.get(id);
                    	regByRegistrant.put(id,count);
                    }
                }
                /*for(Topic t : regByTopic.keySet())
                {
                	System.out.println("Topic "+t.getTopic()+" "+regByTopic.get(t)/avgCounter);
                }*/

            }
            writer.close();
        } catch (IOException e) {

            e.printStackTrace();
        }

        write_registered_registrant_average();
        write_registered_topics_average();
        write_registered_registrar_average();
        write_eclipsing_results();
        write_registration_stats();
    	if(CommonState.getTime() > 3000000)
            write_node_info();
        write_registered_topics_timing();
        write_average_storage_utilisation_per_topic();
        write_exchanged_msg_stats_over_time();
        write_waiting_times();
        return false;
    }

}
