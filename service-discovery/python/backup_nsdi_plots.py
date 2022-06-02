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
import scipy.stats as sp # for calculating standard error
import os


def plot_clustered_stacked(dfall, labels=None, title="",  H="/", **kwargs):
    """Given a list of dataframes, with identical columns and index, create a clustered stacked bar plot.                                                                                     
labels is a list of the names of the dataframe, used for the legend                            
title is a string for the title of the plot 
H is the hatch used for identification of the different dataframe"""                           
    
    n_df = len(dfall)                                                                          
    n_col = len(dfall[0].columns) 
    n_ind = len(dfall[0].index)                                                                
    #axe = plt.subplots(111)  
    fig, axe = plt.subplots()                                                                   
        
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

    return fig, axe

# plot per-topic, average waiting times and number
def analyzeWaitingTimes(dirs, x_vals, x_label, plot_labels):

    dirs = sorted(dirs)
    colors = ['r', 'g', 'b', 'w', 'e', 'a', 'o']
    
    fig, ax = plt.subplots()
    dfs = [] 
    for log_dir in dirs:
        path = LOGDIR + '/' + log_dir
        path.replace('//','/')
        sub_dirs = next(os.walk(path))[1]
        sub_dirs.sort()  # sort alphabetically first
        sub_dirs.sort(key=len) # then sort by ascending length
        waiting_times = {}
        vals = []
        for subdir in sub_dirs:
            path = LOGDIR + '/' + log_dir + '/' + subdir + '/'
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
        path = LOGDIR + '/' + log_dir
        path.replace('//','/')
        sub_dirs = next(os.walk(path))[1]
        sub_dirs.sort()  # sort alphabetically first
        sub_dirs.sort(key=len) # then sort by ascending length
        vals = []
        for subdir in sub_dirs:
            #print(log_dir)
            path = LOGDIR + '/' + log_dir + '/' + subdir + '/'
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
        path = LOGDIR + '/' + log_dir
        path.replace('//','/')
        sub_dirs = next(os.walk(path))[1]
        sub_dirs.sort()  # sort alphabetically first
        sub_dirs.sort(key=len) # then sort by ascending length
        vals = []
        for subdir in sub_dirs:
            #print(log_dir)
            path = LOGDIR + '/' + log_dir + '/' + subdir + '/'
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

OUTDIR = './'

if len(sys.argv) < 2:
    print('Usage: ', sys.argv[0], ' <Path_to_Log_files> <OPTIONAL: Path_to_output_dir>' )
    sys.exit(1) 

LOGDIR = sys.argv[1]
if len(sys.argv) > 2:
    OUTDIR = sys.argv[2]

if not os.path.exists(OUTDIR):
    os.makedirs(OUTDIR)

print('Will read logs from', LOGDIR)
print('Plots will be saved in ', OUTDIR)

#labels = ['AdLifeTime 5 min','AdLifeTime 15 min','AdLifeTime 30 min','AdLifeTime 60 min']
#labels = ['500 nodes','1000 nodes','5000 nodes','10000 nodes']
#labels = ['0.5 AdLifeTime','1 AdLifeTime','1.5 AdLifeTime','2 AdLifeTime']
#labels = ['Bucket size 3','Bucket size 5','Bucket Size 10','Bucket size 16']
##labels = ['No refresh','Refresh']

dirs = ['dhtnoticket', 'dhtticket', 'discv4', 'discv5']
#plot_labels = ['dht', 'discv4', 'discv5']
x_vals = ['1000', '2000', '3000', '4000', '5000']
x_label = 'network size'


#dfs = createPerNodeStats(LOGDIR)
analyseOverhead()

#dirs = ['dhtticket', 'discv5']
#analyzeWaitingTimes(dirs, x_vals, x_label, dirs)
#dirs = ['dhtticket', 'dhtnoticket', 'discv5', 'discv4']



#labels = ['AdLifeTime 5 min','AdLifeTime 15 min','AdLifeTime 30 min','AdLifeTime 60 min']
#labels = ['500 nodes','1000 nodes','5000 nodes','10000 nodes']
#labels = ['0.5 AdLifeTime','1 AdLifeTime','1.5 AdLifeTime','2 AdLifeTime']
#labels = ['Bucket size 3','Bucket size 5','Bucket Size 10','Bucket size 16']
##labels = ['No refresh','Refresh']

dirs = ['dhtnoticket', 'dhtticket', 'discv4', 'discv5']
#plot_labels = ['dht', 'discv4', 'discv5']
x_vals = ['1000', '2000', '3000', '4000', '5000']
x_label = 'network size'


#dirs = ['dhtticket', 'discv5']
#analyzeWaitingTimes(dirs, x_vals, x_label, dirs)
#dirs = ['dhtticket', 'dhtnoticket', 'discv5', 'discv4']

