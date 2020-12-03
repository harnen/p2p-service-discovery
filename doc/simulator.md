## Service Discovery Simulator

Our service discovery simulator for Ethereum 2.0, included in this repository, is based on an existing P2P simulator, [PeerSim P2P Simulator](http://peersim.sourceforge.net/). We extended PeerSim to adapt to the current Ethereum DHT implementation and we added the implementation of our  [service discovery proposition](#service-discovery-proposition), along with other ones for comparison purposes.

### Build

To run the project the following software requirements are necessary:

* Java version 11 or higher
* Maven

On Ubuntu run the following to install them:

```shell
$ sudo apt install openjdk-14 maven
```

Run the following command to build the project:

```shell
$ git clone https://github.com/harnen/p2p-service-discovery.git
$ cd service-discovery
$ mvn package
```

### Running
  
```shell
$ ./run.sh <config_file>
```

All the config files are in `./config/` check this folder for config file descriptions. 

### Configuration files

We provide a set of configuration files in order to simulate different setups, placed in the `./config/` folder

* simple.cfg: It performs a single Kademlia lookup with no topic registration or service discovery.

* discv5ticket.cfg: This configuration sets a scenario where a set of nodes are performing topic registration and discovery using discv5 tickets mechanism described here.

* discv5proposal.cfg: This configuration sets a scenario where a set of nodes are performing topic registration and discovery using our [service discovery proposition](#service-discovery-proposition) for discv5.

* ethclient.cfg: This configuration set ups a scenario where nodes behave as Ethereum clients, having a list of active connections and performing lookups to fill up the DHT table when a slot is empty in the list of connections. No discv5 is simulated in this configuration yet.

* ethclientdns.cfg: Same than previous one but loading nodes from 
[Eth crawling results](https://github.com/ethereum/discv4-dns-lists)

### Gathering statistics

TBC
