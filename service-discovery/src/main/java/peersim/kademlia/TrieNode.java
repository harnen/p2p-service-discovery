package peersim.kademlia;
import java.util.logging.Logger;

public class TrieNode {

    private int count;
    private TrieNode zero;
    private TrieNode one;
    private static final Logger logger = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName()  );

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
        } 
        currNode.increment();
        
        return (1.0*score)/(31.0*root.count());
    }
    
    public static void removeIp(TrieNode root, String ip) {

        //logger.info("Removing ip: " + ip);
        TrieNode currNode = root;
        TrieNode prevNode = root;
        boolean delete = false;
        int length = 0;

        for (; length < 32; length++) {

            currNode.decrement();
            if (currNode.count() == 0) {
                delete = true;
                break;
            }
    
            prevNode = currNode;
            char bit = ip.charAt(length);
            if (bit == '0') 
               currNode = currNode.zero();

            else if (bit == '1')
                currNode = currNode.one();
        }

        if (delete) {
            if(prevNode.zero() == currNode)
                prevNode.zero = null;
            else if (prevNode.one() == currNode)
                prevNode.one = null;

            for (; length < 32; length++) {
                char bit = ip.charAt(length);
                prevNode = currNode;
                if (bit == '0') {
                    currNode = currNode.zero();
                    prevNode.zero = null;
                }
                else if (bit == '1') {
                    currNode = currNode.one();
                    prevNode.one = null;
                }
                currNode.decrement();
                //logger.info("currNode count: " + currNode.count());

                assert currNode.count() == 0;
            } 
        }
    }
}
