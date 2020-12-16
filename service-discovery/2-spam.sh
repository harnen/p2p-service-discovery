#!/bin/bash

DEF_SIZE=1000
DEF_TOPIC=5
DEF_ZIPF=0.7
DEF_BUCKET_SIZE=16
DEF_BUCKET_ORDER=1
DEF_TICKET_TABLE_BUCKET_SIZE=16
DEF_SEARCH_TABLE_BUCKET_SIZE=16

DEF_TOPIC_LIMIT=50

#SIZES='1000 5000 10000'
SIZES='500 1000 1500 2000'
TOPICS='1 5 20 60'
ZIPFS='0.1 0.7 1.1'
BUCKET_SIZES='1 3 5 10'
BUCKET_ORDERS='0 1 2'

TOPIC_LIMITS='20 50 100'


IN_CONFIG='config/final.cfg'
OUT_CONFIG='config/tmp.cfg'


function restore_def(){
	SIZE=$DEF_SIZE
	TOPIC=$DEF_TOPIC
	ZIPF=$DEF_ZIPF
	BUCKET_SIZE=$DEF_BUCKET_SIZE
	BUCKET_ORDER=$DEF_BUCKET_ORDER
	TOPIC_LIMIT=$DEF_TOPIC_LIMIT
	TICKET_TABLE_BUCKET_SIZE=$DEF_TICKET_TABLE_BUCKET_SIZE
	SEARCH_TABLE_BUCKET_SIZE=$DEF_SEARCH_TABLE_BUCKET_SIZE
	SPAM_OPT=$DEF_SPAM

	rm -rf logs/*
}

function run_sim(){
	cp $IN_CONFIG $OUT_CONFIG
	sed  -i "s/^SIZE .*$/SIZE $SIZE/g" $OUT_CONFIG 
	sed  -i "s/^control.0traffic.maxtopicnum .*$/control.0traffic.maxtopicnum $TOPIC/g" $OUT_CONFIG
	sed  -i "s/^control.0traffic.zipf .*$/control.0traffic.zipf $ZIPF/g"  $OUT_CONFIG
	sed  -i "s/^protocol.3kademlia.SEARCH_TABLE_BUCKET_SIZE .*$/protocol.3kademlia.SEARCH_TABLE_BUCKET_SIZE $BUCKET_SIZE/g" $OUT_CONFIG
	sed  -i "s/^protocol.3kademlia.LOOKUP_BUCKET_ORDER .*$/protocol.3kademlia.LOOKUP_BUCKET_ORDER $BUCKET_ORDER/g" $OUT_CONFIG
	sed  -i "s/^protocol.3kademlia.ADS_PER_QUEUE .*$/protocol.3kademlia.ADS_PER_QUEUE $TOPIC_LIMIT/g" $OUT_CONFIG
	sed  -i "s/^protocol.3kademlia.TICKET_TABLE_BUCKET_SIZE .*$/protocol.3kademlia.TICKET_TABLE_BUCKET_SIZE $TICKET_TABLE_BUCKET_SIZE/g" $OUT_CONFIG
	sed  -i "s/^protocol.3kademlia.SEARCH_TABLE_BUCKET_SIZE .*$/protocol.3kademlia.SEARCH_TABLE_BUCKET_SIZE $SEARCH_TABLE_BUCKET_SIZE/g" $OUT_CONFIG
	
	./run.sh $OUT_CONFIG &> /dev/null
	mkdir -p ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b${BUCKET_SIZE}_o${BUCKET_ORDER}_q${TOPIC_LIMIT}_ttbs${TICKET_TABLE_BUCKET_SIZE}_stbs${SEARCH_TABLE_BUCKET_SIZE}_spam${SPAM_OPT}
	cp ./logs/*.csv ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b${BUCKET_SIZE}_o${BUCKET_ORDER}_q${TOPIC_LIMIT}_ttbs${TICKET_TABLE_BUCKET_SIZE}_stbs${SEARCH_TABLE_BUCKET_SIZE}_spam${SPAM_OPT}
	cp $OUT_CONFIG ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b${BUCKET_SIZE}_o${BUCKET_ORDER}_q${TOPIC_LIMIT}_ttbs${TICKET_TABLE_BUCKET_SIZE}_stbs${SEARCH_TABLE_BUCKET_SIZE}_spam${SPAM_OPT}

}

function clean(){
	rm logs/*.csv logs/*.cfg
	python3 python/analyze.py logs/*
	mkdir -p $1_results
	mv *.png $1_results
}



restore_def
for SPAM_OPT in $SPAM_OPTIONS
do
	echo running size $SPAM_OPT
	run_sim
	grep '^protocol.3kademlia.TICKET_REMOVE_AFTER_REG' $OUT_CONFIG
done
clean spam

