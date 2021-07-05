package peersim.kademlia;

import java.util.HashMap;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map.Entry;

import peersim.core.CommonState;
import peersim.kademlia.Topic;
import peersim.kademlia.TopicRegistration;

import java.math.BigInteger;

// This topic table ensures that subsequent waiting times returned to users 
// follow a linear reduction trend by maintaining state of previously computed
// modifiers for users.
public class Discv5StatefulTopicTable extends Discv5GlobalTopicTable {

    // Count of each IP, ID, topic in the topic table
    private HashMap<String, Integer> ip_counter;
    private HashMap<BigInteger, Integer> id_counter;
    // Last computed modifier for each IP, ID, and topic
    private HashMap<String, Double> ip_last_modifier;
    private HashMap<BigInteger, Double> id_last_modifier;
    private HashMap<Topic, Double> topic_last_modifier;
    // The time of last modifier computation for each IP, ID, and topic
    private HashMap<String, Long> ip_timestamp;
    private HashMap<BigInteger, Long> id_timestamp;
    private HashMap<Topic, Long> topic_timestamp;

    public Discv5StatefulTopicTable() {
        super();

        // Modifer state initialisation
        ip_counter = new HashMap<String, Integer>();
        id_counter = new HashMap<BigInteger, Integer>();

        ip_last_modifier = new HashMap<String, Double>();
        id_last_modifier = new HashMap<BigInteger, Double>();
        topic_last_modifier = new HashMap<Topic, Double>();

        ip_timestamp = new HashMap<String, Long>();
        id_timestamp = new HashMap<BigInteger, Long>();
        topic_timestamp = new HashMap<Topic, Long>();
    }

    protected long getWaitingTime(TopicRegistration reg, long curr_time, Ticket ticket) {
        long waiting_time=0;
    	double baseWaitingTime;
    	long cumWaitingTime = 0;

    	if(allAds.size()==0) return 0;
    	if(ticket!=null) // if this is the first (initial) ticket request, ticket will be null
            cumWaitingTime=ticket.getCumWaitTime()+ 2*ticket.getRTT();

        ArrayDeque<TopicRegistration> topicQ = topicTable.get(reg.getTopic());

        // check if the advertisement already registered before
        if ( (topicQ != null) && (topicQ.contains(reg)) ) {
            //logger.warning("Ad already registered by this node");
            return -1;
        } else if(allAds.size() == this.tableCapacity) {
            TopicRegistration r = allAds.getFirst();
            long age = curr_time - r.getTimestamp();
            waiting_time = this.adLifeTime - age;
        }
        else {
            baseWaitingTime = getBaseTime();
	        long neededTime  = (long) (Math.max(getTopicModifier(reg)+getIPModifier(reg)+getIdModifier(reg), baseWaitingTime*1/1000000));
	        waiting_time = Math.max(0, neededTime - cumWaitingTime);
        }

        waiting_time = Math.min(waiting_time, adLifeTime);

        return waiting_time;
    }
    
    protected Ticket [] makeRegisterDecision(long curr_time) {   
        Ticket [] tickets = super.makeRegisterDecision(curr_time);
        
         // Remove expired state (in case of discontinued registrations from Turbulence)   
         Iterator<Entry<Topic, Long>> iter_topic = topic_timestamp.entrySet().iterator();
         while(iter_topic.hasNext()) {
            Entry<Topic, Long> entry = iter_topic.next();
            long timestamp = entry.getValue();
            if (curr_time - timestamp > 2*adLifeTime)
            {
                Topic topic = entry.getKey(); 
                topic_last_modifier.remove(topic);
                iter_topic.remove();
            }
         }
         Iterator<Entry<String, Long>> iter_ip = ip_timestamp.entrySet().iterator();
         while(iter_ip.hasNext()) {
            Entry<String, Long> entry = iter_ip.next();
            long timestamp = entry.getValue();
            if (curr_time - timestamp > 2*adLifeTime)
            {
                String ip = entry.getKey(); 
                ip_last_modifier.remove(ip);
                iter_ip.remove();
            }
         }
         Iterator<Entry<BigInteger, Long>> iter_id = id_timestamp.entrySet().iterator();
         while(iter_id.hasNext()) {
            Entry<BigInteger, Long> entry = iter_id.next();
            long timestamp = entry.getValue();
            if (curr_time - timestamp > 2*adLifeTime)
            {
                BigInteger id = entry.getKey(); 
                id_last_modifier.remove(id);
                iter_id.remove();
            }
         }

         return tickets;
    }

    protected void register(TopicRegistration reg) {
        super.register(reg);   
        // update the ip and id counters with the admitted registrations
        String ip = reg.getNode().getAddr();
        Integer ip_count = ip_counter.get(ip);
        if (ip_count == null) {
            ip_counter.put(ip, 1);
        }
        else {
            ip_counter.put(ip, ip_count+1);
        }
        BigInteger id;
        if (reg.getNode().is_evil) {
            id = reg.getNode().getAttackerId();
        }
        else {
            id = reg.getNode().getId();
        }
        Integer id_count = id_counter.get(id);
        if (id_count == null) {
            id_counter.put(id, 1);
        }
        else {
            id_counter.put(id, id_count+1);
        }
    }
    
