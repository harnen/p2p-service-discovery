package peersim.kademlia;
import java.util.logging.Logger;

public class TrieNode {

    private int count;
    private TrieNode zero;
    private TrieNode one;
    private static final Logger logger = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName()  );
	int[] comparators = new int[] {128, 64, 32, 16, 8, 4, 2, 1};

    public TrieNode(){ 
        count = 0;
        zero = null;
        one = null; 
    }

    private TrieNode zero() {
        return zero;
    }

    private TrieNode one() {
        return one;
    }    

    private int count() {
        return count;
    }

    private void increment() {
        count += 1;
    }

    private void decrement() {
        count -= 1;
    }

    private boolean isLeaf() {
        if(one == null && zero == null)
            return true;
        return false;
    }
    // adds an ip to trie
    // returns similarity score
    public static double getSimilarityScore(TrieNode root, String ip) { 

        int score = 0; 
        TrieNode currNode = root;

        for (int length = 0; length < 32; length++) {

            score += currNode.count();
            char bit = ip.charAt(length);
            if (bit == '0') {
               currNode = currNode.zero();
            }
            else if (bit == '1') {
                currNode = currNode.one();
            }
            if (currNode == null)
                break;
        } 
        
        return (1.0*score)/(31.0*root.count());
    }
    
    // adds an ip to trie
    // returns similarity score
    public static double addIp(TrieNode root, String ip) { 

        int score = 0; 
        TrieNode currNode = root;
        //logger.info("Adding ip: " + ip);

        for (int length = 0; length < 32; length++) {

            score += currNode.count();
            currNode.increment();

            String prefix = ip.substring(0, length);
            //logger.info("Increment prefix: " + prefix + " to " + currNode.count());

            char bit = ip.charAt(length);

            if (bit == '0') {
               if (currNode.zero() == null) {
                    currNode.zero = new TrieNode();
               }
               currNode = currNode.zero();
            }
            else if (bit == '1') {
                if (currNode.one() == null) {
                    currNode.one = new TrieNode();
                }
                currNode = currNode.one();
            }
            else { 
                System.out.println("invalid ip address: " + ip);
                System.exit(-1);
            }
        } 
        score += currNode.count();
        currNode.increment();
        
        return (1.0*score)/(31.0*root.count());
    }
    
    public static void removeIp(TrieNode root, String ip) {

        //logger.info("Removing ip: " + ip);
        TrieNode currNode = root;
        TrieNode prevNode = root;
        boolean delete = false;
        int length;

        boolean zero = false;
        for (length = 0; length < 32; length++) {

            String prefix = ip.substring(0, length);

            //logger.info("Before Decrement prefix: " + prefix + " to " + currNode.count());
            currNode.decrement();
            //logger.info("After Decrement prefix: " + prefix + " to " + currNode.count());
            
            if (currNode.count() == 0) {
                zero = true;
                if (currNode != root)
                {
                    char bit = ip.charAt(length-1);
                    if (bit == '0') {
                        prevNode.zero = null;
                    }
                    else { //if (bit == '1') 
                        prevNode.one = null;
                    }
                }
            }
            else {
                if (zero) {
                    assert (currNode.count() == 0) : "count must be greater than zero, but count = " + currNode.count() + " " + length;
                }
            }
            prevNode = currNode;

            // advance currNode
            char bit = ip.charAt(length);
            if (bit == '0') {
                currNode = currNode.zero();
            }
            else { 
                currNode = currNode.one();
            }
        }
        currNode.decrement();
        if (currNode.count() == 0) {
            char bit = ip.charAt(length-1);
            if (bit == '0') {
                prevNode.zero = null;
            }
            else { //if (bit == '1') 
                prevNode.one = null;
            }
        }
    }
}
