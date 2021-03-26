# Research project on Service Discovery in Ethereum 2.0 (continuation of [this project](https://github.com/harnen/p2p-service-discovery))

## Overview
 

This is a follow-up research project on Service Discovery in Ethereum 2.0 (Discv5) (available at: https://github.com/harnen/p2p-service-discovery). The previous project was successfully completed, but requires additional security analysis, traffic optimization and documentation to allow integration and deployment in the production code. 

DEVP2P is a set of network protocols that form the Ethereum peer-to-peer network and act as an entry point for multiple different networks and applications. A service discovery system is thus necessary to allow each user to find its peers. The main challenge facing such a decentralised system is to efficiently manage the scarce resources of registrant nodes. The platform needs to decide how to place service advertisements to enable fast lookups for applications with thousands of users and avoid having less popular services to be hard to find by their users. This task is crucial for the efficiency of the whole platform, but becomes especially challenging under the presence of malicious nodes who can try to manipulate or overload the system.

To the best of our knowledge, there is no other existing work that proposes a service discovery mechanisms suitable for work with Ethereum 2.0. Similar work has been done for p2p content exchange networks (e.g., BitTorrent), but either require assistance from a centralized tracker, are not scalable enough or comes with an additional set of assumptions. Our goal is to discover nodes offering a specific service, without having a separate, dedicated p2p network (or a swarm) for each service.. Our solution aims at proposing a new service discovery mechanism for the DEVP2P network that can be used to discover any service in the network efficiently, with good performance for any service regardless of the popularity and with resistance to malicious nodes behaviour (with the focus on sybil, spam and eclipse attacks).

The specific outcome of this project is to improve the service discovery mechanism, designed during the last project, on already identified aspects, such as traffic load balancing, optimal topic table structure or measurements on the resistance against various (e.g., DoS, Sybil and Eclipse) attacks by malicious nodes.
The outcome of this project will be:
* Improvement of the existing network simulator.
* Specifications of the service discovery mechanism with the proposed improvements.
* Technical report including the performance evaluation of the proposed mechanism along
with an evaluation of the security resistance against security attacks.
* (Optional) Service discovery performance evaluation in a real testbed using production
software (go-ethereum).

## Project Plan

The project is organised in three main milestones of the project together with their deliverables. We represent our progress with the following:
* [X] Complete tasks 
* [ ] Pending or uncomplete tasks

* Objective 1: Discv5 simulation environment
  * [X] Task 1: Selection of an appropriate simulator: Investigate the scalability of the Speer simulator (https://github.com/danalex97/Speer) which is written in Go. In case of problems, we will proceed with Peersim (http://peersim.sourceforge.net/) simulator in the rest of the tasks.
  * [X] Task 2: Adding Ethereum DHT to the simulator: Implement the Ethereum’s modified version of Kademlia protocol in the chosen simulator.
  * [X] Task 3: Adding discv5 to the simulator: Implement the existing discovery protocol using radius estimation in the chosen simulator.
  * Deliverables: Discv5 network simulator, including code repository, docs and examples. Deadline: Deadline: 31st August 2020.

* Objective 2: Implementation of the proposed enhancements to discv5
  * [X] Task 4: New discv5 discovery: Implementation of the proposed topic registration and discovery protocol where registrar nodes make individual admission decisions on topic registration requests (based on considerations such as load, popularity of topic, and so on) in the chosen simulator.
  * [X] Task 5: Adding Sybil resistance to discv5: Detection of Sybil attacks by evaluating different evaluating differenct attack vectors and proposing countermeasures.
  * [X] Task 6: Large-scale simulations: Run large-scale simulations in a setting with hundred of thousands of nodes.
  Deadline: 30th September 2020.
  * Deliverables: Service discovery implementation in the simulation environment and an initial version of the specification.
  
* Objective 3: Performance evaluation and improvement
  * [X] Task 7: Add threat model and performance analysis: Implement malicious nodes and computation of performance metrics for discovery and Sybil resistance.
  * [X] Task 8: Evaluation of performance: Investigate the effectiveness of the proposed extensions under heavy presence of malicious nodes and make necessary changes to optimise the performance.
  * Deliverables: Analysis and performance evaluation of service discovery in the presence of malicious actors and final specification of the protocol. Deadline: 30th November 2020.
  
## Service Discovery Requirements

[Requirements](doc/requirements.md)

## Security Analysis

[Security](doc/security.md)


## Discv5 Service Discovery Design

[Design](doc/design.md)

## Service Discovery Simulator

[Simulator](doc/simulator.md)

<!--## Service Discovery Proposition

This is our proposition for topic registration and service discovery in Ethereum 2.0.

### Terms

* A 'topic' is an identifier for a service provided by a node.
* An 'advertiser' is a node providing a service that wants to be found.
* An 'ad' is the registration of an advertiser for a topic on another node.
* An 'advertisement medium' is a node on which an ad is stored.
* A 'searcher' is a node looking for ads for a topic.

### Ad Storage 

### Ad registration

The registrant tries to register on every node during its random walk towards `H(t)` and let the nodes on the path decide whether to accept it or not.

The decision could be taken based on the node's distance from H(t) (the closer, the higher chance of accepting) and the current number of registration among all the topics (the more registrations, the lower chance of accepting). It would create a balanced system where, if there are not many registrations in the system, nodes would accept any registration, but when the traffic is increasing, each node would start to specialize in topics close to its own ID.

If the registrant finds that it doesn't have incoming connection, it can repeat the process using alternative paths (or repeat on the same one if the registration decision is made nondeterministic).

The register message can be attached to `FINDNODE` message. The registrant tries to perform the regular DHT node lookup towards H(t). At te same time, it would try to register on every node it queries.

### Service Lookup

### Messages

* **MSG_FIND**
Used to find nodes. User is requesting destination neighbours at a certain distance `d` indicated in the body.
The body is the common prefix length (log distance) between the recipient of the message and the node we're looking for. 

* **MSG_REPLY**
Replies to MSG_FIND. Returns the neighbours requested by MSG_FIND. In the body contains a list neighbours from a bucket at distance `d` indicated by MSG_FIND. 
If the indicated bucket does not have enough nodes ( < KademliaCommonConfig.K), we get nodes from `d+1` and `d-1`. 

* **MSG_REGISTER**
Registers a topic. Performs a walk towards the node closest to the hash of the topic trying to register on all the encountered nodes. 
In the body contains a topic `t` to be registered. MSG_REGISTERED is answered with MSG_REPLY at the moment containing neighbours closest to the hash of `t`. 

* **MSG_LOOKUP**
Currently not yet implemented. Looking for registrations for specific topic indicated in the body. 

* **MSG_LOOKUP_REPLY**
This message should contain a list of registrations for the requested topic AND list of neighbours closest to the hash of the topic (similarely to MSG_REPLY). 
We should decide how to implement it. 


### Open aspects

* Should nodes involved in a topic also keep information about other nodes in the topic? So that if you find one, you should be able to find others?
* Should we have some replication? Or only rely on registrations performed during the random walk?
* Should we use multiple hash functions (or different salt)? If we use different hash functions, we'd have different H(t) for the same topic. It i) provide necessary redundancy (if the node close to H(t) is down, we don't loose all the information) ii) we could easily perform parallel lookups (similar to alpha, but here, it would go to different instead of the same H(t)
* Spamming more registration shouldn't result in more state created on different nodes - this is necessary to prevent spamming attacks.-->

## Simulation results

<!--[Ticket vs No Ticket protocol comparison](doc/report/report.md)

[Security report comparing two discovery protocols](doc/security_report/report.md)

[Security report for final protocol](doc/ticket_security_report/report.md)-->

[Evaluation results](doc/FinalReport.pdf)

## Resources 

[Eth crawling results](https://github.com/ethereum/discv4-dns-lists)

[How the list above is created](https://geth.ethereum.org/docs/developers/dns-discovery-setup)

## Notes

* p2p/discover <--- use this
* p2p/discv5 <----------- OLD, don't use

* [Specification of the wire protocol](https://github.com/ethereum/devp2p/blob/master/discv5/discv5-wire.md#findnode-request-0x03) - for find node, we don't reveal the node you're looking for. This is to avoid malicious peers to generate the node being looked for on the fly and responding with this fake identity. 

* If we have parallel lookups we need to make sure that the paths are disjoin. 

* [S-Kademlia](https://www.sciencedirect.com/science/article/abs/pii/S1389128615004168)

* [Check this code](https://github.com/ethereum/go-ethereum/blob/master/p2p/discover/v5_udp.go#L280) - here's the main for the protocol, handling messages etc. How a look the lookupWorker and lookupDistances

## Other

[Service discovery implementation](doc/discovery.md)
