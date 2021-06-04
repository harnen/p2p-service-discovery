package peersim.kademlia;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.Security;
import java.util.Iterator;
import java.util.Set;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.io.Base64URLSafe;
import org.apache.tuweni.devp2p.EthereumNodeRecord;
import org.json.JSONException;
import org.json.JSONObject;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.dynamics.NodeInitializer;

/**
 * This control initializes the whole network (that was already created by peersim) assigning a unique NodeId, randomly generated,
 * to every node.
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class DnsListDistribution extends CustomDistribution {
	protected NodeInitializer[] inits;

	private static final String PAR_INIT = "init";

	private JSONObject json;
	public DnsListDistribution(String prefix) {
<<<<<<< HEAD
		System.out.println("DNSLISTDIstr "+prefix);
		protocolID = Configuration.getPid(prefix + "." + PAR_PROT);
		urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, CommonState.r);
=======
		super(prefix);
>>>>>>> add_ips
		Object[] tmp = Configuration.getInstanceArray(prefix + "." + PAR_INIT);
		inits = new NodeInitializer[tmp.length];
		for (int i = 0; i < tmp.length; ++i) {
			inits[i] = (NodeInitializer) tmp[i];
		}
		
		try {
			json = readJsonFromUrl("https://raw.githubusercontent.com/ethereum/discv4-dns-lists/master/all.mainnet.ethdisco.net/nodes.json");
		}catch(Exception e) {
			System.err.println("Failed to read remote file with nodes "+e);
		}

		
	}

    public boolean execute() {
    	Security.addProvider(new BouncyCastleProvider());
        int num_evil_nodes = (int) (Network.size()*percentEvil);
        System.out.println("Number of evil nodes: " + num_evil_nodes);
        Iterator<String> jsonIt = json.keySet().iterator();
        if (json.keySet().size() < Network.size()){
			System.out.println("Not enough nodes downloaded from the github repo");
			System.out.println("Requested network size:" + Network.size() + ". Maximum possible size: " + json.keySet().size());
			System.exit(-1);
		}

        for (int i = 0; i < Network.size(); ++i) {
        	if(!jsonIt.hasNext()) {
        		System.out.println("Not enough IPs in the fetched file");
        		System.exit(-1);
        	}
        	JSONObject nodeJson = json.getJSONObject(jsonIt.next());
        	EthereumNodeRecord enr = EthereumNodeRecord.fromRLP((Base64URLSafe.decode(nodeJson.getString("record").substring(4))));
			final BigInteger id = enr.publicKey().bytes().slice(KademliaCommonConfig.BITS/8).toUnsignedBigInteger();
			String IP = enr.ip().toString();
			int portNum = enr.tcp();
			
        	
            Node generalNode = Network.get(i);
            KademliaNode node; 
            if (i < num_evil_nodes) {
                int topicNo; 
                if (this.attackTopicNo != -1)
                    topicNo = this.attackTopicNo;
                else 
                    topicNo = zipf.sample();

                String topic = new String("t"+topicNo);
                Topic t = new Topic(topic);
               
                node = new KademliaNode(id, IP, portNum);
                generalNode.setProtocol(protocolID, null);
                node.is_evil = true;
                ((KademliaProtocol) (Network.get(i).getProtocol(evilProtocolID))).setNode(node);
                generalNode.setKademliaProtocol((KademliaProtocol) (Network.get(i).getProtocol(evilProtocolID)));
                ((KademliaProtocol) (Network.get(i).getProtocol(evilProtocolID))).setProtocolID(evilProtocolID);
                //if (idDist.equals(KademliaCommonConfig.NON_UNIFORM_ID_DISTRIBUTION)) 
                ((KademliaProtocol) (Network.get(i).getProtocol(evilProtocolID))).setTargetTopic(t);
                    
            }
            else {
                node = new KademliaNode(id, IP, portNum);
                if (evilProtocolID != -1) {
                    generalNode.setProtocol(evilProtocolID, null);
                }
                generalNode.setKademliaProtocol((KademliaProtocol) (Network.get(i).getProtocol(protocolID)));
                ((KademliaProtocol) (Network.get(i).getProtocol(protocolID))).setNode(node);
                ((KademliaProtocol) (Network.get(i).getProtocol(protocolID))).setProtocolID(protocolID);
            }
        }

        return false;
    }
    
    
	public boolean execute2() {
		Security.addProvider(new BouncyCastleProvider());
		if (json.keySet().size() < Network.size()){
			System.out.println("Not enough nodes downloaded from the github repo");
			System.out.println("Requested network size:" + Network.size() + ". Maximum possible size: " + json.keySet().size());
		}
		
		int i = 0;
		for (String keyStr : json.keySet()) {
<<<<<<< HEAD
			if(i<Network.size()) {
		        JSONObject json2 = json.getJSONObject(keyStr);
				//System.out.println("Record: "+ json2.getString("record").substring(4)+" "+json.keySet().size());
				
				//EthereumNodeRecord enr = EthereumNodeRecord.fromRLP(Bytes.fromHexString(json2.getString("record")));
				EthereumNodeRecord enr = EthereumNodeRecord.fromRLP((Base64URLSafe.decode(json2.getString("record").substring(4))));
			    System.out.println("Record: "+ ((Bytes)enr.getData().get("id")).toString());
				System.out.println("Record: "+ enr.ip());
	
				//enr.va
				KademliaNode node = new KademliaNode(enr.publicKey().bytes().slice(KademliaCommonConfig.BITS/8).toUnsignedBigInteger(), enr.ip().toString(), enr.tcp());
				//node.setProtocolId(protocolID);
				/*if(i>=Network.size()) {
					Node newNode = (Node) Network.prototype.clone();
					for (int j = 0; j < inits.length; ++j)
						inits[j].initialize(newNode);
					Network.add(newNode);
	
					// get kademlia protocol of new node
					KademliaProtocol newKad = (KademliaProtocol) (newNode.getProtocol(protocolID));
					
				}*/
				
	            Node generalNode = Network.get(i);
	
	            generalNode.setKademliaProtocol((KademliaProtocol) (Network.get(i).getProtocol(protocolID)));
	            ((KademliaProtocol) (Network.get(i).getProtocol(protocolID))).setNode(node);
	            ((KademliaProtocol) (Network.get(i).getProtocol(protocolID))).setProtocolID(protocolID);
					
				//((KademliaProtocol) (Network.get(i).getProtocol(protocolID))).setNode(node);
				i++;
			} else {
				break;
=======
	        JSONObject json2 = json.getJSONObject(keyStr);
			EthereumNodeRecord enr = EthereumNodeRecord.fromRLP((Base64URLSafe.decode(json2.getString("record").substring(4))));
			BigInteger nodeID = enr.publicKey().bytes().slice(KademliaCommonConfig.BITS/8).toUnsignedBigInteger();
			String IP = enr.ip().toString();
			int portNum = enr.tcp();
			
			
			KademliaNode node = new KademliaNode(nodeID, IP, portNum);
			System.out.println("IP:" + enr.ip().toString() + " TCP:" + enr.tcp());
			//node.setProtocolId(protocolID);
			if(i>=Network.size()) {
				Node newNode = (Node) Network.prototype.clone();
				for (int j = 0; j < inits.length; ++j)
					inits[j].initialize(newNode);
				Network.add(newNode);

				// get kademlia protocol of new node
				KademliaProtocol newKad = (KademliaProtocol) (newNode.getProtocol(protocolID));
>>>>>>> add_ips
			}
				
	    }
		
		while(i<Network.size())
			Network.remove(i);

		return false;
	}
	
	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
    	InputStream is = new URL(url).openStream();
    	try {
    		BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
    		String jsonText = readAll(rd);
    		//System.out.println("JSON string "+jsonText);
    		JSONObject json = new JSONObject(jsonText);
    		return json;
    	} finally {
    		is.close();
    	}
    }

}
