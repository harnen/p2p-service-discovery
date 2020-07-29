package peersim.kademlia;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.transport.Transport;

/**
 * Initialization class that performs the bootsrap filling the k-buckets of all initial nodes.<br>
 * In particular every node is added to the routing table of every other node in the network. In the end however the various nodes
 * doesn't have the same k-buckets because when a k-bucket is full a random node in it is deleted.
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class StateBuilderFromDnsList implements peersim.core.Control {

	private static final String PAR_PROT = "protocol";
	private static final String PAR_TRANSPORT = "transport";

	private String prefix;
	private int kademliaid;
	private int transportid;

	public StateBuilderFromDnsList(String prefix) {
		this.prefix = prefix;
		kademliaid = Configuration.getPid(this.prefix + "." + PAR_PROT);
		transportid = Configuration.getPid(this.prefix + "." + PAR_TRANSPORT);
		
		try {
			JSONObject json = readJsonFromUrl("https://raw.githubusercontent.com/ethereum/discv4-dns-lists/master/all.mainnet.ethdisco.net/nodes.json");
			//for(Object n : json.names())
			//System.out.println("JSON "+json);
			//Iterator<String> keys = json.keys();
			for (String keyStr : json.keySet()) {
		        JSONObject json2 = json.getJSONObject(keyStr);
				for (String keyStr2 : json2.keySet()) {
		            //JSONObject object = array.getJSONObject(i);
		        //Print key and value
					System.out.println("Record: "+ json2.getString("record"));
				}
		        //for nested objects iteration if required
		        //if (keyvalue instanceof JSONObject)
		        //    printJsonObject((JSONObject)keyvalue);
		    }
				
		}catch(Exception e) {
			System.err.println("Exception "+e);
		}/*catch(JSONException e) {
			System.err.println("Exception "+e);
		}*/

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

		  
	// ______________________________________________________________________________________________
	public final KademliaProtocol get(int i) {
		return ((KademliaProtocol) (Network.get(i)).getProtocol(kademliaid));
	}

	// ______________________________________________________________________________________________
	public final Transport getTr(int i) {
		return ((Transport) (Network.get(i)).getProtocol(transportid));
	}

	// ______________________________________________________________________________________________
	public static void o(Object o) {
		System.out.println(o);
	}

	// ______________________________________________________________________________________________
	public boolean execute() {

		// Sort the network by nodeId (Ascending)
		Network.sort(new Comparator<Node>() {

			public int compare(Node o1, Node o2) {
				Node n1 = (Node) o1;
				Node n2 = (Node) o2;
				KademliaProtocol p1 = (KademliaProtocol) (n1.getProtocol(kademliaid));
				KademliaProtocol p2 = (KademliaProtocol) (n2.getProtocol(kademliaid));
				return Util.put0(p1.node.getId()).compareTo(Util.put0(p2.node.getId()));
			}

		});

		int sz = Network.size();

		// for every node take 50 random node and add to k-bucket of it
		for (int i = 0; i < sz; i++) {
			Node iNode = Network.get(i);
			KademliaProtocol iKad = (KademliaProtocol) (iNode.getProtocol(kademliaid));

			for (int k = 0; k < 100; k++) {
				KademliaProtocol jKad = (KademliaProtocol) (Network.get(CommonState.r.nextInt(sz)).getProtocol(kademliaid));
				iKad.routingTable.addNeighbour(jKad.node.getId());
			}
		}

		// add other 50 near nodes
		for (int i = 0; i < sz; i++) {
			Node iNode = Network.get(i);
			KademliaProtocol iKad = (KademliaProtocol) (iNode.getProtocol(kademliaid));

			int start = i;
			if (i > sz - 50) {
				start = sz - 25;
			}
			for (int k = 0; k < 50; k++) {
				start = start++;
				if (start > 0 && start < sz) {
					KademliaProtocol jKad = (KademliaProtocol) (Network.get(start++).getProtocol(kademliaid));
					iKad.routingTable.addNeighbour(jKad.node.getId());
				}
			}
		}

		return false;

	} // end execute()

}
