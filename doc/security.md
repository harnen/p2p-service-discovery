
# Ethereum 2 service discovery vulnerabilities

Ethereum network, and its service discovery system based on Kademlia decentralized hash table (DHT), as a permissionless peer-to-peer network where creation of multiple identities by single users is possible, is especially vulnerable to sybil attack when trying to manipulate the normal functioning of the network, and eclipse attack when the adversary targets a specific node (as opposed to the network as a whole) so as to cut off all of their inbound/outbound communications with other peers. This attack allows an attacker  to create a virtual partition to isolate those victim nodes from the rest of the network. In the following, we list the specific vulnerabilities and possible countermeasures for the existing Ethereum discovery service.

## Vulnerabilities Ethereum discv4

* False friend attack in inbound connections:
  * Description: Eclipsing inbound connections simply starting multiple Geth instances on different ports and configuring them to repeatedly connect to the victim.   * Countermeasure: Avoiding same domain ip in inbound connections (already implemented?)
  * Resource: https://arxiv.org/abs/1908.10141

* False friend attack in outbound connections using table poisoning:
  * Description: The outcome is to fill the whole discovery table with Sybil nodes to ensure that only these are proposed to the peer management, we leverage properties of the peer selection mechanism to achieve the same result with only one Sybil node per neighbor table bucket. It suffices to have one Sybil node in each bucket of the neighbour table to make sure that only adversarial nodes are returned to the peer management. Specifically, an attacker still can exploit the public peer selection vulnerability  to poison the victim nodes’ routing tables when these tables are reboot and reset (e.g., the attacker could craft fake nodes and insert them into those routing tables to make the victim nodes’ outgoing TCP connections point to the fake nodes controlled by the attacker). This effectively circumvents the implemented countermeasures and still needs very little resources (two IP addresses in distinct /24 subnets). 
  * Countermeasure:  fixed mapping between the IP address and ECDSA key making the mapping of IDs to buckets in secret, but will lose the benefits of logarithmic routing in Kademlia
  * Resource: https://arxiv.org/abs/1908.10141

* Attack by manipulating time: 
  * Description: manipulating the local clock at a victim node can turns a ‘established’ node with knowledge of the network (and is known to other geth nodes in the network) into one that knows nothing about the network (and is unknown to other geth nodes). Worse yet, the victim will refuse to accept network information learned from most honest nodes, while happily accepting information from the attacker. This is because the victim will reject a UDP message if its timestamp is more than 20 seconds old.  The victim will forget about all other nodes and other nodes will forget about the victim, being especially vulnerable to eclipse attacks.
  * Countermeasure: countermeasures for timing attacks. https://www.ndss-symposium.org/wp-content/uploads/2017/09/attacking-network-time-protocol.pdf


## Vulnerabilities in Ethereum discv5

* Assumptions: Based on kademlia network and inherits discv4 current security issues

* Advertising medium attack
  * Description: Eclipsing others by only replying herself to lookups, discarding other advertisers registrations in the local table.
  * Countermeasure: Same as 2.

* Advertise registrant / Advertising medium attack
  * Description: Trying to register to too many advertising mediums to have control of the network. An attacker can try to create fake ids close to a topic hash to receive all advertise registrations but also  to receive all service lookups.
  * Countermeasure: Detect Id's distribution abnormality created by fake ids
  * Resource: https://hal.inria.fr/inria-00490509/file/HotP2P10-KAD_DHT_attack_mitigation-cholez.pdf



## To be implemented in the simulator:

### Contermeasures/ Security Analysis:
* IP address variable in nodes information. (done)
* IP address random allocation, multiple nodes with the same IP address and IP address from dnslist.
* Limit of nodes ip per table bucket.
* DHT attack mitigation through peers’ ID distribution. Fake ids cause proximity and density abnormalities in IDs. According to https://hal.inria.fr/inria-00490509/file/HotP2P10-KAD_DHT_attack_mitigation-cholez.pdf can be detected  using Kullback-leibler divergence with low	 false-positive and false-negative rates.
http://java-ml.sourceforge.net/api/0.1.4/net/sf/javaml/featureselection/scoring/KullbackLeiblerDivergence.html 
	

## Scenarios:
* Scenario 1: Simulation with/without IP address limitations
* Scenario 2: Simulation with variable number of nodes discarding other advertisers registrations in the local table and advertising only herself.
* Scenario 3: Simulation with variable number of nodes creating n  fake ids close to a topic hash.
* Scenario 4: Scenario 3 with Kullback-leibler divergence mechanism.


# Attack Scenarios

## Attacks as registrar
* Region hijacking query attack - an attacker creates multiple identities close to the target topic hash, accepts registrations, but rejects topic query. Succeeds if: users cannot discover peers for the topic being attacked.

## Attacks as registrant
* Target registrar spam attack - an attacker creates multiple registrations (potentially with Sybil identities) for a topic close (or equal) to the id of the targeted registrar. Succeeds if: the registrar does not accept registrations for other topics (being further away from the registrar hash)
* Target topic spam attack - an attacker creates multiple registrations (potentially with Sybil identities) for a specific topic. The attacker hope to reduce or completely flush legitimate registrations for this topic. Succeeds if: users cannot discover peers for the topic being attacked.
