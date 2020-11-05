#!/usr/bin/python3

import pandas as pd
import matplotlib
from matplotlib import pyplot as plt
from matplotlib.lines import Line2D
from numpy import genfromtxt
import numpy as np
import array
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

    i=0

    for log_dir in dirs:
        print(log_dir)
        df = pd.read_csv(log_dir + '/3500000_registrations.csv')
        print(df['host'].value_counts())
        df['host'].value_counts().plot(ax=ax1, kind='line', xticks=[], title="Registrations by advertisement medium", label=log_dir)
        width=0.3
        margin=width*i
        ax2.bar(np.arange(len(df['topic'].value_counts()))+margin, df['topic'].value_counts(), width=width, label=log_dir)
        ax2.set_title("Global registration count by topic")
        df['registrant'].value_counts().plot(ax=ax3, kind='line', xticks=[], title="Registrations by advertiser", label=log_dir)
        i = i+1

    ticks = df['topic'].value_counts().index
    ax2.set_xticks(range(len(ticks)))
    ax2.set_xticklabels(ticks)
    #ax2.set_xticklabels(df['topic'].value_counts().index)
    ax1.legend()
    ax2.legend()
    ax3.legend()


def analyzeRegistrationsAverage(dirs):
    print("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    fig, ax1 = plt.subplots()
    fig, ax2 = plt.subplots()
#    fig, ax3 = plt.subplots()

    for log_dir in dirs:
        print(log_dir)
        data1 = genfromtxt(log_dir+'/registeredRegistrant.csv',delimiter=',',names=['x', 'y'])
        ax1.plot(sorted(data1['y'],reverse=True))
        data2 = genfromtxt(log_dir+'/registeredRegistrar.csv',delimiter=',',names=['x', 'y'])
        ax2.plot(sorted(data2['y'],reverse=True))
#        data3 = genfromtxt(log_dir+'/registeredTopics.csv',delimiter=',',names=['x', 'y'])
#        ax3.bar(data3['x'],data3['y'],label=log_dir)

#    ax1.xlabel ('Nodes')
#    ax1.ylabel ('#Registrations')
    ax1.set_title('Registrations by advertiser (average)')
    ax2.set_title('Registrations by advertiser medium (average)')
    ax1.legend()
    ax2.legend()

def analyzeRegistrarDistribution(dirs):
    fig, ax1 = plt.subplots()
    ax1.tick_params(bottom=False,
                labelbottom=False)
    topics = set()

    colors = ['red', 'green', 'blue']
    x = []
    y = []
    topics = []
    dir_nums = []
    c = []

    for log_dir in dirs:
        stats = {}
        print(log_dir)
        dir_num = dirs.index(log_dir)
        with open(log_dir + '/900000_registrations.csv', newline='') as csvfile:
            reader = csv.DictReader(csvfile)
            for row in reader:
                node = row['host']
                topic = row['topic']
                topics.append(topic)
                dir_nums.append(dir_num)
                x.append(node)
                c.append(colors[dir_num])

    counter = 0
    topics_set = set(topics)
    for topic in topics:
        topic_index = sorted(topics_set).index(topic)
        dir_offset = dir_nums[counter]*0.3
        y.append(topic_index + dir_offset)
        counter += 1

    scatter = ax1.scatter(x, y, c=c)
    legend_elements = []
    for log_dir in dirs:
        dir_num = dirs.index(log_dir)
        color = colors[dir_num]
        legend_elements.append(Line2D([0], [0], marker='o', color=color, label=log_dir,
                          markerfacecolor=color, markersize=15))
    print(legend_elements)
    ax1.legend(handles=legend_elements)
    ax1.set_title("Registrars")




def analyzeRegistrantDistribution(dirs):
    fig, ax1 = plt.subplots()
    ax1.tick_params(bottom=False,
                labelbottom=False)
    topics = set()

    colors = ['red', 'green', 'blue']
    x = []
    y = []
    s = []
    c = []

    x_nondiscovered = []
    y_nondiscovered = []
    s_nondiscovered = []
    c_nondiscovered = []

    for log_dir in dirs:
        stats = {}
        print(log_dir)
        dir_num = dirs.index(log_dir)
        with open(log_dir + '/operations.csv', newline='') as csvfile:
            reader = csv.DictReader(csvfile)
            for row in reader:
                if row['type'] == 'LookupOperation' or row['type'] == 'LookupTicketOperation' or row['type'] == 'RegisterOperation':
                    assert (row['topic'] != '')
                    if(row['type'] == 'RegisterOperation'):
                        discovered = []
                        discovered.append(row['src'])
                    else:
                        #continue
                        discovered = row['discovered_list'].split()
                    topic = row['topic']
                    topics.add(topic)
                    for node in discovered:
                        if node not in stats:
                            stats[node] = {}
                        if topic not in stats[node]:
                            stats[node][topic] = 0
                        if row['type'] != 'RegisterOperation':
                            stats[node][topic] += 1


            for node in stats:
                for topic in stats[node]:
                    topic_index = sorted(topics).index(topic)
                    if(stats[node][topic] == 0):
                        x_nondiscovered.append(node)
                        y_nondiscovered.append(topic_index + 0.15 + dir_num*0.3)
                        c_nondiscovered.append(colors[dir_num])
                        s_nondiscovered.append(1000)
                    else:
                        x.append(node)
                        y.append(topic_index + dir_num*0.3)
                        c.append(colors[dir_num])
                        s.append(stats[node][topic])
    ax1.scatter(x, y, c=c, s=s)
    scatter = ax1.scatter(x_nondiscovered, y_nondiscovered, c=c_nondiscovered, s=s_nondiscovered, marker = 'x')
    legend_elements = []
    for log_dir in dirs:
        dir_num = dirs.index(log_dir)
        color = colors[dir_num]
        legend_elements.append(Line2D([0], [0], marker='o', color=color, label=log_dir,
                          markerfacecolor=color, markersize=15))
    print(legend_elements)
    ax1.legend(handles=legend_elements)
    ax1.set_title("Discovered Registrants")



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

def analyzeEclipsedNodesOverTime(dirs):
    print("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    fig, ax1 = plt.subplots()

    for log_dir in dirs:
        print(log_dir)
        df = pd.read_csv(log_dir + '/eclipse_counts.csv')
        ax1.set_title("Number of eclipsed nodes over time")
        ax1.plot(df['time'], df['numberOfNodes'])

    ax1.legend()

def analyzeEclipsedNodeDistribution(dirs):

    fig, ax1 = plt.subplots()
    ax1.tick_params(bottom=False,
                labelbottom=False)
    colors = ['red', 'green', 'blue', 'black']
    markers = ["o" , "v" , "^" , "<", ">"]
    topics = set()
    for log_dir in dirs:
        x = []
        y = []
        m = []
        c = []
        # get only the columns you want from the csv file
        df1 = pd.read_csv(log_dir + '/node_information.csv', usecols=['nodeID', 'topicID'])
        node_to_topicID = df1.set_index('nodeID')['topicID'].to_dict()
        df2 = pd.read_csv(log_dir + '/node_information.csv', usecols=['nodeID', 'is_evil?'])
        is_evil_node = df2.set_index('nodeID')['is_evil?'].to_dict()

        uneclipsed_nodes_set = None
        eclipsed_nodes_set = None
        evil_nodes_set = None
        with open(log_dir + '/eclipse_counts.csv', newline='') as csvfile:
            reader = csv.DictReader(csvfile)

            for row in reader:
                if row['time'] != '3500000':
                    continue

                eclipsed_nodes = row['eclipsedNodes'].split()
                eclipsed_nodes_set = set(eclipsed_nodes)
                uneclipsed_nodes = row['UnEclipsedNodes'].split()
                uneclipsed_nodes_set = set(uneclipsed_nodes)
                evil_nodes = row['EvilNodes'].split()
                evil_nodes_set = set(evil_nodes)

        for topic in node_to_topicID.values():
            topics.add(topic)

        IDs = []
        id_to_short = {}
        for node in is_evil_node.keys():
            IDs.append(node)
        for topic in topics:
            IDs.append(topic)

        IDs = sorted(IDs)
        short_num = 0
        for identifier in IDs:
            id_to_short[identifier] = short_num
            short_num += 1

        x,y = [],[]
        for topic in topics:
            x.append(id_to_short[topic])
            y.append(id_to_short[topic])

        ax1.scatter(x,y, s=100,marker='x',color='black',linewidths=4)

        x,y = [],[]
        for node in eclipsed_nodes_set:
            x.append(id_to_short[node_to_topicID[node]])
            y.append(id_to_short[node])

        ax1.scatter(x,y, s=1,marker='o',color='red',linewidths=1)

        x,y = [],[]
        for node in uneclipsed_nodes_set:
            x.append(id_to_short[node_to_topicID[node]])
            y.append(id_to_short[node])

        ax1.scatter(x,y, s=1,marker='v',color='green',linewidths=1)

        x,y = [],[]
        for node in evil_nodes_set:
            x.append(id_to_short[node_to_topicID[node]])
            y.append(id_to_short[node])
        ax1.scatter(x,y, s=1,marker='<',color='blue',linewidths=1)

    ax1.set_yticks([])
    ax1.scatter(x, y, color=c, marker='o')
    legend_elements = []
    legend_elements = [Line2D([0], [0], marker='o',  color='red', label='Eclipsed Node'),
                   Line2D([0], [0], marker='v', color='green', label='Non-Eclipsed Node'),
                   Line2D([0], [0], marker='<', color='blue', label='Malicious Node'),
                   Line2D([0], [0], marker='x', color='black', label='Topic ID')]

    ax1.legend(handles=legend_elements, loc='upper center')

print('Will read logs from', sys.argv[1:])
#analyzeMessages(sys.argv[1:])
analyzeRegistrations(sys.argv[1:])
#analyzeRegistrationsAverage(sys.argv[1:])
#analyzeOperations(sys.argv[1:])
#analyzeRegistrantDistribution(sys.argv[1:])
#analyzeRegistrarDistribution(sys.argv[1:])
#analyzeEclipsedNodesOverTime(sys.argv[1:])
#analyzeEclipsedNodeDistribution(sys.argv[1:])

plt.show()
