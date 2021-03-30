package peersim.kademlia;

import java.util.Map;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.Collections;
import java.math.BigInteger;

import peersim.kademlia.Topic;
import peersim.kademlia.TopicRegistration;
import peersim.core.CommonState;

// Round-robin Discv5 Topic table
public class Discv5RRTopicTable extends Discv5TopicTable { // implements TopicTable 

    private SortedMap<Topic, Long> registrationTimes; 
    // index to the current topic in the round-robin
    boolean roundRobin;
    Long endOfRoundTime; //time of next round start
    Long startOfRoundTime; // time of current round start
    int nextTopicIndex; //current topic index in the round-robin order
   
	public Discv5RRTopicTable() {
        super();
        this.registrationTimes = new TreeMap<Topic, Long>();
        this.roundRobin = false;
        this.endOfRoundTime =  Long.valueOf(-1);
        this.startOfRoundTime =  Long.valueOf(-1);
        this.nextTopicIndex = -1;
        //this.adsPerQueue = this.tableCapacity;
    }
    
    // Returns the current time when xth space is available in the topic table.
    // If slot_number is one, then this returns the time that first space opens up in topic table
    private long getTimeOfNextKAvailableSlot(int slot_number, long curr_time) {
        int remainingCapacity = this.tableCapacity - this.allAds.size();

        if (remainingCapacity >= slot_number) 
            return curr_time;

        if (slot_number > this.tableCapacity) {
            // This can happen if numberOfTopics > tableCapacity, but otherwise shouldn't
            logger.severe("Requested slot is greater than topic table capacity");
            slot_number = this.tableCapacity;
        }

		Iterator<TopicRegistration> it = allAds.iterator();
        int count = remainingCapacity;
        long waiting_time = 0;
        for(TopicRegistration reg : allAds) {
            waiting_time = this.adLifeTime - (curr_time - reg.getTimestamp());
            count += 1;
            if (count == slot_number)
                break;
        }

        return curr_time + waiting_time;
    }
    
    protected long getRRWaitingTime(Topic topicToRegister, long curr_time) {
        assert this.roundRobin : "get method is only to be called in round-robin mode";

        Long reg_time = this.registrationTimes.get(topicToRegister);
        if (reg_time != null && reg_time > curr_time) {
            return reg_time - curr_time;
        }
     
        assert this.endOfRoundTime >= curr_time : "end of round time must be later than now";   
        return this.endOfRoundTime - curr_time;
    }

    protected long getWaitingTime(TopicRegistration reg, long curr_time) {
        Topic topicToRegister = reg.getTopic();
        if (this.roundRobin) {
            // in round robin mode
            System.out.println("Entered round-robin!");
            Long reg_time = this.registrationTimes.get(topicToRegister);
            if (reg_time != null && reg_time > curr_time) {
                return reg_time - curr_time; // - KademliaCommonConfig.ONE_UNIT_OF_TIME;
            }
            else {
                return this.endOfRoundTime - curr_time; // - KademliaCommonConfig.ONE_UNIT_OF_TIME;
            }
        }
        else { // not in round-robin mode (table is not full)
            return super.getWaitingTime(reg, curr_time);
        }
    }

    private int getNumberOfCompetingTickets() {
        int numOfTickets = 0;
        for (Map.Entry<Topic,ArrayList<Ticket>> entry : this.competingTickets.entrySet()) {
            ArrayList<Ticket> ticket_list = entry.getValue();
            numOfTickets += ticket_list.size();
        }
        return numOfTickets;

    }

    private int getNumberOfCompetingTopics() {

        int numOfTopics = 0;
        for (Map.Entry<Topic,ArrayList<Ticket>> entry : this.competingTickets.entrySet()) {
            ArrayList<Ticket> ticket_list = entry.getValue();
            if (ticket_list.size() > 0 )
                numOfTopics += 1; 
        }

        return numOfTopics;
    }
    
    protected Ticket [] makeRegisterDecision(long curr_time) {  
        // update topic table (remove expired ads)
        updateTopicTable(curr_time);
        int remainingCapacity = this.tableCapacity - this.allAds.size();
        int numOfCompetingTopics = getNumberOfCompetingTopics();
        System.out.println("Number of competing topics: " + numOfCompetingTopics);
        System.out.println("Remaining capacity: " + remainingCapacity);
        boolean sched_done = false;
        if ( this.roundRobin && (numOfCompetingTopics > remainingCapacity) && (this.nextTopicIndex >= this.registrationTimes.keySet().size() ) ) {
            sched_done = true;
            scheduleRoundRobin(curr_time);
        }
        else if ( !this.roundRobin && (numOfCompetingTopics > remainingCapacity) ) {
            this.roundRobin = true;
            sched_done = true;
            scheduleRoundRobin(curr_time);
        }
        if ( this.roundRobin )
            return makeRegisterDecisionRoundRobin(curr_time, sched_done);
        else
            return super.makeRegisterDecision(curr_time);
    }

