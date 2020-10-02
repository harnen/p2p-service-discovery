import pandas as pd
import matplotlib
from matplotlib import pyplot as plt

def analyzeMessages(df):
    print(df)
    print("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    fig, ax = plt.subplots()
    df['dst'].value_counts().plot(ax=ax, kind='bar', xticks=[], title="Message received by node", width=1)
    fig, ax = plt.subplots()
    df['type'].value_counts().plot(ax=ax, kind='bar', title="Message types", width=1)
    fig, ax = plt.subplots()
    df['src'].value_counts().plot(ax=ax, kind='bar', xticks=[], title="Message sent by node", width=1)

    #plt.show()

def analyzeRegistrations(df):
    print(df)

    fig, ax = plt.subplots()
    df['host'].value_counts().plot(ax=ax, kind='bar', xticks=[], title="Registrations by advertisement medium", width=1)

    fig, ax = plt.subplots()
    df['topic'].value_counts().plot(ax=ax, kind='bar', title="Registerations by topic")

    fig, ax = plt.subplots()
    df['registrant'].value_counts().plot(ax=ax, kind='bar', xticks=[], title="Registrations by advertiser", width=1)

    #plt.show()

df_messages = pd.read_csv("../messages.csv")
analyzeMessages(df_messages)

#input()

df_registrations = pd.read_csv("../300000_registrations.csv")
analyzeRegistrations(df_registrations)

plt.show()

