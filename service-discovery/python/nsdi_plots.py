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

# there should be as many subdirs as x_vals
def analyzeMessageReceivedByNodes(dirs, x_vals, x_label, plot_labels):

    dirs = sorted(dirs)
    colors = ['r', 'g', 'b', 'w', 'e', 'a', 'o']

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
        path = LOGDIR + '/' + log_dir
        path.replace('//','/')
        sub_dirs = next(os.walk(path))[1]
        sub_dirs.sort()  # sort alphabetically first
        sub_dirs.sort(key=len) # then sort by ascending length
        vals = []
        valsreg = []
        valslook = []
        for subdir in sub_dirs:
            path = LOGDIR + '/' + log_dir + '/' + subdir + '/'
            path.replace('//','/')
            try: 
                with open(path + 'msg_received.csv', newline='') as csvfile:
                    print('Reading folder ', path)
                    reader = csv.DictReader(csvfile)

                    for row in reader:
                        #calculate a total for registration
                        yreg_vals.append(int(row['MSG_REGISTER']) + int(row['MSG_TICKET_REQUEST']) + int(row['MSG_TICKET_RESPONSE']) + int(row['MSG_REGISTER_RESPONSE']))
                        ylook_vals.append(int(row['MSG_TOPIC_QUERY']) + int(row['MSG_TOPIC_QUERY_REPLY']) + int(row['MSG_FIND']) + int(row['MSG_RESPONSE']))
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
                    valslook.append([minlook_vals[-1], averagelook_vals[-1] - minlook_vals[-1], maxlook_vals[-1] - averagelook_vals[-1]])
                    #print("minlook_vals:", minlook_vals, "averagelook_vals", averagelook_vals, "maxlook_vals", maxlook_vals)
                    #print("minreg_vals:", minreg_vals, "averagereg_vals", averagereg_vals, "maxreg_vals", maxreg_vals)
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

    fig, ax = plot_clustered_stacked(dfs,plot_labels)
    fig.tight_layout()
    ax.set_xlabel(x_label)
    ax.set_ylabel('Number of messages/sec')
    ax.set_title('Total overhead')
    fig.savefig(OUTDIR + '/messages_received')

    fig, ax = plot_clustered_stacked(dfsreg,plot_labels)
    fig.tight_layout()
    ax.set_xlabel(x_label)
    ax.set_title('Registration overhead')
    ax.set_ylabel('Number of messages/sec')
    fig.savefig(OUTDIR + '/reg_messages_received')

    fig, ax = plot_clustered_stacked(dfslook,plot_labels)
    fig.tight_layout()
    ax.set_xlabel(x_label)
    ax.set_title('Lookup overhead')
    ax.set_ylabel('Number of messages/sec')
    fig.savefig(OUTDIR + '/look_messages_received')
    #ax.set_xticks(x_vals)
    #ax.set_yticks(ax.get_yticks()[::100])
    

    #ax.set_title('Message received by node')
    #
    #plt.savefig(OUTDIR + '/messages_received')

def getProtocolFromPath(path):
    return  path.split('/')[0]

def getNetworkSizeFromPath(path):
    return  int(path.split('size_')[1].strip('/'))

def analyseRegistrations(dir):
    df_list = []
    object_list = os.listdir(dir)
    dirs = []
    for obj in object_list:
        if(os.path.isdir(obj)):
            dirs.append(obj)
    
    for log_dir in dirs:
        print("log_dir:", log_dir)
        tmp = next(os.walk(log_dir))
        print("tmp:", tmp)
        sub_dirs = tmp[1]
        for subdir in sub_dirs:
            path = log_dir + '/' + subdir + '/'
            path.replace('//','/')
            print('Reading folder ', path)
            try: 
                df = pd.read_csv(path + 'register_overhead.csv')
                protocol = getProtocolFromPath(path)
                size = getNetworkSizeFromPath(path)
                    
                df['protocol'] = protocol
                df['size'] = size

                #extract the total number of registrations
                #the file doesn't exist for discv4
                if(protocol == 'discv4'):
                    total_regs = 0
                else:
                    df_regs = pd.read_csv(path + '')
                    #this is dirty, but for some reason I couldn't make
                    #df_regs['registration'].sum() work
                    total_regs[protocol + str(size)] = df_regs.sum()[2]/2
                    df['num_regs'] = total_regs

            except FileNotFoundError:
                print("Error: ", path, "msg_received.csv not found")
                continue



