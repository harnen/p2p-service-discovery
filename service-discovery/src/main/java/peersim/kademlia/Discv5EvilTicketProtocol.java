package peersim.kademlia;

/**
 * Discv5 Ticket Evil Protocol implementation.
 *
 */ 
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.TreeMap;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;
import peersim.transport.UnreliableTransport;
import peersim.kademlia.KademliaCommonConfig;
import peersim.kademlia.KademliaNode;
import peersim.kademlia.Message;
import peersim.kademlia.operations.TicketOperation;
import peersim.kademlia.TicketTable;

public class Discv5EvilTicketProtocol extends Discv5TicketProtocol {

    // VARIABLE PARAMETERS
    final String PAR_ATTACK_TYPE = "attackType";
    final String PAR_NUMBER_OF_REGISTRATIONS = "numberOfRegistrations";
    
    // type of attack (TopicSpam)
    private String attackType;
    // number of registrations to make
    private int numOfRegistrations;
    private int targetNumOfRegistrations;

    /**
     * Replicate this object by returning an identical copy.<br>
     * It is called by the initializer and do not fill any particular field.
     * 
     * @return Object
     */
    public Object clone() {
        Discv5EvilTicketProtocol dolly = new Discv5EvilTicketProtocol(Discv5EvilTicketProtocol.prefix);
        return dolly;
    }
    
    /**
     * Used only by the initializer when creating the prototype. Every other instance call CLONE to create the new object.
     * 
     * @param prefix
     *            String
     */
    public Discv5EvilTicketProtocol(String prefix) {
        super(prefix);
        this.attackType = Configuration.getString(prefix + "." + PAR_ATTACK_TYPE);
        this.numOfRegistrations = 0;
        this.targetNumOfRegistrations = Configuration.getInt(prefix + "." + PAR_NUMBER_OF_REGISTRATIONS, 0);
    }
    
    /**
     * Start a register topic operation.<br>
     * If this is an on-going register operation with a previously obtained 
     * ticket, then send a REGTOPIC message; otherwise,
     * Find the ALPHA closest node and send REGTOPIC message to them
     * 
     * @param m
     *            Message received (contains the node to find)
     * @param myPid
     *            the sender Pid
     */
    protected void handleInitRegisterTopic(Message m, int myPid) {
        Topic t = (Topic) m.body;
        //t.setHostID(this.node.getId());
        
        if (this.attackType.equals(KademliaCommonConfig.ATTACK_TYPE_TOPIC_SPAM) || this.attackType.equals(KademliaCommonConfig.ATTACK_TYPE_HYBRID) ) {
            System.out.println("Spamming registration for topic "+t.getTopic());
            
            int numberOfOutstandingRegistrations = 0;
            if(!ticketTable.containsKey(t.getTopicID())) {
                TicketTable rou = new TicketTable(KademliaCommonConfig.NBUCKETS, this.numOfRegistrations/KademliaCommonConfig.NBUCKETS, 10, this,t,myPid);
                rou.setNodeId(t.getTopicID());
                ticketTable.put(t.getTopicID(),rou);
            
                for(int i = 0; i<= KademliaCommonConfig.BITS;i++) {
                    BigInteger[] neighbours = routingTable.getNeighbours(i);
                    //if(neighbours.length!=0)logger.warning("Bucket at distance "+i+" with "+neighbours.length+" nodes");
                    //else logger.warning("Bucket at distance "+i+" empty");
                    numberOfOutstandingRegistrations += 1;
                    rou.addNeighbour(neighbours);
                }
            }
            if (this.numOfRegistrations != numberOfOutstandingRegistrations) {
                System.out.println("Failed to send " + this.numOfRegistrations + " registrations - only sent " + numberOfOutstandingRegistrations);
            }
            //sendLookup(t,myPid);
        }
        else {
            super.handleInitRegisterTopic(m, myPid);
        }
    }
    
