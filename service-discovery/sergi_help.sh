#!/bin/bash

DEF_SIZE=1000
DEF_TOPIC=4
DEF_ZIPF=1
DEF_BUCKET_SIZE=17
DEF_BUCKET_ORDER=0
DEF_TOPIC_LIMIT=17

#SIZES='1000 5000 10000'
SIZES='1000 1500 2000'
TOPICS='1 4 20 60'
ZIPFS='0.1 0.5 1 1.5'
#ZIPFS=''
BUCKET_SIZES='1 3 5 10'
BUCKET_ORDERS='0 1 2'
TOPIC_LIMITS='17 50 1000'

IN_CONFIG='config/final.cfg'
OUT_CONFIG='config/tmp.cfg'


SIZE=$DEF_SIZE
TOPIC=$DEF_TOPIC
ZIPF=$DEF_ZIPF
BUCKET_SIZE=$DEF_BUCKET_SIZE
BUCKET_ORDER=$DEF_BUCKET_ORDER
TOPIC_LIMIT=$DEF_TOPIC_LIMIT
rm -rf logs/*
for TOPIC_LIMIT in $TOPIC_LIMITS
do
	echo Running topic per queue limit: $TOPIC_LIMIT
	cp $IN_CONFIG $OUT_CONFIG
	sed  -i "s/^SIZE .*$/SIZE $SIZE/g" $OUT_CONFIG 
	sed  -i "s/^control.0traffic.maxtopicnum .*$/control.0traffic.maxtopicnum $TOPIC/g" $OUT_CONFIG
	sed  -i "s/^control.0traffic.zipf .*$/control.0traffic.zipf $ZIPF/g"  $OUT_CONFIG
	sed  -i "s/^protocol.3kademlia.SEARCH_TABLE_BUCKET_SIZE .*$/protocol.3kademlia.SEARCH_TABLE_BUCKET_SIZE $BUCKET_SIZE/g" $OUT_CONFIG
	sed  -i "s/^protocol.3kademlia.LOOKUP_BUCKET_ORDER .*$/protocol.3kademlia.LOOKUP_BUCKET_ORDER $BUCKET_ORDER/g" $OUT_CONFIG
	sed  -i "s/^protocol.3kademlia.ADS_PER_QUEUE .*$/protocol.3kademlia.ADS_PER_QUEUE $TOPIC_LIMIT/g" $OUT_CONFIG
	
	./run.sh $OUT_CONFIG &> /dev/null
	mkdir -p ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b${BUCKET_SIZE}_o${BUCKET_ORDER}_q${TOPIC_LIMIT}
	cp ./logs/*.csv ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b${BUCKET_SIZE}_o${BUCKET_ORDER}_q${TOPIC_LIMIT}
	cp $OUT_CONFIG ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b${BUCKET_SIZE}_o${BUCKET_ORDER}_q${TOPIC_LIMIT}
	grep '^protocol.3kademlia.ADS_PER_QUEUE' $OUT_CONFIG
done
rm logs/*.csv logs/*.cfg
python3 python/analyze.py logs/*
mkdir -p queue_size_results
mv *.png queue_size_results