def createPerNodeStats(dir):
    df_list = []
    reg_count = {}
    reg_count['protocol'] = []
    reg_count['size'] = []
    reg_count['registrations'] = []
    object_list = os.listdir(dir)
    dirs = []
    for obj in object_list:
        if(os.path.isdir(obj)):
            dirs.append(obj)
    
    for log_dir in dirs:
        print("log_dir:", log_dir)
        tmp = next(os.walk(log_dir))
        print("tmp:", tmp)
        sub_dirs = tmp[1]
        for subdir in sub_dirs:
            path = log_dir + '/' + subdir + '/'
            path.replace('//','/')
            print('Reading folder ', path)
            try:
                protocol = getProtocolFromPath(path)
                size = getNetworkSizeFromPath(path)

                df = pd.read_csv(path + 'msg_received.csv')
                df.to_csv(path + 'df.csv')

                df_regs_done = pd.read_csv(path + 'registeredRegistrant.csv')
                #rename columns do make it consistent across dfs
                df_regs_done.rename(columns = {'nodeId':'Node', 'count':'regsPlaced', 'evil':'evilRegsPlaced'}, inplace = True)
                df_regs_done.to_csv(path + 'df_regs_done.csv')
                df_merged = df.merge(df_regs_done, how='left', on='Node')
                df_regs_done.to_csv(path + 'df_regs_done.csv')
                df_merged.to_csv(path + 'df_merged1.csv')

                df_regs_accepted = pd.read_csv(path + 'registeredRegistrar.csv')
                #rename columns do make it consistent across dfs
                df_regs_accepted.rename(columns = {'nodeId':'Node', 'count':'regsAccepted', 'evil':'evilRegsAccepted'}, inplace = True)
                #merge per topic info
                df_regs_accepted = df_regs_accepted.groupby(['Node'])['regsAccepted', 'evilRegsAccepted'].sum().reset_index()
                df_regs_accepted.to_csv(path + 'df_refs_accepted.csv')
                df_merged = df_merged.merge(df_regs_accepted, how='left', on='Node')
                #NaN -> 0
                df_merged.fillna(0)
                df_merged.to_csv(path + 'df_merged2.csv')

                #those should hold without turbulance
                assert(len(df.index) == size)
                assert(len(df_merged.index) == size)
                
                df = df_merged
                df['protocol'] = protocol
                df['size'] = size

                if(protocol == 'discv4'):
                    #should be all 0, but plotting for sanity check
                    reg_cols = ['MSG_REGISTER', 'MSG_TICKET_REQUEST', 'MSG_TICKET_RESPONSE', 'MSG_REGISTER_RESPONSE']
                    df['registrationMsgs'] = df[reg_cols].sum(axis=1)
                    look_cols = ['MSG_FIND', 'MSG_RESPONSE', 'MSG_TOPIC_QUERY', 'MSG_TOPIC_QUERY_REPLY']
                    df['lookupMsgs'] = df[look_cols].sum(axis=1)             
                    #discv4 doesn't place any registrations
                    total_regs = 0
                else:
                    reg_cols = ['MSG_REGISTER', 'MSG_TICKET_REQUEST', 'MSG_TICKET_RESPONSE', 'MSG_REGISTER_RESPONSE', 'MSG_FIND', 'MSG_RESPONSE']
                    df['registrationMsgs'] = df[reg_cols].sum(axis=1)
                    look_cols = ['MSG_TOPIC_QUERY', 'MSG_TOPIC_QUERY_REPLY']
                    df['lookupMsgs'] = df[look_cols].sum(axis=1)  

                    #read registrations from the file
                    df_regs = pd.read_csv(path + 'register_overhead.csv')
                    #this is dirty, but for some reason I couldn't make
                    #df_regs['registration'].sum() work
                    total_regs = df_regs.sum()[2]/2

                reg_count['protocol'].append(protocol)
                reg_count['size'].append(size)
                reg_count['registrations'].append(total_regs)

                df_list.append(df)
                df.to_csv(path + 'df.csv')
            except FileNotFoundError:
                print("Error: ", path, "msg_received.csv not found")
                quit()
    #merge all the dfs
    dfs = pd.concat(df_list, axis=0, ignore_index=True)
    print(dfs)
    dfs.to_csv('dfs.csv')
    return dfs


def analyseOverhead(dfs):

    pd.set_option('display.max_rows', None)
    for graph in ['registrationMsgs', 'lookupMsgs', 'discovered', 'regsPlaced', 'regsAccepted']:
        fig, ax = plt.subplots()
        for protocol, group in dfs.groupby('protocol'):
            #NaN -> 0
            group = group.fillna(0)
            #print("!!!!!!!!!!!!!protocol", protocol, "graph:", graph)
            #print(group['regsPlaced'])
            avg = group.groupby('size')[graph].mean()
            std = group.groupby('size')[graph].std()
            bx = avg.plot(x='size', y=graph, yerr=std, ax=ax, legend=True, label=protocol)
            bx.set_xlabel("Network Size")
            if (graph == 'discovered'):
                bx.set_ylabel("# Avg Peers discovered")
                bx.set_title("Peers discovered")
            else:
                bx.set_ylabel("Messages")
                bx.set_title(graph + " overhead")
            
        fig.savefig(OUTDIR + '/' + graph)

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


dfs = createPerNodeStats(LOGDIR)
analyseOverhead(dfs)

#dirs = ['dhtticket', 'discv5']
#analyzeWaitingTimes(dirs, x_vals, x_label, dirs)
#dirs = ['dhtticket', 'dhtnoticket', 'discv5', 'discv4']

