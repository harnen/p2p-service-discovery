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
    
    fig, ax = plt.subplots()
    dfs = [] 
    for log_dir in dirs:
        sub_dirs = next(os.walk(log_dir))[1]
        sub_dirs.sort()  # sort alphabetically first
        sub_dirs.sort(key=len) # then sort by ascending length
        waiting_times = {}
        vals = []
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

            # TODO: std below and above mean can be used as min and max
            for columnName in column_names:
                print('column name: ', columnName)
                waiting_times[columnName] = df[columnName].mean()
            
            min_val = float('inf')
            max_val = 0
            total = 0.0
            for columnName in column_names:
                if waiting_times[columnName] < min_val:
                    min_val = waiting_times[columnName]
                if waiting_times[columnName] > max_val:
                    max_val = waiting_times[columnName]

                total += waiting_times[columnName]
            
            average = total / len(column_names)
            vals.append([min_val, average-min_val, max_val-average])
            
        stacked_bar_per_x = np.array(vals)
        print('stacked_bar_per_x = ', stacked_bar_per_x)
        df = pd.DataFrame(stacked_bar_per_x, 
                   index=x_vals,
                   columns=["Min", "Average", "Max"])
        dfs.append(df)

    plot_clustered_stacked(dfs,plot_labels)
    ax.set_xlabel(x_label)
    ax.set_ylabel('Waiting time (sec)')
                    
    plt.tight_layout()
    plt.savefig(OUTDIR + '/waiting_times.png')

def analyzeRegistrationTime(dirs, x_vals, x_label, plot_labels): 

    dirs = sorted(dirs)
    colors = ['r', 'g', 'b', 'w', 'e', 'a', 'o']

    fig, ax = plt.subplots()
    dfs = [] 
    for log_dir in dirs:
        sub_dirs = next(os.walk(log_dir))[1]
        sub_dirs.sort()  # sort alphabetically first
        sub_dirs.sort(key=len) # then sort by ascending length
        vals = []
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
                reg_times[topic] = df[df.topic == topic]['average_registration_time'].mean()
                topics.append(topic)

            min_val = float('inf')
            max_val = 0
            total = 0.0
            for topic in topics:
                if reg_times[topic] < min_val:
                    min_val = reg_times[topic]
                if reg_times[topic] > max_val:
                    max_val = reg_times[topic]
                total += reg_times[topic]

            average = total / len(topics)
            vals.append([min_val, average-min_val, max_val-average])
        
        stacked_bar_per_x = np.array(vals)
        print('stacked_bar_per_x = ', stacked_bar_per_x)
        df = pd.DataFrame(stacked_bar_per_x, 
                   index=x_vals,
                   columns=["Min", "Average", "Max"])
        dfs.append(df)
                        
    plot_clustered_stacked(dfs,plot_labels)
    ax.set_xlabel(x_label)
    ax.set_ylabel('Registration time (sec)')
                        
    plt.tight_layout()
    plt.savefig(OUTDIR + '/reg_times.png')

def analyzeDiscoveryTime(dirs, x_vals, x_label, plot_labels): 

    dirs = sorted(dirs)
    colors = ['r', 'g', 'b', 'w', 'e', 'a', 'o']

    fig, ax = plt.subplots()
    dfs = [] 
    for log_dir in dirs:
        average_vals = []
        sub_dirs = next(os.walk(log_dir))[1]
        sub_dirs.sort()  # sort alphabetically first
        sub_dirs.sort(key=len) # then sort by ascending length
        vals = []
        for subdir in sub_dirs:
            #print(log_dir)
            path = log_dir + '/' + subdir + '/'
            path.replace('//','/')
            df = pd.read_csv(path + 'registeredTopicsTime.csv')

            discv_times = {}
            #registrations = {}
            topics = []
            averages_per_topic = {}
            for topic in df['topic'].unique():
                discv_times[topic] = df[df.topic == topic]['average_discovery_time'].mean()
                #registrations[topic] = df[df.topic == topic]['registrant'].value_counts()
                topics.append(topic)

            min_val = float('inf')
            max_val = 0
            total = 0.0
            for topic in topics:
                if discv_times[topic] < min_val:
                    min_val = discv_times[topic]
                if discv_times[topic] > max_val:
                    max_val = discv_times[topic]
                total += discv_times[topic]

            average = total / len(topics)
            vals.append([min_val, average-min_val, max_val-average])

        stacked_bar_per_x = np.array(vals)
        print('stacked_bar_per_x = ', stacked_bar_per_x)
        df = pd.DataFrame(stacked_bar_per_x, 
                   index=x_vals,
                   columns=["Min", "Average", "Max"])
        dfs.append(df)

    plot_clustered_stacked(dfs,plot_labels)
    ax.set_xlabel(x_label)
    ax.set_ylabel('Discovery time (sec)')
                        
    plt.tight_layout()
    plt.savefig(OUTDIR + '/discovery_times.png')

