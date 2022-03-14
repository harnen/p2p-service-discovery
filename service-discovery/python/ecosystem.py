#!/usr/bin/python3
import pandas as pd
import matplotlib
from matplotlib import pyplot as plt
from matplotlib.ticker import StrMethodFormatter

font = {'family' : 'normal',
        'weight' : 'bold',
        'size'   : 16}

matplotlib.rc('font', **font)
matplotlib.rcParams['pdf.fonttype'] = 42
matplotlib.rcParams['ps.fonttype'] = 42
protocols = list(reversed(['Eth Mainnet', 'Eth Ropsten', 'Eth Rinkeby', 'Swarm', 'Musicoin', 'Pirl', 'LES', 'Eth Classic', 'Ubiq', 'Other']))
counts = list(reversed([173, 14.958, 14.944, 6.579, 5.235, 4.976, 4.431, 3.974, 3.685, 10.446]))

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