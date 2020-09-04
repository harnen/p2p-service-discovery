package peersim.kademlia;

public class LookupOperation extends Operation {
	final Topic topic;
	
	public LookupOperation(Long timestamp, Topic t) {
		super(t.getTopicID(), Message.MSG_TOPIC_QUERY, timestamp);
		this.topic = t;
	}

}
