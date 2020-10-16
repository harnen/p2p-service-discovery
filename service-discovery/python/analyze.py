#!/usr/bin/python3

import pandas as pd
import matplotlib
from matplotlib import pyplot as plt
import sys

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

def analyzeOperations(dirs):
    print("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    fig, ax1 = plt.subplots()
    fig, ax2 = plt.subplots()
    
    x = ['RegisterOperation','LookupOperation']
    for log_dir in dirs:
        print(log_dir)
        df = pd.read_csv(log_dir + '/operations.csv')
        print(df['type'].value_counts())
        ax1.bar(df['type'].value_counts().index, df['type'].value_counts(), label=log_dir)
        ax1.set_title("Operations by type")
    
        print(df['hops'].mean())
        ax2.bar(log_dir, df['hops'].mean()) 
        ax2.set_title("Avg hop count")
        
    ax1.legend()
    ax2.legend()

if (len(sys.argv) < 2):
    print("Provide at least one directory with log files (messages.csv and 3500000_registrations.csv")
    exit(1)

print('Will read logs from', sys.argv[1:])
analyzeMessages(sys.argv[1:])
analyzeRegistrations(sys.argv[1:])
analyzeOperations(sys.argv[1:])

plt.show()
