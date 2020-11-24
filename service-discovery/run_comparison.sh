#!/bin/bash

mkdir -p ./logs/ticket_search/
echo "Running proposal"
./run.sh ./config/discv5ticket_search.cfg &> /dev/null
cp ./logs/*.csv ./logs/ticket_search/

mkdir -p ./logs/ticket/
echo "Running ticket"
./run.sh ./config/discv5ticket.cfg &> /dev/null
cp ./logs/*.csv ./logs/ticket/

python3 ./python/analyze.py ./logs/ticket_search/ ./logs/ticket/
