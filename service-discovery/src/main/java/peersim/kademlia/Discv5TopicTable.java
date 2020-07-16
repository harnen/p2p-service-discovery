package peersim.kademlia;

import java.util.HashMap;
import java.util.ArrayDeque;
import java.util.Iterator;

import peersim.core.CommonState;

public class Discv5TopicTable implements TopicTable {
    
    private int capacity = KademliaCommonConfig.TOPIC_TABLE_CAP;
    private int adsPerQueue = KademliaCommonConfig.ADS_PER_QUEUE;
    private int adLifeTime = KademliaCommonConfig.AD_LIFE_TIME;

    private HashMap<String, ArrayDeque<Registration>> topicTable;
    private ArrayDeque<Registration> allAds;
    //private KademliaNode node; 


    public Discv5TopicTable() {
        topicTable = new HashMap<String, ArrayDeque<Registration>>(); 
        allAds = new ArrayDeque<Registration>();
    }

    private void updateTopicTable(long curr_time) {
        while(!allAds.isEmpty()) {
            Registration r = allAds.getFirst();
            if (curr_time - r.getTimestamp() > KademliaCommonConfig.AD_LIFE_TIME) {
                r = allAds.pop();
                ArrayDeque<Registration> topicQ = topicTable.get(r.getTopic());
                Registration r_same = topicQ.pop();
                //TODO assert that r_same and r are the same registrations
            }
        }
    }

    private long getWaitingTime(Registration reg, long curr_time) {
        String topic = reg.getTopic();
        ArrayDeque<Registration> topicQ = topicTable.get(topic);
        long waiting_time;

        if (topicQ.size() == KademliaCommonConfig.ADS_PER_QUEUE) {
            Registration r = topicQ.getFirst();
            long age = curr_time - r.getTimestamp();
            waiting_time = KademliaCommonConfig.AD_LIFE_TIME - age;
        }
        else if(allAds.size() == KademliaCommonConfig.TOPIC_TABLE_CAP) {
            Registration r = allAds.getFirst();
            long age = curr_time - r.getTimestamp();
            waiting_time = KademliaCommonConfig.AD_LIFE_TIME - age;
        }
        else {
            waiting_time = 0;
        }
        
        if ( (topicQ != null) && (topicQ.contains(reg)) ) {
            Iterator<Registration> it = topicQ.iterator();
            while(it.hasNext()) {
                Registration reg_existing = it.next();
                if (reg.equals(reg_existing)) {
                    long age = curr_time - reg_existing.getTimestamp();
                    waiting_time = KademliaCommonConfig.AD_LIFE_TIME - age;
                }
            }
        }

        return waiting_time;
    }
    // FIXME    
    // Ideally this method should take an instance of Ticket as argument 
    // and modify its waiting time in case of reject. I decided to 
    // use the TopicTable interface for now...
    public boolean register(Registration reg, Topic ti){
        long curr_time = CommonState.getTime();
        updateTopicTable(curr_time);

        long waiting_time = getWaitingTime(reg, curr_time);
        reg.setTimestamp(curr_time + waiting_time); 

        if (waiting_time > 0) {
            reg.setTimestamp(waiting_time); 
            return false;
        }

        ArrayDeque<Registration> topicQ = this.topicTable.get(reg.getTopic());
        topicQ.add(reg);
        this.allAds.add(reg);
        //TODO assertion: the tail of allAds have a smaller timestamp than reg
        return true;
    }

    public Ticket getTicket(String topic, KademliaNode node) {

        Registration reg = new Registration(node, topic);

        //update the topic table (remove expired advertisements)
        long curr_time = CommonState.getTime();
        updateTopicTable(curr_time);

        //compute ticket waiting time
        long waiting_time = getWaitingTime(reg, curr_time);
        
        Ticket ticket = new Ticket(topic, curr_time, waiting_time, node);
        return ticket;
    }

    public Registration[] getRegistration(Topic t){

        return null;
    }
}
