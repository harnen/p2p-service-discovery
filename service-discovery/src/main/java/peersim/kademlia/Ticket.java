package peersim.kademlia;

public class Ticket {

    // waiting time assigned when ticket was created
	private long wait_time;

    // absolute time of REGTOPIC request
    private long req_time;

    // cummulative waiting time of this node
    private long cum_wait;

    // registration time (once registration is successful)
    private long reg_time;

    // the topic that ticket is valid for
    private Topic topic; 

    // the node that obtained the ticket
    private KademliaNode src;

    // Success or failure 
    private boolean isRegistrationComplete;

    public Ticket(Topic topic, long req_time, long wait_time) {
        this.topic = topic;
        this.req_time = req_time;
        this.wait_time = wait_time;
        this.cum_wait = wait_time;
        this.isRegistrationComplete = false;
    }

    public Ticket(Topic topic, long req_time, long wait_time, KademliaNode src) {
        this.topic = topic;
        this.req_time = req_time;
        this.wait_time = wait_time;
        this.cum_wait = wait_time;
        this.src = src;
        this.isRegistrationComplete = false;
    }
    
    public Topic getTopic() {
    	return topic;
    }
    
    public void setTopic() {
    	this.topic = topic;
    }
    
    public long getWaitTime() {
    	return wait_time;
    }
    
    public void setWaitTime(long wait_time) {
    	this.wait_time = wait_time;
    }
    
    public long getRegTime() {
    	return wait_time;
    }
    
    public void setRegTime(long reg_time) {
    	this.reg_time = reg_time;
    }
    
    public long getReqTime() {
    	return wait_time;
    }
    
    public void setReqTime(long req_time) {
    	this.req_time = req_time;
    }
    
    public long getCumWaitTime() {
    	return wait_time;
    }
    
    public void setCumWaitTime(long cum_wait) {
    	this.cum_wait = cum_wait;
    }
    
    public void setRegistrationComplete(boolean complete) {
    	this.isRegistrationComplete = complete;
    }
    
    public boolean isRegistrationComplete() {
    	return isRegistrationComplete;
    }
    
}
