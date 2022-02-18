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
import os

csv.field_size_limit(sys.maxsize)

font = {'family' : 'normal',
        'weight' : 'bold',
        'size'   : 16}

matplotlib.rc('font', **font)
matplotlib.rcParams['pdf.fonttype'] = 42
matplotlib.rcParams['ps.fonttype'] = 42

def plot_clustered_stacked(dfall, labels=None, title="",  H="/", **kwargs):
    """Given a list of dataframes, with identical columns and index, create a clustered stacked bar plot.                                                                                     
labels is a list of the names of the dataframe, used for the legend                            
title is a string for the title of the plot 
H is the hatch used for identification of the different dataframe"""                           
    
    n_df = len(dfall)                                                                          
    n_col = len(dfall[0].columns) 
    n_ind = len(dfall[0].index)                                                                
    axe = plt.subplot(111)                                                                     
        
    for df in dfall : # for each data frame                                                    
        axe = df.plot(kind="bar",
                      linewidth=0,                                                             
                      stacked=True,
                      ax=axe,                                                                  
                      legend=False,                                                            
                      grid=False,                                                              
                      **kwargs)  # make bar plots                                              

    h,l = axe.get_legend_handles_labels() # get the handles we want to modify                  
    for i in range(0, n_df * n_col, n_col): # len(h) = n_col * n_df                            
        for j, pa in enumerate(h[i:i+n_col]):                                                  
            for rect in pa.patches: # for each index                                           
                rect.set_x(rect.get_x() + 1 / float(n_df + 1) * i / float(n_col))              
                rect.set_hatch(H * int(i / n_col)) #edited part                                
                rect.set_width(1 / float(n_df + 1))                                            
                   
    axe.set_xticks((np.arange(0, 2 * n_ind, 2) + 1 / float(n_df + 1)) / 2.)                    
    axe.set_xticklabels(df.index, rotation = 0)                                                
    axe.set_title(title)

    # Add invisible data to add another legend
    n=[]        
    for i in range(n_df):
        n.append(axe.bar(0, 0, color="gray", hatch=H * i))

    l1 = axe.legend(h[:n_col], l[:n_col], loc=[1.01, 0.5])
    if labels is not None:
        l2 = plt.legend(n, labels, loc=[1.01, 0.1])
    axe.add_artist(l1)
    return axe

# plot per-topic, average waiting times and number
def analyzeWaitingTimes(dirs, x_vals, x_label, plot_labels):

    dirs = sorted(dirs)
    colors = ['r', 'g', 'b', 'w', 'e', 'a', 'o']
    
    fig, ax = plt.subplots(figsize=(10, 4))
    label_indx = 0
    for log_dir in dirs:
        sub_dirs = next(os.walk(log_dir))[1]
        sub_dirs = sorted(sub_dirs)
        min_vals = {}
        max_vals = {}
        average_vals = []
        waiting_times = {}
        for subdir in sub_dirs:
            path = log_dir + '/' + subdir + '/'
            path.replace('//','/')
            print ('Reading from directory: ', path)
            try:
                with open(path + 'waiting_times.csv', newline='') as csvfile:
                    reader = csv.DictReader(csvfile)
                    # Read first line with column names
                    ncols = len(next(reader)) 
                    numOfTopics = int((ncols-1)/6)
            except FileNotFoundError:
                print("Path: ", path, " does not contain waiting_times.csv ")
                continue

            column_names = []
            # generate list of column names to read
            for topic in range(1, numOfTopics+1):
                column_names.append('t'+ str(topic) + '_cumWait')
            
            #df = pd.read_csv(path + 'registeredTopicsTime.csv', names = column_names)
            df = pd.read_csv(path + 'waiting_times.csv')

            averages_per_topic = {}
            for columnName in column_names:
                waiting_times[columnName] = []
                min_vals[columnName] = []
                max_vals[columnName] = []
                averages_per_topic[columnName] = []

            # TODO: std below and above mean can be used as min and max
            for columnName in column_names:
                print('column name: ', columnName)
                waiting_times[columnName] = df[columnName].to_list()
                print(waiting_times[columnName])
                averages_per_topic[columnName].append( (sum(waiting_times[columnName])*1.0)/len(waiting_times[columnName]) )
                min_vals[columnName].append(min(waiting_times[columnName]))
                max_vals[columnName].append(max(waiting_times[columnName]))
            
            last_average_per_topic = [averages_per_topic[columnName][-1] for columnName in column_names]
            average_vals.append((1.0*sum(last_average_per_topic))/len(column_names))

        min_topic = min(averages_per_topic, key=averages_per_topic.get)
        max_topic = max(averages_per_topic, key=averages_per_topic.get)
        print('Min topic: ', min_topic, ' max topic: ', max_topic)
        f(ax, x_vals, average_vals, min_vals[min_topic], max_vals[max_topic], colors[label_indx], plot_labels[label_indx])
        label_indx += 1

    ax.set_xticks(x_vals)
    ax.set_xlabel(x_label)
                    
    plt.savefig(OUTDIR + '/waiting_times.png')

