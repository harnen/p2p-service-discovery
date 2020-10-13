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
    
        df['type'].value_counts().plot(ax=ax2, kind='bar', title="Message types", width=1)
    
        df['src'].value_counts().plot(ax=ax3, kind='line', xticks=[], title="Message sent by node", label=log_dir)
    ax1.legend()
    ax3.legend()
    #plt.show()

def analyzeRegistrations(dirs):
    print("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    fig, ax1 = plt.subplots()
    fig, ax2 = plt.subplots()
    fig, ax3 = plt.subplots()

    for log_dir in dirs:
        print(log_dir)
        df = pd.read_csv(log_dir + '/3500000_registrations.csv')
        df['host'].value_counts().plot(ax=ax1, kind='line', xticks=[], title="Registrations by advertisement medium", label=log_dir)

        df['topic'].value_counts().plot(ax=ax2, kind='bar', title="Registerations by topic", label=log_dir)

        df['registrant'].value_counts().plot(ax=ax3, kind='line', xticks=[], title="Registrations by advertiser")
    ax1.legend()
    ax3.legend()

    #plt.show()

if (len(sys.argv) < 2):
    print("Provide at least one directory with log files (messages.csv and 3500000_registrations.csv")
    exit(1)

print('Will read logs from', sys.argv[1:])
analyzeMessages(sys.argv[1:])
analyzeRegistrations(sys.argv[1:])

plt.show()