    // Make registration decision in round-robin order 
    // NOTE: adsPerQueue limit is not used when determining registration
    private Ticket [] makeRegisterDecisionRoundRobin(long curr_time, boolean sched_done) {
        Topic current_topic = null;
        int remainingCapacity = this.tableCapacity - this.allAds.size();
        ArrayList<Ticket> all_tickets = new ArrayList<Ticket>();
        int topic_index = this.nextTopicIndex;
        boolean endOfRound = false;

        while (remainingCapacity > 0 || sched_done) {

            if ( remainingCapacity > 0 && endOfRound ) {
                // End of round reached but there is still space in the table
                scheduleRoundRobin(curr_time);
                this.nextTopicIndex = 0;
                topic_index = 0;
                int numOfCompetingTopics = getNumberOfCompetingTopics();
                if (numOfCompetingTopics <= remainingCapacity)
                    this.roundRobin = false;
                else 
                    sched_done = true;
                    // Fix the waiting time of tickets that are rejected in the 
                    // previous round but waiting time is set to 0 (because 
                    // endOdRoundTime coincided with last topic of round)
                    for (Ticket t : all_tickets) {
                        if ( !t.isRegistered() && ( t.getWaitTime() == 0 ) ) {
                            long waiting_time = getRRWaitingTime(t.getTopic(), curr_time);
                            t.setWaitTime(waiting_time);
                        }
                    }

                endOfRound = false;
            }

            // Determine which topic's turn in the round-robin order 
            // The ordering of the keys in the TreeMap determines the order of topics.
            int indx = 0;
            for (Topic topic : this.registrationTimes.keySet()) {
                if ( indx == topic_index ) {
                    current_topic = topic;
                    break;
                }
                indx++;    
            }
            assert current_topic != null : "current topic should not be null";
            
            ArrayList<Ticket> ticket_list = new ArrayList<Ticket> ();
            ticket_list.addAll(this.competingTickets.get(current_topic));
            all_tickets.addAll(ticket_list);
            ArrayList<Ticket> competingTicketsOfTopic = this.competingTickets.get(current_topic);
            //this.competingTickets.remove(current_topic);
            
            // Sort tickets by cumulative waiting time (oldest ticket first) 
            Collections.sort(ticket_list);

            // admit first (oldest) ticket, only if there is available capacity
            boolean first = false;
            if (remainingCapacity > 0)
                first = true;

            for(Ticket ticket: ticket_list) {
            // Admit the first (oldest) ticket in the queue, the rest is assigned a non-zero waiting time. 
                if (first &&  (this.allAds.size() < tableCapacity) ) { //accepted ticket
                    TopicRegistration reg = new TopicRegistration(ticket.getSrc(), ticket.getTopic(), curr_time);
                    reg.setTimestamp(curr_time);
                    first = false;
                    KademliaObserver.reportCumulativeTime(ticket.getTopic(), ticket.getCumWaitTime());
                    ticket.setRegistrationComplete(true);
                    ticket.setWaitTime(0);
                    register(reg);
                    this.nextTopicIndex += 1;
                    competingTicketsOfTopic.remove(ticket);
                }
                else {
                    assert !first : "first must be false"; 
                    ticket.setRegistrationComplete(false);
                    long waiting_time = getRRWaitingTime(ticket.getTopic(), curr_time); 
                    if (waiting_time != 0)
                        competingTicketsOfTopic.remove(ticket);
                    //this.endOfRoundTime - curr_time;
                    assert waiting_time >= 0 : "waiting time must be positive or zero";
                    //waiting_time -= KademliaCommonConfig.ONE_UNIT_OF_TIME;
                    waiting_time = (waiting_time - ticket.getRTT() > 0) ? waiting_time - ticket.getRTT() : 0;
                    ticket.setWaitTime(waiting_time);
                    KademliaObserver.reportWaitingTime(ticket.getTopic(), waiting_time);
                }
            }
            remainingCapacity--; 
            topic_index += 1;
            if (topic_index == this.registrationTimes.keySet().size() && sched_done)
                sched_done = false;
            if (this.nextTopicIndex == this.registrationTimes.keySet().size())
                endOfRound = true; 
        }
        for (Ticket t : all_tickets) {
            Topic topic = t.getTopic();
            this.competingTickets.remove(topic);
        }

        Ticket [] tickets = (Ticket []) all_tickets.toArray(new Ticket[all_tickets.size()]);
        return tickets;
    }

    // Computes a registration time for each topic with non-empty competing tickets.
    // The round-robin registration for each topic is done according to the 
    // scheduled registration time.
    private void scheduleRoundRobin(long curr_time) {
        // NOTE: the schedule does not consider adsPerQueue limit

        this.registrationTimes.clear();

        // Identify the topics with competing tickets and add them to Sorted Map
        for (Map.Entry<Topic, ArrayList<Ticket>> entry : this.competingTickets.entrySet()) {
            Topic topic = entry.getKey();
            ArrayList<Ticket> ticket_list = entry.getValue();
            if ( ( ticket_list != null ) && ( ticket_list.size() > 0 ) ) {
                this.registrationTimes.put(topic, null);
            }
        }

        // Identify the waiting times for each topic in round-robin order
        int slot = 1;
        this.startOfRoundTime = getTimeOfNextKAvailableSlot(slot, curr_time);
        for (Map.Entry<Topic, Long> entry : this.registrationTimes.entrySet()) {
            long slot_time = getTimeOfNextKAvailableSlot(slot, curr_time);
            entry.setValue(slot_time);
            slot+=1;
            System.out.println("For topic: " + entry.getKey().getTopic() + " registration to take place at: " + entry.getValue());
        }

        this.endOfRoundTime = getTimeOfNextKAvailableSlot(slot, curr_time);
        System.out.println("End of round time: " + this.endOfRoundTime);
        this.nextTopicIndex = 0;
    }
}


