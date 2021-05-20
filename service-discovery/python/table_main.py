
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
    if len(sys.argv) > 1:
        input_file = sys.argv[1]
    else:
        input_file = './workloads/regular_size100_dist2.csv'

def select_results_with_default_params(df, exclude = None):
    restore_default()
    params = ['ad_lifetime', 'capacity', 'honest_count', 'honest_count', 'malicious_count']
    defaults = {}
    defaults['ad_lifetime'] = ad_lifetime
    defaults['capacity'] = capacity
    defaults['input_file'] = input_file
    defaults['honest_count'] = honest_count
    defaults['malicious_count'] = malicious_count


    if(exclude != None):
        params.remove(exclude)
    print(params, 'params')
    for p in params:
        df = df.loc[df[p] == defaults[p]]
    print("df excluding", exclude)
    print(df)
    quit()
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
    table = DiversityTable(capacity, ad_lifetime)
    table.load(input_file)
    #table.display(runtime - 1)
    table.add_stats(runtime-1, stats)
    table.run(runtime)


def generate_input_file():
    global attack, honest_size
    if(attack == 'none'):
        generate_regular(size = honest_size, output_filename = 'input.csv')

def run_all():
    stats = {}
    stats['table'] = []
    stats['input_file'] = []
    stats['malicious_count'] = []
    stats['honest_count'] = []
    stats['occupancy_total'] = []
    #stats['malicious_total'] = []
    #stats['honest_total'] = []
    stats['total_count'] = []
    stats['capacity'] = []
    stats['ad_lifetime'] = []

    capacities = [10, 100]


    attack = ['none', 'spam', 'topic']
    honest_size = [1, 2, 5, 10]


    restore_default()
    global input_file, capacity
    for i in input_files:
        input_file = i
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
    print(df)
    
    fig, ax = plt.subplots(figsize=(10, 4))
    plot_feature(ax, df, 'total_count', 'occupancy_total', '#registrants', 'avg occupancy')
    plt.show()

runtime = 1000000

#run_all()
analyze()