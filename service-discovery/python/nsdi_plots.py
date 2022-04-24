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
import seaborn as sns
from .header import *

from enum import Enum, unique
@unique
class GraphType(Enum):
    line = 1
    bar = 2
    violin = 3


#csv.field_size_limit(sys.maxsize)

def getProtocolFromPath(path):
    return  path.split('/')[0]


#security_features = ['idDistribution', 'sybilSize', 'attackTopic', 'percentEvil', 'discv5regs']
security_features = []
#current dir format: _size-3000_topic-40_...
def getFeatureListFromPath(path):
    print("Getting features from path:", path)
    result = set()
    for item in path.strip('/').split('/')[-1].split('_')[1:]:
        feature = item.split('-')[0]
        print("extrated feature:", feature)
        assert(feature not in result)
        if(feature in security_features):
            continue
        result.add(feature)
    return result

def getFeatureFromPath(feature, path):
    print("feature:", feature)
    print("path: ", path)
    return  path.split('_' + feature + '-')[1].split('_')[0].replace('/', '')

def createPerNodeStats(dir):
    features = set()
    df_list = []
    object_list = os.listdir(dir)
    dirs = []
    for obj in object_list:
        if(os.path.isdir(obj)):
            dirs.append(obj)
    
    for log_dir in dirs:
        print("log_dir:", log_dir)
        if(log_dir not in ['discv5', 'dht', 'dhtnoticket', 'discv4']):
            continue
        tmp = next(os.walk(log_dir))
        print("tmp:", tmp)
        sub_dirs = tmp[1]
        for subdir in sub_dirs:
            path = log_dir + '/' + subdir + '/'
            path.replace('//','/').rstrip()
            print("cwd:", os.getcwd())
            print('Reading folder: ', path+"|")

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
                    #we use ints, floats and strings
                    to_convert = getFeatureFromPath(feature, path)
                    try:
                        #try converting to int
                        value = int(to_convert)
                    except ValueError:
                        try:
                            #try converting to float
                            value = float(to_convert) 
                        except ValueError:
                            #if th above don't work, keep as string
                            value = to_convert
                    df[feature] = value

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
                print("Error: ", path + "msg_received.csv not found")
                quit()
    #merge all the dfs
    dfs = pd.concat(df_list, axis=0, ignore_index=True)
    #print(dfs)
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
    #print(dfs)
    dfs.to_csv('dfs.csv')
    return dfs

def plotPerNodeStats(OUTDIR, simulation_type, graphType = GraphType.violin):
    pd.set_option('display.max_rows', None)
    print("Reading:", os.getcwd(), "/dfs.csv")
    dfs = pd.read_csv('dfs.csv')
    
    #features = ['size']
    #default values for all the features
    defaults = {}
    #FIXME: assumed that the default value is the most popular
    #it might not always be the case for the network size, should think of something better
    for feature in features:
        if( (simulation_type == 'attack') and (features[feature]['type'] == 'attack')):
            defaults[feature] = features[feature]['defaultAttack']    
        else:
            defaults[feature] = features[feature]['default']

    print("############# Features:", features)
    #x-axis
    for feature in features:
        if(features[feature]['type'] != simulation_type):
            continue
        #make sure we don't modify the initial df
        df = dfs.copy(deep=True)
        #filter the df so that we only have default values for non-primary features
        for secondary_feature in features:
            if(features[secondary_feature]['type'] != simulation_type):
                continue
            if(secondary_feature != feature):
                df = df[df[secondary_feature] == defaults[secondary_feature]]
        #y-axis

        if simulation_type == 'benign':
            y_vals = benign_y_vals
        else:
            y_vals = attack_y_vals

        for graph in y_vals:
            fig, ax = plt.subplots()
            print("Plotting y-axis:", graph, "x-axis", feature)

            if(graphType == GraphType.violin):
                sns.violinplot(ax = ax,
                                data = df,
                                x = feature,
                                y = graph,
                                hue = 'protocol',
                                split = False)
            else:
                groups = df.groupby('protocol')

                #set bar in the middle of x-tics
                i = (len(groups)-1) * -0.5
                i = -1.5
                for protocol, group in groups:
                    #NaN -> 0
                    group.to_csv("dupa.csv")
                    print("protocol:", protocol)
                    print("group:", group)
                    group = group.fillna(0)
                    avg = group.groupby(feature)[graph].mean()
                    print("avg_index:", avg.index)
                    x_vals = range(0, avg.index[-1],  int(avg.index[-1]/len(avg.index)))
                    x_vals = list(x_vals)
                    
                    print('x_vals: ', x_vals)
                    std = group.groupby(feature)[graph].std()
                    if(graphType == GraphType.line):
                        avg.plot(x=feature, y=graph, yerr=std, ax=ax, legend=True, label=protocol)
                    elif(graphType == GraphType.bar):
                        #calculate bar width based on the max x-value and the number of protocols
                        width = avg.index[-1]/(len(groups)*(len(groups)+3))
                        #x = [int(val) + (i * width) for val in avg.index]
                        x = [int(val) + (i * width) for val in x_vals]
                        print("x:", x)
                        print("avg:", avg)
                        plt.bar(x, avg, width, label=protocolPrettyText[protocol], yerr = std)
                        ax.set_xlabel(feature)
                        ax.set_ylabel("Average " + titlePrettyText[graph])
                        ax.set_title(titlePrettyText[graph])
                        ax.legend()
                        i += 1
                    else:
                        print("Unknown graph type:", graphType)
                        exit(-1)

                        #evenly space the x ticks
                        ax.set_xticks(x_vals)
                        ax.set_xticklabels(list(avg.index))
            
            fig.savefig(OUTDIR + '/' + graphType.name + "_" + feature + "_" + graph)

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


