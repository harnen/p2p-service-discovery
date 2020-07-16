package peersim.kademlia;

/**
 * Fixed Parameters of a kademlia network. They have a default value and can be configured at startup of the network, once only.
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class KademliaCommonConfig {

	public static int BITS = 160; // length of Id (default is 160)

	public static int K = 20; // dimension of k-buckets (default is 5)
	public static int ALPHA = 3; // number of simultaneous lookup (default is 3)
	public static int TOPIC_TABLE_CAP = 100; //the number of topics per node we can regiter
    public static int ADS_PER_QUEUE = 10; //the number of ads per topic queue
    public static int AD_LIFE_TIME = 100; //life time of ads the topic table

	/**
	 * short information about current mspastry configuration
	 * 
	 * @return String
	 */
	public static String info() {
		return String.format("[K=%d][ALPHA=%d][BITS=%d][TOPIC_TABLE_CAP=%d]", K, ALPHA, BITS, TOPIC_TABLE_CAP);
	}

}
