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

csv.field_size_limit(sys.maxsize)

features = set()

titlePrettyText = {'registrationMsgs' : 'Number of Registration Messages', 
              'lookupMsgs': 'Number of Lookup Messages', 
              'discovered' : 'Number of Discovered Peers', 
              'wasDiscovered': 'Number of Times Discovered by Others',
              'lookupAskedNodes' : 'Number of Contacted Nodes during Lookups', 
              'percentageEclipsedLookups': 'Percentage of Eclipsed Lookups', 
              'percentageMaliciousDiscovered' : 'Percentage of Malicious Nodes Returned from Lookups', 
              'regsPlaced': 'Number of Registrations Placed',
              'regsAccepted':'Number of Registrations Accepted'
              }

protocolPrettyText = {'dhtnoticket':'DHT_w/o_Ticket',
                      'dhtticket': 'DHT_Ticket',
                      'discv5' : 'TBSD',
                      'discv4' : 'Discv4'
                      }

def getProtocolFromPath(path):
    return  path.split('/')[0]

#current dir format: _size-3000_topic-40_...
def getFeatureListFromPath(path):
    result = set()
    for item in path.split('.')[-1].split('_')[1:]:
        feature = item.split('-')[0]
        assert(feature not in result)
        result.add(feature)
    return result

def getFeatureFromPath(feature, path):
    return  path.split('_' + feature + '-')[1].split('_')[0].replace('/', '')

def createPerNodeStats(dir):
    global features
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
                df = pd.read_csv(path + 'msg_received.csv')
                protocol = getProtocolFromPath(path)
                df['protocol'] = protocol
                
                #include features read from the path
                dir_features = getFeatureListFromPath(path)
                if(len(features) == 0):
                    features = dir_features
                else:
                    #make sure we have the same set of features in every dir
                    assert(dir_features == features)

                for feature in features:
                    df[feature] = int(getFeatureFromPath(feature, path))

                df['percentageMaliciousDiscovered'] = np.where(df['discovered'] == 0, 0, df['maliciousDiscovered']/df['discovered'])
                df['percentageEclipsedLookups'] = np.where(df['lookupOperations'] == 0, 0, df['eclipsedLookupOperations']/df['lookupOperations'])

                if(protocol == 'discv4'):
                    #should be all 0 in discv4, but including anyway for sanity check
                    reg_cols = ['MSG_REGISTER', 'MSG_TICKET_REQUEST', 'MSG_TICKET_RESPONSE', 'MSG_REGISTER_RESPONSE']
                    df['registrationMsgs'] = df[reg_cols].sum(axis=1)
                    look_cols = ['MSG_FIND', 'MSG_RESPONSE', 'MSG_TOPIC_QUERY', 'MSG_TOPIC_QUERY_REPLY']
                    df['lookupMsgs'] = df[look_cols].sum(axis=1)             
                else:
                    reg_cols = ['MSG_REGISTER', 'MSG_TICKET_REQUEST', 'MSG_TICKET_RESPONSE', 'MSG_REGISTER_RESPONSE', 'MSG_FIND', 'MSG_RESPONSE']
                    df['registrationMsgs'] = df[reg_cols].sum(axis=1)
                    look_cols = ['MSG_TOPIC_QUERY', 'MSG_TOPIC_QUERY_REPLY']
                    df['lookupMsgs'] = df[look_cols].sum(axis=1)  

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

# Used for eclipsing results
def createPerLookupOperationStats(dir):
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
                df = pd.read_csv(path + 'eclipse_counts.csv')
                protocol = getProtocolFromPath(path)
                sybilSize = int(getFeatureFromPath('sybil_size', path))
                attackTopic = getFeatureFromPath('attackTopic', path)
                distribution = getFeatureFromPath('id_distribution', path)
                percentEvil = getFeatureFromPath('percentEvil', path)
                print("From path:", path, "Extracted protocol:", protocol, "sybil size:", sybilSize, "attack topic:", attackTopic, "ID distribution: ",distribution )

                percentDiscoveredField = 'PercentEvilDiscovered-t' + attackTopic
                percentEclipsedField = 'PercentEclipsed-t' + attackTopic
                df = df.loc[~(df[percentDiscoveredField] == 0)]
                print('Series: ', df[percentDiscoveredField])
                print('Here')
                df['PercentEvilDiscovered'] = df[percentDiscoveredField]
                df['PercentEclipsed'] = df[percentEclipsedField] 
                df['percentEvil'] = percentEvil
                
                df['protocol'] = protocol
                df['sybilSize'] = sybilSize
                df['attackTopic'] = attackTopic
                df['distribution'] = distribution
                 
                df_list.append(df)
                df.to_csv(path + 'df.csv')
            except FileNotFoundError:
                print("Error: ", path, "eclipse_counts.csv not found")
                quit()
    #merge all the dfs
    dfs = pd.concat(df_list, axis=0, ignore_index=True)
    print(dfs)
    dfs.to_csv('dfs.csv')
    return dfs

