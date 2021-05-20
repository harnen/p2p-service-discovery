
from table import *
import matplotlib.pyplot as plt
from threshold import *
from generate_table_workloads import *
import sys
import pandas as pd

def restore_default():
    global ad_lifetime, input_file, capacity, attack, honest_count, malicious_count
    ad_lifetime = 3000
    capacity = 100
    attack = 'none'
    honest_count = 100
    malicious_count = 0



def select_results_with_default_params(df, exclude = None):
    restore_default()
    params = ['ad_lifetime', 'capacity', 'honest_count', 'malicious_count']
    defaults = {}
    defaults['ad_lifetime'] = ad_lifetime
    defaults['capacity'] = capacity
    defaults['honest_count'] = honest_count
    defaults['malicious_count'] = malicious_count


    if(exclude != None):
        params.remove(exclude)
    print(params, 'params')
    for p in params:
        df = df.loc[df[p] == defaults[p]]
    print("df excluding", exclude)
    print(df)
    #quit()
    return df

def plot_feature(ax, df, label, y, x_title, y_title, key_suffix = None):
    colors = ['red', 'green', 'blue', 'orange', 'black']#['0.1', '0.5', '0.8']
    styles = ['solid', 'dashed', 'dashdot']
    counter = 0
    for key, group in df.groupby('table'):
        if(key_suffix != None):
            key += key_suffix
        print(key, group)
        group_specific = select_results_with_default_params(group, label)
        print(group_specific)
        print(group_specific)
        print("x_old", group_specific[label])
        print("y_old", group_specific[y])
        #sort points
        x_new, y_new = zip(*sorted(zip(group_specific[label], group_specific[y])))
        print("x_new", x_new)
        print("y_new", y_new)
        ax.plot(x_new, y_new, label=key, c = colors[counter%len(colors)], linestyle = styles[counter%len(styles)], linewidth = 5)
        ax.scatter(x_new, y_new, c = colors[counter%len(colors)], linewidth = 5)
        #ax.plot(group_specific[label], group_specific[y], label=key, linewidth = 5)
        counter += 1
    ax.set_xlabel(x_title)
    ax.set_ylabel(y_title)
    ax.spines['right'].set_visible(False)
    ax.spines['top'].set_visible(False)
    ax.legend()
    #plt.tight_layout()
    #plt.savefig('./output/'+ x_title + y_title +'.png')



def run(stats):
    filename = generate_input_file()
    table = DiversityTable(capacity, ad_lifetime)
    table.load(filename)
    #table.display(runtime - 1)
    table.add_stats(runtime-1, stats)
    table.run(runtime)


def generate_input_file():
    global attack, honest_count
    if(attack == 'none'):
        generate_regular(size = honest_count, output_filename = 'input.csv')
    return 'input.csv'

def run_all():
    stats = {}
    stats['table'] = []
    stats['malicious_count'] = []
    stats['honest_count'] = []
    stats['occupancy_total'] = []
    stats['malicious_occupancy_total'] = []
    stats['honest_occupancy_total'] = []
    stats['capacity'] = []
    stats['ad_lifetime'] = []

    capacities = [10, 100, 200, 300]

    attacks = ['none']
    honest_counts = [1, 2, 5, 10, 20, 30, 50]



    restore_default()
    global capacity, honest_count
    for i in honest_counts:
        honest_count = i
        run(stats)

    restore_default()
    for i in capacities:
        capacity = i
        run(stats)
        
    #plt.show()
    print(stats)
    df = pd.DataFrame(stats)
    print("stats", df)
    df.to_csv('dump.csv')

def analyze(input_file = 'dump.csv'):
    df = pd.read_csv(input_file)
    df.drop(df.columns[[0]], axis=1, inplace=True)
    df.drop_duplicates(inplace=True)
    #print(df)

    
    fig, ax = plt.subplots(figsize=(10, 4))
    plot_feature(ax, df, 'honest_count', 'occupancy_total', '#honest registrants', 'avg occupancy')
    plot_feature(ax, df, 'honest_count', 'honest_occupancy_total', '#honest registrants', 'avg occupancy', '_honest')
    plot_feature(ax, df, 'honest_count', 'malicious_occupancy_total', '#honest registrants', 'avg occupancy', '_malicious')

    fig, ax = plt.subplots(figsize=(10, 4))
    plot_feature(ax, df, 'capacity', 'occupancy_total', 'capacity', 'avg occupancy')


    plt.show()

runtime = 100000


run_all()
analyze()