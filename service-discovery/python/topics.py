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

if (len(sys.argv) < 2):
    print("Provide at least one file")
    exit(1)

df = pd.read_csv('./topics.csv')
print(df)
plt.plot(df.sort_values(by=['topics'])['topics'].to_list())

plt.show()