def plotPerNodeStats(bar=True):
    pd.set_option('display.max_rows', None)
    dfs = pd.read_csv('dfs.csv')
    
    #features = ['topic']
    #default values for all the features
    #defaults = {'size':21970}
    #FIXME: assumed that the default value is the most popular
    #it might not always be the case for the network size, should think of something better
    for feature in features:
        defaults[feature] = dfs[feature].value_counts().idxmax()

    #x-axis
    for feature in features:
        #make sure we don't modify the initial df
        df = dfs.copy(deep=True)
        #filter the df so that we only have default values for non-primary features
        for secondary_feature in features:
            if(secondary_feature != feature):
                df = df[df[secondary_feature] == defaults[secondary_feature]]
        #y-axis
        for graph in ['registrationMsgs', 'lookupMsgs', 'discovered', 'wasDiscovered', 'regsPlaced', 
                    'regsAccepted', 'lookupAskedNodes', 'percentageMaliciousDiscovered', 'percentageEclipsedLookups']:
            fig, ax = plt.subplots()
            groups = df.groupby('protocol')

            #set bar in the middle of x-tics
            i = (len(groups)-1) * -0.5
            i = -1.5
            for protocol, group in groups:
                #NaN -> 0
                group = group.fillna(0)
                avg = group.groupby(feature)[graph].mean()
                x_vals = range(0, avg.index[-1],  int(avg.index[-1]/len(avg.index)))
                x_vals = list(x_vals)
                #print('x_vals: ', x_vals)
                std = group.groupby(feature)[graph].std()
                if(bar == False):
                    avg.plot(x=feature, y=graph, yerr=std, ax=ax, legend=True, label=protocol)
                else:
                    #calculate bar width based on the max x-value and the number of protocols
                    width = avg.index[-1]/(len(groups)*(len(groups)+3))
                    #x = [int(val) + (i * width) for val in avg.index]
                    x = [int(val) + (i * width) for val in x_vals]
                    plt.bar(x, avg, width, label=protocolPrettyText[protocol], yerr = std)
                    ax.set_xlabel(feature)
                    ax.set_ylabel("Average " + titlePrettyText[graph])
                    ax.set_title(titlePrettyText[graph])
                    ax.legend()
                    i += 1

                    #evenly space the x ticks
                    ax.set_xticks(x_vals)
                    ax.set_xticklabels(list(avg.index))
            if(bar == False):
                fig.savefig(OUTDIR + '/'+ feature + "_" + graph)
            else:
                fig.savefig(OUTDIR + '/' + 'bar_'  + feature + "_" + graph)

def plotPerLookupOperation():
    pd.set_option('display.max_rows', None)
    dfs = pd.read_csv('dfs.csv')
    features = ['sybilSize', 'attackTopic', 'distribution', 'percentEvil']
    #default values for all the features
    defaults = {'sybilSize':'5', 'attackTopic':'1', 'distribution':'uniform', 'percentEvil':'0.2'}
    for feature in features:
        defaults[feature] = dfs[feature].value_counts().idxmax()

    #x-axis
    for feature in features:
        #make sure we don't modify the initial df
        df = dfs.copy(deep=True)
        #filter the df so that we only have default values for non-primary features
        for secondary_feature in features:
            if(secondary_feature != feature):
                df = df[df[secondary_feature] == defaults[secondary_feature]]
        #y-axis
        for graph in ['PercentEvilDiscovered', 'PercentEclipsed']:
            fig, ax = plt.subplots()
            for protocol, group in df.groupby('protocol'):
                #NaN -> 0
                group = group.fillna(0)
                avg = group.groupby(feature)[graph].mean()
                std = group.groupby(feature)[graph].std()
                bx = avg.plot(x=feature, y=graph, yerr=std, ax=ax, legend=True, label=protocol)
                bx.set_xlabel(feature)
                bx.set_ylabel("Average " + graph)
                bx.set_title(graph)
                
            fig.savefig(OUTDIR + '/' + feature + "_" + graph)

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

#createPerLookupOperationStats(LOGDIR)
#plotPerLookupOperation()

createPerNodeStats(LOGDIR)
plotPerNodeStats()