    /**
     * Process a register response message.<br>
     * The body should contain a ticket, which indicates whether registration is 
     * complete. In case it is not, schedule sending a new register request
     * 
     * @param m
     *            Message received (contains the node to find)
     * @param myPid
     *            the sender Pid
     */
    protected void handleRegisterResponse(Message m, int myPid) {
        Ticket ticket = (Ticket) m.body;
        Topic topic = ticket.getTopic();
        if (ticket.isRegistrationComplete() == true) {
            logger.warning("Registration succesful for topic "+ticket.getTopic().getTopicID()+" at node "+m.src.getId());
            this.numOfRegistrations += 1;
            if (this.numOfRegistrations < this.targetNumOfRegistrations) {
                ticketTable.get(ticket.getTopic().topicID).removeNeighbour(m.src.getId());
            }
        }
        else
            super.handleRegisterResponse(m, myPid);
    }

    /**
     * Process a topic query message.<br>
     * The body should contain a topic. Return a response message containing
     * the registrations for the topic and the neighbors close to the topic.
     * 
     * @param m
     *            Message received (contains the node to find)
     * @param myPid
     *            the sender Pid
     */
    protected void handleTopicQuery(Message m, int myPid) {
        if(!this.attackType.equals(KademliaCommonConfig.ATTACK_TYPE_HYBRID) && !this.attackType.equals(KademliaCommonConfig.ATTACK_TYPE_MALICIOUS_REGISTRAR))
            super.handleTopicQuery(m, myPid);

        Topic t = (Topic) m.body;
        TopicRegistration[] registrations = new TopicRegistration[0];

        if (this.attackType.equals(KademliaCommonConfig.ATTACK_TYPE_HYBRID)) {
            TopicRegistration[] all_registrations = this.topicTable.getRegistration(t);
            if (all_registrations.length > 0) {
                List evilRegList = new ArrayList<TopicRegistration>();
            
                for(TopicRegistration reg: all_registrations) {
                    KademliaNode n = reg.getNode();
                    if (n.is_evil) 
                        evilRegList.add(reg);
                }

                if(evilRegList.size() > 0) 
                    registrations = (TopicRegistration[]) evilRegList.toArray(new TopicRegistration[evilRegList.size()]);
            }
        }
        
        BigInteger[] neighbours = this.routingTable.getNeighbours(Util.logDistance(t.getTopicID(), this.node.getId()));

        Message.TopicLookupBody body = new Message.TopicLookupBody(registrations, neighbours);
        Message response  = new Message(Message.MSG_TOPIC_QUERY_REPLY, body);
        response.operationId = m.operationId;
        response.src = this.node;
        response.ackId = m.id; 
        logger.info(" responds with TOPIC_QUERY_REPLY");
        //System.out.println(" responds with TOPIC_QUERY_REPLY");
        sendMessage(response, m.src.getId(), myPid);
    }
    /**
     * manage the peersim receiving of the events
     * 
     * @param myNode
     *            Node
     * @param myPid
     *            int
     * @param event
     *            Object
     */
    public void processEvent(Node myNode, int myPid, Object event) {

        //super.processEvent(myNode, myPid, event);
        Message m;
        
        SimpleEvent s = (SimpleEvent) event;
        if (s instanceof Message) {
            m = (Message) event;
            m.dest = this.node;
        }
        
        //TODO we could simply let these "handle" calls made in the parent class
        switch (((SimpleEvent) event).getType()) {
            
            case Message.MSG_INIT_REGISTER:
                m = (Message) event;
                handleInitRegisterTopic(m, myPid);
                break;

            case Message.MSG_REGISTER_RESPONSE:
                m = (Message) event;
                handleRegisterResponse(m, myPid);
                break;

            case Message.MSG_TOPIC_QUERY:
                m = (Message) event;
                handleTopicQuery(m, myPid);
                break;

            default :
                super.processEvent(myNode, myPid, event);
        }
    }
}
