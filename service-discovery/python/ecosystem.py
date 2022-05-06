#!/usr/bin/python3
import pandas as pd
import matplotlib
from matplotlib import pyplot as plt
from matplotlib.ticker import StrMethodFormatter
from numpy import arange
import numpy as np
from matplotlib import mlab

font = {'family' : 'normal',
        'weight' : 'bold',
        'size'   : 16}

matplotlib.rc('font', **font)
matplotlib.rcParams['pdf.fonttype'] = 42
matplotlib.rcParams['ps.fonttype'] = 42



#protocols = list(reversed(['Eth Mainnet', 'Eth Ropsten', 'Eth Rinkeby', 'Swarm', 'Musicoin', 'Pirl', 'LES', 'Eth Classic', 'Ubiq', 'Other']))
protocols = {'Eth Mainnet':173000, 'Eth Ropsten':14958, 'Eth Rinkeby':14944, 'Swarm':6579, 'Musicoin':5235, 'Pirl':4976, 'LES':4431, 'Eth Classic':3974, 'Ubiq':3685, 'Other':10446}
#counts = [173, 14.958, 14.944, 6.579, 5.235, 4.976, 4.431, 3.974, 3.685, 10.446]
#counts = [17, 14.958, 14.944, 6.579, 5.235, 4.976, 4.431, 3.974, 3.685, 10.446]
#counts = [1, 1, 1, 1, 2, 3, 3, 4, 5, 6, 7, 8, 9, 10]
counts = []

for protocol in protocols.keys():
    counts.append([protocol]*protocols[protocol])

np.random.seed(0)

mu = 200
sigma = 25
n_bins = 400



fig, ax = plt.subplots(figsize=(8, 4))

counts = [0.1, 0.1, 0.1, 0.1]
print("sorted:", sorted(counts))
# plot the cumulative histogram
n, bins, patches = ax.hist(counts, n_bins, density=True, histtype='step',
                           cumulative=True, label='Empirical')


# tidy up the figure
ax.grid(True)
ax.legend(loc='right')
ax.set_title('Cumulative step histograms')
ax.set_xlabel('Annual rainfall (mm)')
ax.set_ylabel('Likelihood of occurrence')

plt.show()




plt.show()
exit(1)

df = pd.DataFrame({"Protocol": protocols,"Count": counts})
ax = df.plot(kind='barh', x='Protocol', y='Count', figsize=(10, 4), width=0.8, color='0.6')

# Despine
ax.spines['right'].set_visible(False)
ax.spines['top'].set_visible(False)
ax.spines['left'].set_visible(False)
#ax.spines['bottom'].set_visible(False)
ax.get_legend().remove()

# Switch off ticks
#ax.tick_params(axis="both", which="both", bottom="off", top="off", labelbottom="on", left="off", right="off", labelleft="on")

# Draw vertical axis lines
vals = ax.get_xticks()
for tick in vals:
    ax.axvline(x=tick, linestyle='dashed', alpha=0.4, color='#eeeeee', zorder=1)

# Set x-axis label
ax.set_xlabel("#Nodes (thousands)")#, labelpad=20, weight='bold', size=12)

# Set y-axis label
#ax.set_ylabel("Start Station", labelpad=20, weight='bold', size=12)
#Format y-axis label
ax.xaxis.set_major_formatter(StrMethodFormatter('{x:,g}'))
plt.show()