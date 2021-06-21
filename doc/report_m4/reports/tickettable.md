
# Parameters evaluated

* Ticket table bucket size: 3, 5, 10, 16.
* Ticket table bucket number: 5,10,17,256.

* Value selected: 5 bucket size, 10 buckets.

# Results

## Active registrations
<p align="center">
  <img src="../imgs/ticket_table/bucket_size/registration_origin.png" width="35%" />
  <img src="../imgs/ticket_table/nbucket/registration_origin.png" width="35%" />
</p>

## Traffic load

* Message quantity
<p align="center">
  <img src="../imgs/ticket_table/bucket_size/message_quantity.png" width="35%" />
  <img src="../imgs/ticket_table/nbucket/message_quantity.png" width="35%" />
</p>

* Message distribution

<p align="center">
  <img src="../imgs/ticket_table/bucket_size/messages_received2.png" width="35%" />
  <img src="../imgs/ticket_table/nbucket/messages_received2.png" width="35%" />
</p>


## Discovery

* Registrant discovery distribution

<p align="center">
  <img src="../imgs/ticket_table/bucket_size/registrant_distribution.png" width="35%" />
  <img src="../imgs/ticket_table/nbucket/registrant_distribution.png" width="35%" />
</p>

* Time between registration to first discovery

<p align="center">
  <img src="../imgs/ticket_table/bucket_size/min_time_discovery.png" width="35%" />
  <img src="../imgs/ticket_table/nbucket/min_time_discovery.png" width="35%" />
</p>

* Lookup hopcount

<p align="center">
  <img src="../imgs/ticket_table/bucket_size/lookup_hopcount.png" width="35%" />
  <img src="../imgs/ticket_table/nbucket/lookup_hopcount.png" width="35%" />
</p>

## Table occupancy

<p align="center">
  <img src="../imgs/ticket_table/bucket_size/storage_utilisation.png" width="35%" />
  <img src="../imgs/ticket_table/nbucket/storage_utilisation.png" width="35%" />
</p>


# Conclusions

* We selected bucket size 5 and 10 buckets as configuration parameters.
* As we increase bucket size seems to increase the number of registrations but also traffic load in the same way. The lookup hopcount performance does not seem to improve in the same way, so we choose 5 as a bucket size to control the traffic load at nodes close to topic hash id. 
* For the bucket number 
