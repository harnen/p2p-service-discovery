# Waiting time parameter selection


The waiting time formula:
* **n** - capacity of the table
* **d** - number of entries currently in the table
* **d(IP)** - number of entries currently in the table with given IP
* **d(ID)** - number of entries currently in the table with given ID
* **d(topic)** - number of entries currently in the table with given topic
* **a** - registration lifetime (time spent in the table)
* **w** - waiting time
<img src="https://render.githubusercontent.com/render/math?math=\Large w=sum((\frac{1}{10^9}),(\frac{d(IP)}{d})^\textit{ip\_power},(\frac{d(ID)}{d})^\textit{id\_power},(\frac{d(topic)}{d})^\textit{topic\_power}) \frac{a*\text{base\_multiplier}}{(1-\frac{n}{d})^\textit{occupancy\_power}}">

We thus have 5 parameters to fix:
* **ip_power**
* **id_power**
* **topic_power**
* **occupancy_power**
* **base_multiplier**

We perform all the simulations with 50 honest nodes, 250 malicious honest having 10 IPs/IDs (10% of honest ones). We present results for three different table sizes: 50 (< #registrants), 400 (~= #registrants), 15000 (>> #registrants). 

## base_multiplier
We start by investigatnig base_multiplier. Lower values means lower waiting times. However, the the value must be high enough to efficiently prevent the malicious traffic from dominating the table. 

<p align="center"><img src="../imgs/cap50/base_multiplier.png" width="60%" /></p>
<p align="center"><img src="../imgs/cap400/base_multiplier.png" width="60%" /></p>
<p align="center"><img src="../imgs/cap15000/base_multiplier.png" width="60%" /></p>

## occupancy_power

<p align="center"><img src="../imgs/cap50/occupancy_power.png" width="60%" /></p>
<p align="center"><img src="../imgs/cap400/occupancy_power.png" width="60%" /></p>
<p align="center"><img src="../imgs/cap15000/occupancy_power.png" width="60%" /></p>

## id_ip_power

<p align="center"><img src="../imgs/cap50/id_ip_power.png" width="60%" /></p>
<p align="center"><img src="../imgs/cap400/id_ip_power.png" width="60%" /></p>
<p align="center"><img src="../imgs/cap15000/id_ip_power.png" width="60%" /></p>

## topic_power

<p align="center"><img src="../imgs/cap50/topic_power.png" width="60%" /></p>
<p align="center"><img src="../imgs/cap400/topic_power.png" width="60%" /></p>
<p align="center"><img src="../imgs/cap15000/topic_power.png" width="60%" /></p>