def analyzeRegistrationTime(dirs, x_vals, x_label, plot_labels): 

    dirs = sorted(dirs)
    colors = ['r', 'g', 'b', 'w', 'e', 'a', 'o']

    fig, ax = plt.subplots(figsize=(10, 4))
    label_indx = 0
    for log_dir in dirs:
        min_vals = {}
        max_vals = {}
        average_vals = []
        sub_dirs = next(os.walk(log_dir))[1]
        sub_dirs = sorted(sub_dirs)
        for subdir in sub_dirs:
            #print(log_dir)
            path = log_dir + '/' + subdir + '/'
            path.replace('//','/')
            df = pd.read_csv(path + 'registeredTopicsTime.csv')

            reg_times = {}
            registrations = {}
            topics = []
            averages_per_topic = {}
            for topic in df['topic'].unique():
                reg_times[topic] = df[df.topic == topic]['average_registration_time']
                registrations[topic] = df[df.topic == topic]['registrant'].value_counts()
                topics.append(topic)
                min_vals[topic] = []
                max_vals[topic] = []
                averages_per_topic[topic] = []

            for topic in topics:
                reg_times[topic] = [(1.0*x)/1000 for x in reg_times[topic]]
                averages_per_topic[topic].append((1.0*sum(reg_times[topic]))/len(reg_times[topic]))
                min_vals[topic].append(min(reg_times[topic]))
                max_vals[topic].append(max(reg_times[topic]))
            
            last_average_per_topic = [averages_per_topic[topic][-1] for topic in topics]
            average_vals.append((1.0*sum(last_average_per_topic))/len(topics))

        min_topic = min(averages_per_topic, key=averages_per_topic.get)
        max_topic = max(averages_per_topic, key=averages_per_topic.get)

        f(ax, x_vals, average_vals, min_vals[min_topic], max_vals[max_topic], colors[label_indx], plot_labels[label_indx])
        label_indx += 1
        
    ax.set_xticks(x_vals)
    ax.set_xlabel(x_label)
                        
    plt.savefig(OUTDIR + '/reg_times.png')

def analyzeDiscoveryTime(dirs, x_vals, x_label, plot_labels): 

    dirs = sorted(dirs)
    colors = ['r', 'g', 'b', 'w', 'e', 'a', 'o']

    fig, ax = plt.subplots(figsize=(10, 4))
    label_indx = 0
    for log_dir in dirs:
        min_vals = {}
        max_vals = {}
        average_vals = []
        sub_dirs = next(os.walk(log_dir))[1]
        sub_dirs = sorted(sub_dirs)
        for subdir in sub_dirs:
            #print(log_dir)
            path = log_dir + '/' + subdir + '/'
            path.replace('//','/')
            df = pd.read_csv(path + 'registeredTopicsTime.csv')

            discv_times = {}
            registrations = {}
            topics = []
            averages_per_topic = {}
            for topic in df['topic'].unique():
                discv_times[topic] = df[df.topic == topic]['average_discovery_time']
                registrations[topic] = df[df.topic == topic]['registrant'].value_counts()
                topics.append(topic)
                averages_per_topic[topic] = []

            for topic in topics:
                if topic not in min_vals.keys():
                    min_vals[topic] = []
                    max_vals[topic] = []

                discv_times[topic] = [(1.0*x)/1000 for x in discv_times[topic]]
                averages_per_topic[topic].append((1.0*sum(discv_times[topic]))/len(discv_times[topic]))
                min_vals[topic].append(min(discv_times[topic]))
                max_vals[topic].append(max(discv_times[topic]))
            
            last_average_per_topic = [averages_per_topic[topic][-1] for topic in topics]
            average_vals.append((1.0*sum(last_average_per_topic))/len(topics))
                

        min_topic = min(averages_per_topic, key=averages_per_topic.get)
        max_topic = max(averages_per_topic, key=averages_per_topic.get)

        f(ax, x_vals, average_vals, min_vals[min_topic], max_vals[max_topic], colors[label_indx], plot_labels[label_indx])
        label_indx += 1
        
    ax.set_xticks(x_vals)
    ax.set_xlabel(x_label)
                        
    plt.savefig(OUTDIR + '/discovery_times.png')

