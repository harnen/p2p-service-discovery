# Messages


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