    protected void updateTopicTable(long curr_time) {
		Iterator<TopicRegistration> it = allAds.iterator();
		while (it.hasNext()) {
    		TopicRegistration r = it.next();
        	if (curr_time - r.getTimestamp() >= this.adLifeTime) {
            	ArrayDeque<TopicRegistration> topicQ = topicTable.get(r.getTopic());
	            //TopicRegistration r_same = topicQ.pop(); 
	            topicQ.pop(); 
                //assert r_same.equals(r);
				it.remove(); //removes from allAds
                
                // update ip, id counts
                String ip = r.getNode().getAddr();
                Integer ip_count = ip_counter.get(ip);
                ip_count = ip_count - 1;
                if (ip_count == 0)
                    ip_counter.remove(ip);
                else {
                    ip_counter.put(ip, ip_count);
                }
                BigInteger id;
                if (r.getNode().is_evil)
                    id = r.getNode().getAttackerId();
                else
                    id = r.getNode().getId();
                Integer id_count = id_counter.get(id);
                id_count -= 1;
                if (id_count == 0) {
                    id_counter.remove(id);
                }
                else {
                    id_counter.put(id, id_count);
                }
			}
		}
    }

    private double getBaseTime() {
        double occupancy = 1.0 - ( ((double) allAds.size()) / this.tableCapacity);

        return ( (long) baseMultiplier * (adLifeTime/Math.pow(occupancy, occupancyPower)) );
    }
   
    protected double getTopicModifier(TopicRegistration reg) {
        double modifier = super.getTopicModifier(reg);
        modifier = modifier * getBaseTime();
        
        ArrayDeque<TopicRegistration> topicQ = topicTable.get(reg.getTopic());
        if ( (topicQ != null) && (topicQ.size() > 0) ) {
            Long last_timestamp = topic_timestamp.get(reg.getTopic());
            double lower_bound = 0;
            if (last_timestamp != null) {
                long delta_time = CommonState.getTime() - last_timestamp;
                lower_bound = Math.max(0, topic_last_modifier.get(reg.getTopic()) - delta_time);
            }
            modifier = Math.max(modifier, lower_bound);
            if (lower_bound < modifier) {
                topic_last_modifier.put(reg.getTopic(), modifier);
                topic_timestamp.put(reg.getTopic(), CommonState.getTime());
            }
        }
        
        return modifier;
    }

    protected double getIPModifier(TopicRegistration reg) {

        if(allAds.size()==0)
            return 0;
    	
        Integer ip_count = ip_counter.get(reg.getNode().getAddr());
        if (ip_count == null) {
            ip_count = 0;
        }

        double modifier = getBaseTime() * Math.pow((double)ip_count/(allAds.size()),amplify*ipModifierExp);
        double lower_bound = 0;
        if (ip_count > 0) {
            Long last_timestamp = ip_timestamp.get(reg.getNode().getAddr());
            if (last_timestamp != null) {
                long delta_time = CommonState.getTime() - last_timestamp;
                lower_bound = Math.max(0, ip_last_modifier.get(reg.getNode().getAddr()) - delta_time);
            }
            modifier = Math.max(modifier, lower_bound);
            if (lower_bound < modifier) {
                ip_last_modifier.put(reg.getNode().getAddr(), modifier);
                ip_timestamp.put(reg.getNode().getAddr(), CommonState.getTime());
            }
        }

        return modifier;
    }

    protected double getIdModifier(TopicRegistration reg) {
		Iterator<TopicRegistration> it = allAds.iterator();
        BigInteger reg_id;

        // Use attackerID for registrant ID if node is evil
        if (reg.getNode().is_evil)
            reg_id = reg.getNode().getAttackerId(); 
        else
            reg_id = reg.getNode().getId();

        Integer id_count = id_counter.get(reg_id);
        if (id_count == null)
            id_count = 0;

        double modifier = getBaseTime() * Math.pow((double) id_count/(allAds.size()),amplify*idModifierExp); 
        double lower_bound = 0;
        if (id_count > 0) {
            Long last_timestamp = id_timestamp.get(reg_id);
            if (last_timestamp != null) {
                long delta_time = CommonState.getTime() - last_timestamp;
                lower_bound = Math.max(0, id_last_modifier.get(reg_id) - delta_time);
            }
            modifier = Math.max(modifier, lower_bound);
            if (lower_bound < modifier) {
                id_last_modifier.put(reg_id, modifier);
                id_timestamp.put(reg_id, CommonState.getTime());
            }
        }

        return modifier;
    }
}
