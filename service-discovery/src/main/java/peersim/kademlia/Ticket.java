package peersim.kademlia;

public class Ticket {

    // waiting time assigned when ticket was created
    public long wait_time;

    // absolute time of REGTOPIC request
    public long req_time;

    // cummulative waiting time of this node
    public long cum_wait;

    // registration time (once registration is successful)
    public long reg_time;

    // the topic that ticket is valid for
    public String topic; 

    // the node that obtained the ticket
    public KademliaNode src;

    // Success or failure 
    boolean isRegistrationComplete;

    public Ticket(String topic, long req_time, long wait_time) {
        this.topic = topic;
        this.req_time = req_time;
        this.wait_time = wait_time;
        this.cum_wait = wait_time;
        this.isRegistrationComplete = false;
    }

    public Ticket(String topic, long req_time, long wait_time, KademliaNode src) {
        this.topic = topic;
        this.req_time = req_time;
        this.wait_time = wait_time;
        this.cum_wait = wait_time;
        this.src = src;
        this.isRegistrationComplete = false;
    }
}
