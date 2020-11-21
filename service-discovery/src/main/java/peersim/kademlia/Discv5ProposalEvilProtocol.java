package peersim.kademlia;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
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
	 * This procedure is called only once and allow to inizialize the internal state of KademliaProtocol. Every node shares the
	 * same configuration, so it is sufficient to call this routine once.
	 */
	protected void _init() {
		// execute once
		if (_ALREADY_INSTALLED)
			return;

		super._init();
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
    protected void handleInitRegister(Message m, int myPid) {
        Topic t = (Topic) m.body;
        
        if (this.attackType.equals(KademliaCommonConfig.ATTACK_TYPE_TOPIC_SPAM) || this.attackType.equals(KademliaCommonConfig.ATTACK_TYPE_HYBRID) ) {

            TopicRegistration r = new TopicRegistration(this.node, t);
    
            KademliaObserver.addTopicRegistration(t, this.node.getId());
    
            RegisterOperation rop = new RegisterOperation(this.node.getId(), m.timestamp, t, r);
            rop.body = m.body;
		    rop.type = Message.MSG_REGISTER;
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
                if (nextNode != null) {
                    m.dest = new KademliaNode(nextNode);
                    sendMessage(m.copy(), nextNode, myPid);
                    rop.nrHops++;
                }//nextNode may be null, if the node has less than ALPHA neighbours
                else {
                    rop.available_requests = i;
                    logger.info("Number of parallel registrations " + i);
                    break;
                }
            }
        }
        else 
            super.handleInitRegister(m, myPid);
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
        
        Operation op = (Operation)   this.operations.get(m.operationId);
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
    protected void handleRegisterResponse(Message m, int myPid) {
        Topic t = (Topic) m.body;

        KademliaObserver.reportActiveRegistration(t, this.node.is_evil);
        this.numOfSuccessfulRegistrations += 1;
    }   

    protected void handleTopicQuery(Message m, int myPid) {

        if(!this.attackType.equals(KademliaCommonConfig.ATTACK_TYPE_HYBRID) && !this.attackType.equals(KademliaCommonConfig.ATTACK_TYPE_MALICIOUS_REGISTRAR))
            super.handleTopicQuery(m, myPid);
        
        Topic t = (Topic) m.body;
        TopicRegistration[] registrations = new TopicRegistration[0];

        if (this.attackType.equals(KademliaCommonConfig.ATTACK_TYPE_HYBRID)) {
            // Return only malicious nodes as results (if registered)
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
        assert m.src != null;
        response.dest = m.src;
        response.ackId = m.id; 
        logger.info(" responds with TOPIC_QUERY_REPLY");
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
        
        if(((SimpleEvent) event).getType() == Timeout.TIMEOUT) {
            logger.warning("Timeout in evil protocol");
            return;
        }

        Message m = (Message) event;
        m.dest = this.node;
        
        if (m.src != null) {
            routingTable.addNeighbour(m.src.getId());
        }
        
        //TODO we could simply let these "handle" calls made in the parent class
        switch (((SimpleEvent) event).getType()) {
            case Message.MSG_INIT_REGISTER:
                handleInitRegister(m, myPid);
                break;          
            case Message.MSG_TOPIC_QUERY:
                handleTopicQuery(m, myPid);
                break;
            default :
                super.processEvent(myNode, myPid, event);
        }
    }
}