# there should be as many subdirs as x_vals
def analyzeMessageReceivedByNodes(dirs, x_vals, x_label, plot_labels):

    dirs = sorted(dirs)
    colors = ['r', 'g', 'b', 'w', 'e', 'a', 'o']

    fig, ax = plt.subplots()
    label_indx = 0
    dfs = [] 
    for log_dir in dirs:
        y_vals = []
        topics = {}

        min_vals = []
        max_vals = []
        average_vals = []
        sub_dirs = next(os.walk(log_dir))[1]
        sub_dirs = sorted(sub_dirs)
        vals = []
        for subdir in sub_dirs:
            path = log_dir + '/' + subdir + '/'
            path.replace('//','/')
            try: 
                with open(path + 'msg_received.csv', newline='') as csvfile:
                    print('Reading folder ', path)
                    reader = csv.DictReader(csvfile)
                    for row in reader:
                        y_vals.append(int(row['numMsg']))

                    min_vals.append(min(y_vals))
                    average_vals.append((1.0*sum(y_vals))/len(y_vals))
                    max_vals.append(max(y_vals))
                    vals.append([min_vals[-1], average_vals[-1] - min_vals[-1], max_vals[-1] - average_vals[-1]])
            except FileNotFoundError:
                print("Error: ", path, "msg_received.csv not found")
                continue
        #stacked_bar_per_x = np.array([min_vals, average_vals, max_vals])
        stacked_bar_per_x = np.array(vals)
        print('stacked_bar_per_x = ', stacked_bar_per_x)
        df = pd.DataFrame(stacked_bar_per_x, 
                   index=x_vals,
                   columns=["Min", "Average", "Max"])

        dfs.append(df)
        label_indx += 1

    plot_clustered_stacked(dfs,plot_labels)
    #ax.set_xticks(x_vals)
    #ax.set_yticks(ax.get_yticks()[::100])
    ax.set_xlabel(x_label)
    ax.set_ylabel('Number of messages/sec')

    #ax.set_title('Message received by node')

    plt.savefig(OUTDIR + '/messages_received')

if (len(sys.argv) < 2):
    print("Provide at least one directory with log files (messages.csv and 3500000_registrations.csv")
    exit(1)

OUTDIR = './'
if not os.path.exists(OUTDIR):
    os.makedirs(OUTDIR)

print('Will read logs from', sys.argv[1:])
print('Plots will be saved in ', OUTDIR)

#labels = ['AdLifeTime 5 min','AdLifeTime 15 min','AdLifeTime 30 min','AdLifeTime 60 min']
#labels = ['500 nodes','1000 nodes','5000 nodes','10000 nodes']
#labels = ['0.5 AdLifeTime','1 AdLifeTime','1.5 AdLifeTime','2 AdLifeTime']
#labels = ['Bucket size 3','Bucket size 5','Bucket Size 10','Bucket size 16']
##labels = ['No refresh','Refresh']

dirs = ['dht_ticket', 'dht_noticket', 'discv5']
#plot_labels = ['dht', 'discv4', 'discv5']
x_vals = ['500', '1000', '1500', '2000']
x_label = 'network size'
#analyzeWaitingTimes(dirs, x_vals, x_label, dirs)
#analyzeRegistrationTime(dirs, x_vals, x_label, dirs)
#analyzeDiscoveryTime(dirs, x_vals, x_label, dirs)
analyzeMessageReceivedByNodes(dirs, x_vals, x_label, dirs)

