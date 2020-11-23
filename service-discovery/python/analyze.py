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

csv.field_size_limit(sys.maxsize)


def analyzeMessages(dirs):

    print("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    fig, ax1 = plt.subplots()
    fig, ax2 = plt.subplots()
    fig, ax3 = plt.subplots()
    i=0

    for log_dir in dirs:
        print(log_dir)
        df = pd.read_csv(log_dir + '/messages.csv')
        #print(df)
        df['dst'].value_counts().plot(ax=ax1, kind='line', xticks=[], title="Message received by node", label=log_dir)
        width=0.3
        margin=width*i
        #print(df['type'].value_counts().index)
        if not (df.type == 'MSG_FIND').any():
            new_row = { 'id':0, 'type':'MSG_FIND', 'src':0, 'dst':0, 'topic':'NaN', 'sent/received':'NaN'}
            df = df.append(new_row,ignore_index=True)
        if not (df.type == 'MSG_TICKET_REQUEST').any():
            new_row = { 'id':0, 'type':'MSG_TICKET_REQUEST', 'src':0, 'dst':0, 'topic':'NaN', 'sent/received':'NaN'}
            df = df.append(new_row,ignore_index=True)
        if not (df.type == 'MSG_RESPONSE').any():
            new_row = { 'id':0, 'type':'MSG_RESPONSE', 'src':0, 'dst':0, 'topic':'NaN', 'sent/received':'NaN'}
            df = df.append(new_row,ignore_index=True)
        if not (df.type == 'MSG_TICKET_RESPONSE').any():
            new_row = { 'id':0, 'type':'MSG_TICKET_RESPONSE', 'src':0, 'dst':0, 'topic':'NaN', 'sent/received':'NaN'}
            df = df.append(new_row,ignore_index=True)

        table = df['type'].value_counts().sort_index()
#        sorted_table = table.sort_values(by='type',ascending=False)
#        sorted_table = table.sort_values(by='count',ascending=False)
        print(table)
        ax2.bar(np.arange(len(table.index))+margin,table.values,width=width, label=log_dir)
        #ax2.bar(np.arange(len(df['type'].value_counts(sort=True)))+margin, df['type'].value_counts(sort=True), width=width, label=log_dir)
        #ax2.bar(np.arange(len(df['topic'].value_counts()))+margin, df['topic'].value_counts(), width=width, label=log_dir)
        #ax2.set_title("Global registration count by topic")
        i = i+1
        #ax2.bar(df['type'].value_counts().index, df['type'].value_counts(), label=log_dir)
        df['src'].value_counts().plot(ax=ax3, kind='line', xticks=[], title="Message sent by node", label=log_dir)

    #df = pd.read_csv('logs/ticket/messages.csv')
    ticks = table.index
    #print(ticks)
    #print(len(ticks))
    ax2.set_xticks(range(len(ticks)))
    ax2.set_xticklabels(ticks)
    ax1.legend()
    ax2.legend()
    ax3.legend()

def analyzeActiveRegistrations(dirs):
    """ Plot a bar chart showing the number of registrations by malicious and good nodes.
    """

    topics = []
    evil_registration_count_per_topic = {}
    normal_registration_count_per_topic = {}
    for log_dir in dirs:
        evil_registration_count_per_topic = {}
        normal_registration_count_per_topic = {}
        with open(log_dir + '/registration_stats.csv', newline='') as csvfile:
            reader = csv.DictReader(csvfile)
            ncol = len(next(reader)) # Read first line and count columns
            numOfTopics = int((ncol-1)/2)
            print('Number of topics: ', numOfTopics)
            topics = ['t'+str(x) for x in range(1, numOfTopics+1)]
            for topic in topics:
                normal_registration_count_per_topic[topic] = 0
                evil_registration_count_per_topic[topic] = 0
        with open(log_dir + '/registration_stats.csv', newline='') as csvfile:
            reader = csv.DictReader(csvfile)
            nrows = 0
            for row in reader:
                for topic in topics:
                    normal = int(row[topic + '-normal'])
                    evil = int(row[topic + '-evil'])
                    normal_registration_count_per_topic[topic] += normal
                    evil_registration_count_per_topic[topic] += evil
                nrows += 1
        normal_counts = [normal_registration_count_per_topic[topic]/nrows for topic in topics]
        evil_counts = [evil_registration_count_per_topic[topic]/nrows for topic in topics]
        width = 0.35  # the width of the bars

        fig, ax = plt.subplots()
        x_values = [x-width/2 for x in range(1, numOfTopics+1)]
        rects1 = ax.bar(x_values, normal_counts, width,
                label='Good registrations')
        x_values = [x+width/2 for x in range(1, numOfTopics+1)]
        rects2 = ax.bar(x_values, evil_counts, width,
                label='Malicious registrations')

        # Add some text for labels, title and custom x-axis tick labels, etc.
        ax.set_ylabel('Number of registrations')
        ax.set_title('Active Registrations ' + log_dir)
        ax.set_xticks(x_values)
        ax.set_xticklabels(topics)
        ax.legend()

        plt.savefig('./plots/registration_origin.png')

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
    fig, ax3 = plt.subplots()

    i=0
    for log_dir in dirs:
        print(log_dir)
        data1 = genfromtxt(log_dir+'/registeredRegistrant.csv',delimiter=',',names=['x', 'y'])
        ax1.plot(sorted(data1['y'],reverse=True),label=log_dir)
        data2 = genfromtxt(log_dir+'/registeredRegistrar.csv',delimiter=',',names=['x', 'y'])
        ax2.plot(sorted(data2['y'],reverse=True),label=log_dir)
        width=0.3
        margin=width*i
        table = pd.read_csv(log_dir + '/registeredTopics.csv')
        sorted_table = table.sort_values(by='count',ascending=False)
        ax3.bar(np.arange(len(sorted_table['count'].values))+margin,sorted_table['count'].values,width=width, label=log_dir)
        i=i+1

    ticks = sorted_table['topic'].values
    ax3.set_xticks(range(len(ticks)))
    ax3.set_xticklabels(ticks)

    ax1.set_title('Registrations by advertiser (average)')
    ax2.set_title('Registrations by advertiser medium (average)')
    ax3.set_title('Registrations by topics (average)')
    ax1.legend()
    ax2.legend()
    ax3.legend()

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
    x_val = 1
    x_vals = []
    total_malicious_list = []
    total_discovered_list = []
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
        total_malicious_list.append(total_malicious)
        total_discovered_list.append(total_discovered)
        x_vals.append(x_val)
        x_val += 3

    print('x_vals: ', x_vals)
    width = 1.0  # the width of the bars
    x_values = [x-width/2 for x in x_vals]
    ax3.bar(x_values, total_malicious_list, width, label='Malicious')
    x_values = [x+width/2 for x in x_vals]
    ax3.bar(x_values, total_discovered_list, width, label='All')
    ax3.set_title("Malicious nodes out of all lookup results")
    ax3.set_xticks(x_vals)
    ax3.set_xticklabels(dirs)

    ax1.legend()
    ax2.legend()
    ax3.legend()

def analyzeEclipsedNodesOverTime(dirs):
    print("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    fig, ax1 = plt.subplots()
    colors = ['red', 'green', 'blue']

    for log_dir in dirs:
        print(log_dir)
        df = pd.read_csv(log_dir + '/eclipse_counts.csv')
        ax1.set_title("Number of eclipsed nodes over time")
        ax1.plot(df['time'], df['numberOfNodes'], label=log_dir)

    ax1.legend()
    plt.savefig('./plots/eclipsed_node_over_time.png')

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
            y.append(id_to_short[topic])
            x.append(id_to_short[topic])

        ax1.scatter(x,y, s=100,marker='|',color='black',linewidths=1)

        x,y = [],[]
        for node in eclipsed_nodes_set:
            y.append(id_to_short[node_to_topicID[node]])
            x.append(id_to_short[node])

        ax1.scatter(x,y, s=10,marker='|',color='red',linewidths=1)

        x,y = [],[]
        for node in uneclipsed_nodes_set:
            y.append(id_to_short[node_to_topicID[node]])
            x.append(id_to_short[node])

        ax1.scatter(x,y, s=10,marker='|',color='green',linewidths=1)

        x,y = [],[]
        for node in evil_nodes_set:
            y.append(id_to_short[node_to_topicID[node]])
            x.append(id_to_short[node])
        ax1.scatter(x,y, s=10,marker='|',color='orange',linewidths=1)

    #ax1.set_yticks([])
    topics_list = [id_to_short[x] for x in topics]
    tick_labels = ['topic'+str(x) for x in range(1, len(topics_list)+1)]
    print('Tick labels: ', tick_labels)
    print('topics_list: ', topics_list)
    ax1.set_yticks(sorted(topics_list))
    ax1.set_yticklabels(tick_labels)
    ax1.scatter(x, y, color=c, marker='o')
    legend_elements = []
    legend_elements = [Line2D([0], [0], marker='|',  color='red', label='Eclipsed Node'),
                    Line2D([0], [0], marker='|', color='green', label='Non-Eclipsed Node'),
                    Line2D([0], [0], marker='|', color='orange', label='Malicious Node'),
                    Line2D([0], [0], marker='|', color='black', label='Topic ID')]

    ax1.legend(handles=legend_elements, bbox_to_anchor=(0,1.02,1,0.2), loc="lower left",
                mode="expand", borderaxespad=0, ncol=4)
    #ax1.legend(handles=legend_elements, loc='upper center')
    ax1.set_ylabel('Topics')
    ax1.set_xlabel('Node status')

    plt.savefig('./plots/node_type_dist.png')

def analyzeRegistrationTime(dirs):
    print("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    fig, ax1 = plt.subplots()
    fig, ax2 = plt.subplots()
    fig, ax3 = plt.subplots()
    fig, ax4 = plt.subplots()
    fig, ax5 = plt.subplots()

    for log_dir in dirs:
        print(log_dir)
        data1 = genfromtxt(log_dir+'/registeredTopicsTime.csv',delimiter=',',names=['topic','registrant', 'times','regmintime','regavgtime','discmintime','discavgtime'])
        ax1.plot(sorted(data1['times'],reverse=True),label=log_dir)
        ax2.plot(sorted(data1['regmintime'],reverse=True),label=log_dir)
        ax3.plot(sorted(data1['regavgtime'],reverse=True),label=log_dir)
        ax4.plot(sorted(data1['discmintime'],reverse=True),label=log_dir)
        ax5.plot(sorted(data1['discavgtime'],reverse=True),label=log_dir)


    ax1.set_title('Total registrations by registrant')
    ax2.set_title('Minimum time to register')
    ax3.set_title('Average time to register')
    ax4.set_title('Average time to discovery')
    ax5.set_title('Minimum time to discovery')
    ax1.legend()
    ax2.legend()
    ax3.legend()
    ax4.legend()
    ax5.legend()

if (len(sys.argv) < 2):
    print("Provide at least one directory with log files (messages.csv and 3500000_registrations.csv")
    exit(1)

print('Will read logs from', sys.argv[1:])
analyzeMessages(sys.argv[1:])
#analyzeRegistrations(sys.argv[1:])
analyzeRegistrationsAverage(sys.argv[1:])
analyzeOperations(sys.argv[1:])
analyzeRegistrantDistribution(sys.argv[1:])
analyzeRegistrarDistribution(sys.argv[1:])
analyzeEclipsedNodesOverTime(sys.argv[1:])
analyzeEclipsedNodeDistribution(sys.argv[1:])
analyzeActiveRegistrations(sys.argv[1:])
analyzeRegistrationTime(sys.argv[1:])
plt.show()
