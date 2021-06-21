 # Parameters evaluated

* Topic table capacity 50, 100, 500, and 1000. The rest of the parameters take their default values given in the [setup](../report.md#Setup-(default-parameters))

* Value selected: table capacity = 500.

# Results

## Registration
* Active registrations
<p align="center"><img src="../imgs/topic_table/capacity/registration_origin.png" width="60%" /></p>

* Average time to register
<p align="center"><img src="../imgs/topic_table/capacity/avg_time_register.png" width="60%" /></p>

## Load

* Message quantity
<p align="center"><img src="../imgs/topic_table/capacity/message_quantity.png" width="60%" /></p>

* Message distribution
<p align="center"><img src="../imgs/topic_table/capacity/messages_received2.png" width="60%" /></p>

## Discovery

* Registrant discovery distribution
<p align="center"><img src="../imgs/topic_table/capacity/registrant_distribution.png" width="60%" /></p>

* Time between registration to first discovery
<p align="center"><img src="../imgs/topic_table/capacity/min_time_discovery.png" width="60%" /></p>

* Lookup hopcount
<p align="center"><img src="../imgs/topic_table/capacity/lookup_hopcount.png" width="60%" /></p>


## Table occupancy
<p align="center"><img src="../imgs/topic_table/capacity/storage_utilisation.png" width="60%" /></p>


# Conclusions

* We selected 500 registrations as a capacity of the topic table, since it appears to have the best balance between number of registrations and traffic generated.



