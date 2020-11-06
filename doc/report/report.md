# Report without attacks

## Setup
The report consist of a comparison between simulations using the following configuration files:
* `./config/discv5ticket.cfg` - `./logs/ticket` on the graphs
* `./config/discv5noticket.cfg` - `./logs/noticket` on the graphs

Both networks consists of 1000 nodes and no turbulance/churn. 



![](./report/img/Figure_1.png)
![](./report/img/Figure_3.png)

The graph above shows the number of messages received by nodes in the network. 
The ticket protocol sends advertisement and lookups to a higher number of nodes and expieriences high number of received messages.
In contract, the noticket protocol sends advertisement and lookups to a small portion of the nodes (close to the topic hash) and produces lower overhead.

The same applies for the sent messages 


We continue by analyzing the number of registrations present on regisrars. 
![](./report/img/Figure_4.png)

The noticket proposal results in less equal load on registrars. Nodes closer to hashes of popular topic receive more traffic than the rest. 
This effect is mitigated in the ticket protocol as the registration are performed at uniformly distributed nodes. However, the ticket protocol 

