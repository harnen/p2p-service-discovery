package peersim.kademlia;

import java.util.Arrays;
import java.math.BigInteger;

import peersim.core.Node;
import peersim.config.Configuration;
import peersim.kademlia.KademliaCommonConfig;
import peersim.kademlia.operations.Operation;
import peersim.kademlia.operations.RegisterOperation;


public class Discv5ProposalEvilProtocol extends Discv5ProposalProtocol {

    // VARIABLE PARAMETERS
    final String PAR_ATTACK_TYPE = "attackType";
    final String PAR_NUMBER_OF_REGISTRATIONS = "numberOfRegistrations";

    // type of attack (TopicSpam)
    private String attackType;
    private int targetNumOfRegistrations;
    public int numOfSuccessfulRegistrations;


    public Discv5ProposalEvilProtocol(String prefix) {
        super(prefix);
        this.attackType = Configuration.getString(prefix + "." + PAR_ATTACK_TYPE);
        this.targetNumOfRegistrations = Configuration.getInt(prefix + "." + PAR_NUMBER_OF_REGISTRATIONS, 10);
        this.numOfSuccessfulRegistrations = 0;
    }

    /**
     * Replicate this object by returning an identical copy.<br>
     * It is called by the initializer and do not fill any particular field.
     * 
     * @return Object
     */
    public Object clone() {
        Discv5ProposalEvilProtocol dolly = new Discv5ProposalEvilProtocol(Discv5ProposalEvilProtocol.prefix);
        return dolly;
    }
    
    /**
     * Start a register opearation.<br>
     * Find the ALPHA closest node and send register request to them.
     * 
     * @param m
     *            Message received (contains the node to find)
     * @param myPid
     *            the sender Pid
     */
    private void handleInitRegister(Message m, int myPid) {
        Topic t = (Topic) m.body;
        TopicRegistration r = new TopicRegistration(this.node, t);
        System.out.println("Spamming topic registration for topic "+t.getTopic());

        KademliaObserver.addTopicRegistration(t.topic, this.node.getId());
    
        RegisterOperation rop = new RegisterOperation(this.node.getId(), m.timestamp, t, r);
        rop.body = m.body;
        operations.put(rop.operationId, rop);
        
        int distToTopic = Util.logDistance((BigInteger) t.getTopicID(), this.node.getId());
        BigInteger[] neighbours = this.routingTable.getNeighbours(distToTopic);
        
        // Get all neighbors
        neighbours = this.routingTable.getKClosestNeighbours(this.targetNumOfRegistrations, distToTopic);
        /*
        if(neighbours.length < KademliaCommonConfig.ALPHA)
            neighbours = this.routingTable.getKClosestNeighbours(KademliaCommonConfig.ALPHA, distToTopic);
        */
        rop.elaborateResponse(neighbours);
    
        m.operationId = rop.operationId;
        m.type = Message.MSG_REGISTER;
        m.src = this.node;
        
        int numParallelRegisterations = this.targetNumOfRegistrations;

        // send parallel register messages
        for (int i = 0; i < numParallelRegisterations; i++) {
            BigInteger nextNode = rop.getNeighbour();
            //System.out.println("Nextnode "+nextNode);
            if (nextNode != null) {
                m.dest = new KademliaNode(nextNode);
                sendMessage(m.copy(), nextNode, myPid);
                rop.nrHops++;
            }//nextNode may be null, if the node has less than ALPHA neighbours
            else {
                rop.available_requests = i;
                System.out.println("Number of parallel registrations " + i);
			    //logger.warning("No neighbor to register");
                break;
            }
        }

    }
	/**
	 * Perform the required operation upon receiving a message in response to a ROUTE message.<br>
	 * Update the find operation record with the closest set of neighbour received. Then, send as many ROUTE request I can
	 * (according to the ALPHA parameter).<br>
	 * If no closest neighbour available and no outstanding messages stop the find operation.
	 * 
	 * @param m
	 *            Message
	 * @param myPid
	 *            the sender Pid
	 */
	protected void handleResponse(Message m, int myPid) {
		
        Operation op = (Operation)	 this.operations.get(m.operationId);
        if (op == null) {
            return;
        }
        if (op.type == Message.MSG_REGISTER) {
            if (this.numOfSuccessfulRegistrations >= this.targetNumOfRegistrations) {
			    logger.warning("Reached target number of spam registrations.");
                op.finished = true;
            }
        }
        super.handleResponse(m, myPid);
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
    private void handleRegisterResponse(Message m, int myPid) {
        boolean is_success = (boolean) m.body;

        if (is_success) 
            this.numOfSuccessfulRegistrations += 1;

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
		
        if(((SimpleEvent) event).getType() == Timeout.TIMEOUT) {
            System.out.println("Timeout in evil protocol");
            return;
        }

        Message m = (Message) event;
        m.dest = this.node;
        
        if (m.src != null) {
            routingTable.addNeighbour(m.src.getId());
            failures.replace(m.src.getId(), 0);
        }
        
        switch (((SimpleEvent) event).getType()) {
            case Message.MSG_INIT_REGISTER:
                handleInitRegister(m, myPid);
                break;          

            case Message.MSG_REGISTER_RESPONSE:
                m = (Message) event;
				//sentMsg.remove(m.ackId);
                handleRegisterResponse(m, myPid);
                break;
            default :
                super.processEvent(myNode, myPid, event);
        }
    }

}
