#!/bin/bash
STOP_REGISTER_WINDOW_SIZES='0 3 16'
STOP_REGISTER_MIN_REGS='0 3 16'
ADS_PER_QUEUES='17 50'

IN_CONFIG='config/fix_overload.cfg'
OUT_CONFIG='config/tmp.cfg'

function run_sim(){
	cp $IN_CONFIG $OUT_CONFIG
    	dos2unix $OUT_CONFIG &> /dev/null
	sed  -i "s/^protocol.3kademlia.ADS_PER_QUEUE .*$/protocol.3kademlia.ADS_PER_QUEUE $ADS_PER_QUEUE/g" $OUT_CONFIG
	sed  -i "s/^protocol.3kademlia.STOP_REGISTER_MIN_REGS .*$/protocol.3kademlia.STOP_REGISTER_MIN_REGS $STOP_REGISTER_MIN_REG/g" $OUT_CONFIG 
	sed  -i "s/^protocol.3kademlia.STOP_REGISTER_WINDOW_SIZE .*$/protocol.3kademlia.STOP_REGISTER_WINDOW_SIZE $STOP_REGISTER_WINDOW_SIZE/g" $OUT_CONFIG  
	
	./run.sh $OUT_CONFIG &> /dev/null
	mkdir -p ./logs/a${ADS_PER_QUEUE}_w${STOP_REGISTER_WINDOW_SIZE}_m${STOP_REGISTER_MIN_REG}
	cp ./logs/*.csv ./logs/a${ADS_PER_QUEUE}_w${STOP_REGISTER_WINDOW_SIZE}_m${STOP_REGISTER_MIN_REG}
	cp $OUT_CONFIG ./logs/a${ADS_PER_QUEUE}_w${STOP_REGISTER_WINDOW_SIZE}_m${STOP_REGISTER_MIN_REG}
}

function clean(){
	rm logs/*.csv logs/*.cfg
	python3 python/analyze.py logs/*
	mkdir -p $1_results
	mv *.png $1_results
}



rm -rf logs/*
for STOP_REGISTER_WINDOW_SIZE in $STOP_REGISTER_WINDOW_SIZES
do
	for STOP_REGISTER_MIN_REG in $STOP_REGISTER_MIN_REGS
	do
		for ADS_PER_QUEUE in $ADS_PER_QUEUES
		do
			echo running WINDOW_SIZE $STOP_REGISTER_WINDOW_SIZE MIN_REG $STOP_REGISTER_MIN_REG ADS_PER_QUEUE $ADS_PER_QUEUE
			run_sim
		done
	done
done
clean STOP_REGISTER
