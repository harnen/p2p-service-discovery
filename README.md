# Research project on Service Discovery in Ethereum 2.0 (Discv5)

## Overview
 
DEVP2P is a set of network protocols which form the Ethereum peer-to-peer network and act as an entry point for multiple different networks and applications. A service discovery system is thus necessary to allow each user to find its peers. The main challenge facing such a decentralised system is to efficiently manage the scarce resources of registrant nodes. The platform needs to decide how to place service advertisement to enable fast lookups for application with thousands of users and avoid starving users of less popular services. This task is crucial for the efficiency of the whole platform, but becomes especially challenging under the presence of malicious nodes who can try to manipulate or overload the system.

The goal of this project is to develop a secure, efficient and robust service discovery protocol for Ethereum 2.0 (Discv5). The project has 3 main stages:

* Develop a Discv5 simulation environment allowing us to prototype and evaluate different variants of the protocol (Objective 1).
* Implement the proposed mechanism of service discovery (Objective 2).
* Perform an extensive evaluation, verification and incremental improvement of the proposed protocol (Objective 3).

An improved service discovery system will increase the synchronization speed between peers, improve user experience and enhance the overall security of the network by providing larger and more divers set of peers to connect to.

## Project Plan

The project is organised in three main milestones of the project together with their deliverables.
* Objective 1: Discv5 simulation environment
  * Task 1: Selection of an appropriate simulator: Investigate the scalability of the Speer simulator (https://github.com/danalex97/Speer) which is written in Go. In case of problems, we will proceed with Peersim (http://peersim.sourceforge.net/) simulator in the rest of the tasks.
  * Task 2: Adding Ethereum DHT to the simulator: Implement the Ethereum’s modified version of Kademlia protocol in the chosen simulator.
  * Task 3: Adding discv5 to the simulator: Implement the existing discovery protocol using radius estimation in the chosen simulator.
  * Deliverables: Discv5 network simulator, including code repository, docs and examples. Deadline: after 3 months.

* Objective 2: Implementation of the proposed enhancements to discv5
  * Task 4: New discv5 discovery: Implementation of the proposed topic registration and discovery protocol where registrar nodes make individual admission decisions on topic registration requests (based on considerations such as load, popularity of topic, and so on) in the chosen simulator.
  * Task 5: Adding Sybil resistance to discv5: Detection of Sybil attacks by observing whether the distribution of the identifiers found during a search of a target diverges signifi- cantly from the theoretical distribution of identifiers.
  * Task 6: Add threat model and performance analysis: Implement malicious nodes and computation of performance metrics for discovery and Sybil resistance.
  * Deliverables: New Sybil-resistant service discovery implementation in the simulation environment and an initial version of the specification. Deadline: after 4 months.
  
* Objective 3: Performance evaluation and improvement
  * Task 7: Large-scale simulations: Run large-scale simulations in a setting with millions of nodes.
  * Task 8: Evaluation of performance: Investigate the effectiveness of the proposed ex- tensions under heavy presence of malicious nodes and make necessary changes to optimise the performance.
  * Deliverables: New Sybil-resistant service discovery analysis, performance evaluation and final version of the specification. Deadline: after 6 months.
  

## Service Discovery Simulator

Our service discovery simulator for Ethereum 2.0 is based on an existing P2P simulator, [PeerSim P2P Simulator](http://peersim.sourceforge.net/). We extended PeerSim to adapt to the current DHT implementation in Ethereum and add the implementation of our  [service discovery proposition](#service-discovery-proposition), along with other ones for comparison purposes.

### Build

To run the project the following software requirements are necessary:

* Java version 11 or higher
* Maven

On Ubuntu run the following to install them:

`sudo apt install openjdk-14 maven`

Run the following command to build the project:

`mvn package`

### Running
  
`./run.sh <config_file`

All the config files are in `./config/` check this folder for config file descriptions. 

[Proposition1](doc/proposition1.md)

[Service discovery implementation](doc/discovery.md)


## Service Discovery Proposition

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
