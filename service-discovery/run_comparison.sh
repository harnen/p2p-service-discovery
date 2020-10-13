#!/bin/bash

mkdir -p ./logs/noticket/
echo "Running proposal"
./run.sh ./config/discv5noticket.cfg &> /dev/null
cp ./logs/*.csv ./logs/noticket/

mkdir -p ./logs/ticket/
echo "Running ticket"
./run.sh ./config/discv5ticket.cfg &> /dev/null
cp ./logs/*.csv ./logs/ticket/

./python/analyze.py ./logs/noticket/ ./logs/ticket/
