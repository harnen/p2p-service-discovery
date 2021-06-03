# Milestone 3 (Security analysis) report

## Intro
The document presents a security analysis of the proposed discovery protocol for the Ethereum  P2P network (discv5) following the described in the following [document](https://github.com/datahop/p2p-service-discovery/blob/d67a7ccd2b4c2c6bec38f5987c99cb13ea074cdc/doc/specs.md). 

## Assumptions

Assumptions followed during the security analysis:

* A1 - the network consists of a large amount of nodes (1k-10k)
* A2 - each honest node has a unique node ID and an IP address. The same IP address may be shared by a small number of honest nodes (e.g., due to NAT). 
* A3 - each node can register to multiple topics. 
* A4 - the popularity of the topics may vary significantly and follows a zipf distribution
* A5 - no single node can be trusted.
* A6 - malicious nodes may be present in the network. The attackers may send any type and amount of messages to any other node, limited only by their the attacker resources (bandwidth, CPU).
* A7 - a single malicious participant can produce a large amount of fake (Sybil) identities. The attacker will be limited in the number of the IP addresses and IDs they control. We expect an attacker being able to generate much more different IDs than IPs.


## Design Goals
Under the assumptions above we aim to achieve the following properties:

* G1 - all the registrants (regardless of the topic they register for) should be able to place their advertisements in the network. Aka no registrants can be globally denied registrations.
* G2 - all the registrants within each topic should have a similar probability of being discovered by their peers.
* G3 - the load (in terms of sent and received messages) should be equally distributed across all the nodes regardless of their ID and location in the network
* G4 - the registration operation should be efficient in terms of time (fast) for all the registrants
* G5 - the registration operation should be efficient in terms of overhead (low amount of sent/received messages) for all the registrants
* G6 - the lookup operation should be efficient in terms of time (fast) and messages sent (hop count) for all the query nodes
* G7 - the number of total registrations per topic should be proportional to the popularity of the topic (the number of registrants).
* G8 - the protocol should be resistant to network dynamic (nodes joining leaving)
* G9 - the protocol should be resistant to sybil attacks launched by malicious nodes as described in the section below.


For the security analysis we performed two different evaluations: 

 * A network simulation evaluation (using Peersim simulator) to evaluate the performance and check the security goals of the whole protocol. 
 * A second evaluation using a Python simulator to analyse the performance of the registration tables under different attacks and loads.

## Network Simulation analysis (Peersim)

### Setup

### Attacks evaluated

* Hybrid attack: It combines spamming attack to existing topic attacks, where evil 'registrants' try to place as much as registrations as possible by using bigger ticket size , with malicious registrars attack, where evil registrars replies with only malicious nodes when receiving a topic query.
* Random topic spam attack: This attack tries to attack registrar topic table by spamming registrations of non-existing topics. 
* Dos registrar attack: Attack where malicious nodes try to backlog registrations from registrants by returning very long waiting times and reduce total number of registrations and therefore the performance of the system.


### Hybrid Attack

#### Active registrations

#### Eclipsed nodes
<p align="center">
  <img src="./img/hybrid/eclipsed_nodes_t1.png" width="30%" />
  <img src="./img/hybrid/eclipsed_nodes_t3.png" width="30%" />
  <img src="./img/hybrid/eclipsed_nodes_t5.png" width="30%" />
</p>

#### Lookup performance

<p align="center">
  <img src="./img/hybrid/lookup_hopcount_t1_0.2.png" width="30%" />
  <img src="./img/hybrid/lookup_hopcount_t3_0.2.png" width="30%" />
  <img src="./img/hybrid/lookup_hopcount_t5_0.2.png" width="30%" />
</p>

<p align="center">
  <img src="./img/hybrid/lookup_hopcount_t1_0.2.png" width="30%" />
  <img src="./img/hybrid/lookup_hopcount_t3_0.2.png" width="30%" />
  <img src="./img/hybrid/lookup_hopcount_t5_0.2.png" width="30%" />
</p>

<p align="center">
  <img src="./img/hybrid/lookup_hopcount_t1_0.2.png" width="30%" />
  <img src="./img/hybrid/lookup_hopcount_t3_0.2.png" width="30%" />
  <img src="./img/hybrid/lookup_hopcount_t5_0.2.png" width="30%" />
</p>
#### Registration/discovery time

#### Discovered nodes distribution


### Random Topic Spam Attack

## DoS Registrar Attack



## Table Occupancy analysis (Python)