# there should be as many subdirs as x_vals
def analyzeMessageReceivedByNodes(dirs, x_vals, x_label, plot_labels):

    dirs = sorted(dirs)
    colors = ['r', 'g', 'b', 'w', 'e', 'a', 'o']

    fig, ax = plt.subplots()
    dfs = [] 
    dfsreg = [] 
    dfslook = [] 
    for log_dir in dirs:
        y_vals = []
        yreg_vals = []
        ylook_vals = []
        
        min_vals = []
        minreg_vals = []
        minlook_vals = []
        max_vals = []
        maxreg_vals = []
        maxlook_vals = []
        average_vals = []
        averagereg_vals = []
        averagelook_vals = []
        #foos.walk(log_dir))
        sub_dirs = next(os.walk(log_dir))[1]
        sub_dirs.sort()  # sort alphabetically first
        sub_dirs.sort(key=len) # then sort by ascending length
        vals = []
        valsreg = []
        valslook = []
        for subdir in sub_dirs:
            path = log_dir + '/' + subdir + '/'
            path.replace('//','/')
            try: 
                with open(path + 'msg_received.csv', newline='') as csvfile:
                    print('Reading folder ', path)
                    reader = csv.DictReader(csvfile)

                    for row in reader:
                        #calculate a total for registration
                        yreg_vals.append(int(row['MSG_REGISTER']) + int(row['MSG_TICKET_REQUEST']) + int(row['MSG_TICKET_REQUEST']) + int(row['MSG_REGISTER_RESPONSE']))
                        ylook_vals.append(int(row['MSG_TOPIC_QUERY']) + int(row['MSG_TOPIC_QUERY_REPLY']))
                        y_vals.append(int(row['numMsg']))

                    min_vals.append(min(y_vals))
                    minreg_vals.append(min(yreg_vals))
                    minlook_vals.append(min(ylook_vals))

                    average_vals.append((1.0*sum(y_vals))/len(y_vals))
                    averagereg_vals.append((1.0*sum(yreg_vals))/len(yreg_vals))
                    averagelook_vals.append((1.0*sum(ylook_vals))/len(ylook_vals))

                    max_vals.append(max(y_vals))
                    maxreg_vals.append(max(yreg_vals))
                    maxlook_vals.append(max(ylook_vals))

                    vals.append([min_vals[-1], average_vals[-1] - min_vals[-1], max_vals[-1] - average_vals[-1]])
                    valsreg.append([minreg_vals[-1], averagereg_vals[-1] - minreg_vals[-1], maxreg_vals[-1] - averagereg_vals[-1]])
                    print("minlook_vals:", minlook_vals, "averagelook_vals", averagelook_vals, "maxlook_vals", maxlook_vals)
                    valslook.append([minlook_vals[-1], averagelook_vals[-1] - minlook_vals[-1], maxlook_vals[-1] - averagelook_vals[-1]])
            except FileNotFoundError:
                print("Error: ", path, "msg_received.csv not found")
                continue
        #stacked_bar_per_x = np.array([min_vals, average_vals, max_vals])
        stacked_bar_per_x = np.array(vals)
        df = pd.DataFrame(stacked_bar_per_x, 
                   index=x_vals,
                   columns=["Min", "Average", "Max"])
        dfs.append(df)

        stacked_bar_per_x = np.array(valsreg)
        df = pd.DataFrame(stacked_bar_per_x, 
                   index=x_vals,
                   columns=["Min", "Average", "Max"])
        dfsreg.append(df)

        stacked_bar_per_x = np.array(valslook)
        df = pd.DataFrame(stacked_bar_per_x, 
                   index=x_vals,
                   columns=["Min", "Average", "Max"])
        dfslook.append(df)

    plot_clustered_stacked(dfs,plot_labels)
    plt.show()
    plot_clustered_stacked(dfsreg,plot_labels)
    plt.show()
    plot_clustered_stacked(dfslook,plot_labels)
    plt.show()
    #ax.set_xticks(x_vals)
    #ax.set_yticks(ax.get_yticks()[::100])
    ax.set_xlabel(x_label)
    ax.set_ylabel('Number of messages/sec')

    #ax.set_title('Message received by node')
    plt.tight_layout()
    #plt.savefig(OUTDIR + '/messages_received')
    

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

dirs = ['dhtticket', 'dhtnoticket', 'discv5', 'discv4']
#plot_labels = ['dht', 'discv4', 'discv5']
x_vals = ['100', '200', '300']
x_label = 'network size'

#analyzeRegistrationTime(dirs, x_vals, x_label, dirs)
#analyzeDiscoveryTime(dirs, x_vals, x_label, dirs)
analyzeMessageReceivedByNodes(dirs, x_vals, x_label, dirs)

dirs = ['dhtticket', 'discv5']
#analyzeWaitingTimes(dirs, x_vals, x_label, dirs)
dirs = ['dhtticket', 'dhtnoticket', 'discv5', 'discv4']

