package peersim.kademlia;

import java.net.InetAddress;
import java.math.BigInteger;

public class KademliaNode {
    private BigInteger id;
    private String addr;
    private int port;

    public KademliaNode(BigInteger id, String addr, int port){
        this.id = id;
        this.addr = addr;
        this.port = port;
    }
    
    public BigInteger getId(){
        return this.id;
    }

    public String getAddr(){
        return this.addr.toString();
    }

    public int getPort(){
        return this.port;
    }
}