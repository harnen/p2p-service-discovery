package peersim.kademlia;

import java.util.HashMap;
import java.util.ArrayDeque;
import java.util.Iterator;

import peersim.core.CommonState;

public class Discv5TopicTable implements TopicTable {
    
    private int capacity = KademliaCommonConfig.TOPIC_TABLE_CAP;
    private int adsPerQueue = KademliaCommonConfig.ADS_PER_QUEUE;
    private int adLifeTime = KademliaCommonConfig.AD_LIFE_TIME;

    private HashMap<String, ArrayDeque<TopicRegistration>> topicTable;
    private ArrayDeque<TopicRegistration> allAds;
    //private KademliaNode node; 


    public Discv5TopicTable() {
        topicTable = new HashMap<String, ArrayDeque<TopicRegistration>>(); 
        allAds = new ArrayDeque<TopicRegistration>();
    }

    private void updateTopicTable(long curr_time) {
		Iterator<TopicRegistration> it = allAds.iterator();
		while (it.hasNext()) {
    		TopicRegistration r = it.next();
        	if (curr_time - r.getTimestamp() > KademliaCommonConfig.AD_LIFE_TIME) {
            	ArrayDeque<TopicRegistration> topicQ = topicTable.get(r.getTopic().getTopic());
	            TopicRegistration r_same = topicQ.pop();
				it.remove();
        	    //TODO assert that r_same and r are the same registrations
			}
		}
    }

    private long getWaitingTime(TopicRegistration reg, long curr_time) {
        //System.out.println("Get Waiting time "+reg.getTopic().getTopic());

        ArrayDeque<TopicRegistration> topicQ = topicTable.get(reg.getTopic().getTopic());
        long waiting_time;

        if (topicQ != null && topicQ.size() == KademliaCommonConfig.ADS_PER_QUEUE) {
            TopicRegistration r = topicQ.getFirst();
            long age = curr_time - r.getTimestamp();
            waiting_time = KademliaCommonConfig.AD_LIFE_TIME - age;
        }
        else if(allAds.size() == KademliaCommonConfig.TOPIC_TABLE_CAP) {
            TopicRegistration r = allAds.getFirst();
            long age = curr_time - r.getTimestamp();
            waiting_time = KademliaCommonConfig.AD_LIFE_TIME - age;
        }
        else {
            waiting_time = 0;
        }
        
        if ( (topicQ != null) && (topicQ.contains(reg)) ) {
            Iterator<TopicRegistration> it = topicQ.iterator();
            System.out.println("Ad already registered by this node");
            waiting_time = KademliaCommonConfig.AD_LIFE_TIME;
            /*
            while(it.hasNext()) {
                TopicRegistration reg_existing = it.next();
                if (reg.equals(reg_existing)) {
                    long age = curr_time - reg_existing.getTimestamp();
                    waiting_time = KademliaCommonConfig.AD_LIFE_TIME - age;
                }
            }*/
        }

        return waiting_time;
    }
    // FIXME    
    // Ideally this method should take an instance of Ticket as argument 
    // and modify its waiting time in case of reject. I decided to 
    // use the TopicTable interface for now...
    public boolean register(TopicRegistration reg, Topic ti){
        long curr_time = CommonState.getTime();
        updateTopicTable(curr_time);

        long waiting_time = getWaitingTime(reg, curr_time);

        if (waiting_time > 0) {
            reg.setTimestamp(waiting_time); 
            return false;
        }

        reg.setTimestamp(curr_time); 
        ArrayDeque<TopicRegistration> topicQ = this.topicTable.get(reg.getTopic().getTopic());
        if (topicQ != null)
            topicQ.add(reg);
        else {
            ArrayDeque<TopicRegistration> q = new ArrayDeque<TopicRegistration>();
            q.add(reg);
            //System.out.println("Add topictable "+reg.getTopic().getTopic());
            this.topicTable.put(reg.getTopic().getTopic(), q);
        }
        this.allAds.add(reg);
        //TODO assertion: the tail of allAds have a smaller timestamp than reg
        return true;
    }

    public Ticket getTicket(Topic topic, KademliaNode node) {
       // System.out.println("Get ticket "+topic.getTopic());

        TopicRegistration reg = new TopicRegistration(node, topic);

        //update the topic table (remove expired advertisements)
        long curr_time = CommonState.getTime();
        updateTopicTable(curr_time);

        //compute ticket waiting time
        long waiting_time = getWaitingTime(reg, curr_time);
        
        Ticket ticket = new Ticket(topic, curr_time, waiting_time, node);
        return ticket;
    }

    public TopicRegistration[] getRegistration(Topic t){
        ArrayDeque<TopicRegistration> topicQ = topicTable.get(t.getTopic());

        if (topicQ == null)
            return new TopicRegistration[0];

        return (TopicRegistration []) topicQ.toArray(new TopicRegistration[topicQ.size()]);
    }
    //TODO
    public String dumpRegistrations() {
    	return "";
    }
}
