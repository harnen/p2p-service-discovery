#!/usr/bin/python3

import pandas as pd
import matplotlib
from matplotlib import pyplot as plt
from matplotlib.lines import Line2D
import sys
import csv
import math

def analyzeMessages(dirs):
    
    print("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    fig, ax1 = plt.subplots()
    fig, ax2 = plt.subplots()
    fig, ax3 = plt.subplots()
    for log_dir in dirs:
        print(log_dir)
        df = pd.read_csv(log_dir + '/messages.csv')
        
        df['dst'].value_counts().plot(ax=ax1, kind='line', xticks=[], title="Message received by node", label=log_dir)
        ax2.bar(df['type'].value_counts().index, df['type'].value_counts(), label=log_dir) 
        df['src'].value_counts().plot(ax=ax3, kind='line', xticks=[], title="Message sent by node", label=log_dir)

    ax1.legend()
    ax2.legend()
    ax3.legend()

def analyzeRegistrations(dirs):
    print("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    fig, ax1 = plt.subplots()
    fig, ax2 = plt.subplots()
    fig, ax3 = plt.subplots()

    for log_dir in dirs:
        print(log_dir)
        df = pd.read_csv(log_dir + '/3500000_registrations.csv')
        print(df['host'].value_counts())
        df['host'].value_counts().plot(ax=ax1, kind='line', xticks=[], title="Registrations by advertisement medium", label=log_dir)
        ax2.bar(df['topic'].value_counts().index, df['topic'].value_counts(), label=log_dir) 
        ax2.set_title("Global registration count by topic")
        df['registrant'].value_counts().plot(ax=ax3, kind='line', xticks=[], title="Registrations by advertiser", label=log_dir)
    ax1.legend()
    ax2.legend()
    ax3.legend()


def analyzeDistribution(dirs):
    fig, ax1 = plt.subplots()
    ax1.tick_params(bottom=False,
                labelbottom=False)
    topics = set()
    
    colors = ['red', 'green', 'blue']
    x = []
    y = []    
    s = []
    c = []
    for log_dir in dirs:
        stats = {}
        print(log_dir)
        dir_num = dirs.index(log_dir)
        with open(log_dir + '/operations.csv', newline='') as csvfile:
            reader = csv.DictReader(csvfile)
            for row in reader:
                if row['type'] == 'LookupOperation' or row['type'] == 'LookupTicketOperation':
                    assert (row['topic'] != '')
                    discovered = row['discovered_list'].split()
                    topic = row['topic']
                    topics.add(topic)
                    for node in discovered:
                        if node not in stats:
                            stats[node] = {}
                        if topic not in stats[node]:
                            stats[node][topic] = 0
                        stats[node][topic] += 1
                        
        
            for node in stats:
                for topic in stats[node]:
                    topic_index = sorted(topics).index(topic)
                    x.append(node)
                    y.append(topic_index + dir_num*0.3)
                    s.append(stats[node][topic])
                    c.append(colors[dir_num])
                    #if(stats[node][topic] > 1):
                    #    print("bigger than 1", s[-1])
    #s = [10] * len(x)
    #print(s)
    scatter = ax1.scatter(x, y, c=c, s=s)
    #ax1.legend(dirs)
    legend_elements = []
    for log_dir in dirs:
        dir_num = dirs.index(log_dir)
        color = colors[dir_num]
        legend_elements.append(Line2D([0], [0], marker='o', color=color, label=log_dir,
                          markerfacecolor=color, markersize=15))
    print(legend_elements)
    ax1.legend(handles=legend_elements)
    print(topics)
    # produce a legend with a cross section of sizes from the scatter
    #handles, labels = scatter.legend_elements(prop="sizes", alpha=0.6)
    #ax1.legend(handles, labels, loc="upper right", title="Sizes")

                    

def analyzeOperations(dirs):
    print("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    fig, ax1 = plt.subplots()
    fig, ax2 = plt.subplots()
    fig, ax3 = plt.subplots()
    
    x = ['RegisterOperation','LookupOperation']
    for log_dir in dirs:
        print(log_dir)
        df = pd.read_csv(log_dir + '/operations.csv')
        print(df)
    
        print(df['type'].value_counts())
        ax1.bar(df['type'].value_counts().index, df['type'].value_counts(), label=log_dir)
        ax1.set_title("Operations by type")
    
        print(df['used_hops'].mean())
        ax2.bar(log_dir, df['used_hops'].mean()) 
        ax2.set_title("Avg recv hop count")
  
        print(df.query("type == 'LookupOperation' or type == 'LookupTicketOperation'")['malicious'].sum())
        print(df.query("type == 'LookupOperation' or type == 'LookupTicketOperation'")['discovered'].sum())
        total_malicious = df.query("type == 'LookupOperation' or type == 'LookupTicketOperation'")['malicious'].sum()
        total_discovered = df.query("type == 'LookupOperation' or type == 'LookupTicketOperation'")['discovered'].sum()
        ax3.bar(["Malicious", "Total_Discovered"], [total_malicious, total_discovered])
        ax3.set_title("Percent Malicious in Lookups")
        
    ax1.legend()
    ax2.legend()
    ax3.legend()

if (len(sys.argv) < 2):
    print("Provide at least one directory with log files (messages.csv and 3500000_registrations.csv")
    exit(1)

def analyzeEclipse(dirs):
    print("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    fig, ax1 = plt.subplots()

    for log_dir in dirs:
        print(log_dir)
        df = pd.read_csv(log_dir + '/eclipse_counts.csv')
        ax1.set_title("Number of eclipsed nodes over time")
        ax1.plot(df['time'], df['numberOfNodes'])

    ax1.legend()

print('Will read logs from', sys.argv[1:])
analyzeMessages(sys.argv[1:])
analyzeRegistrations(sys.argv[1:])
analyzeOperations(sys.argv[1:])
analyzeDistribution(sys.argv[1:])
analyzeEclipse(sys.argv[1:])

plt.show()
