import simpy
from table import *
import matplotlib.pyplot as plt
from threshold import *
import sys


runtime = 100
ad_lifetime = 300
capacity = 100
input_file = './workloads/regular_size100_dist2.csv'

if len(sys.argv) > 1:
    input_file = sys.argv[1]


tables = []
tables.append(SimpleTable(simpy.Environment(), capacity, ad_lifetime))
tables.append(DiversityThreshold(simpy.Environment(), capacity, ad_lifetime,  ipThresholds = {8: capacity/4, 16: capacity/8, 24: capacity/16, 32 : capacity/32}, entropyLimit = 0.8))
tables.append(DiversityTable(simpy.Environment(), capacity, ad_lifetime))
for table in tables:
    table.load(input_file)
    table.display(99)
    table.run(runtime)
plt.show()