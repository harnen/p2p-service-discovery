
from table import *
import matplotlib.pyplot as plt
from threshold import *
from generate_table_workloads import *
from matplotlib.lines import Line2D
import sys
import pandas as pd

def restore_default():
    global ad_lifetime, input_file, capacity, size, malicious_rate, occupancy_power, ip_id_power, topic_power, base_multiplier, attacker_ip_num, attacker_id_num
    ad_lifetime = 3000
    capacity = 200
    size = 300
    malicious_rate = 20
    occupancy_power = 10
    ip_id_power = 0.1
    topic_power = 10
    attacker_ip_num = 10
    attacker_id_num = 10
    base_multiplier = 10



def select_results_with_default_params(df, exclude = None):
    restore_default()
    params = ['ad_lifetime', 'capacity', 'size', 'malicious_rate', 'occupancy_power', 'attacker_ip_num', 'attacker_id_num', 'ip_id_power', 'topic_power', 'base_multiplier']
    defaults = {}
    defaults['ad_lifetime'] = ad_lifetime
    defaults['capacity'] = capacity
    defaults['size'] = size
    defaults['malicious_rate'] = malicious_rate
    defaults['occupancy_power'] = occupancy_power
    defaults['ip_id_power'] = ip_id_power
    defaults['topic_power'] = topic_power
    defaults['attacker_ip_num'] = attacker_ip_num
    defaults['attacker_id_num'] = attacker_id_num
    defaults['base_multiplier'] = base_multiplier
    

    if(exclude != None):
        params.remove(exclude)
    print('params:', params)
    for p in params:
        print("p:", p)
        print("defaults[p]:", defaults[p])
        df = df.loc[df[p] == defaults[p]]
        print(df)
    print("df excluding", exclude)
    print(df)
    #quit()
    return df

    
def plot_feature(ax, df, label, y, x_title, y_title, key_suffix = None, color='blue'):
    colors = ['red', 'green', 'blue', 'orange', 'black']#['0.1', '0.5', '0.8']
    styles = ['solid', 'dashed', 'dotted', 'dashdot']
    counter = 0
    attacks = []
    for key, group in df.groupby('attack'):
        attacks.append(key)
        print("Key:", key)
        print(group)
        group_specific = select_results_with_default_params(group, label)
        print(group_specific)
        print("x_old", group_specific[label])
        print("y_old", group_specific[y])
        #sort points
        x_new, y_new = zip(*sorted(zip(group_specific[label], group_specific[y])))
        print("x_new", x_new)
        print("y_new", y_new)
        ax.plot(x_new, y_new, linestyle = styles[counter%len(styles)], linewidth = 5, c=color)
        ax.scatter(x_new, y_new, c = color, linewidth = 5)
        
        #ax.plot(group_specific[label], group_specific[y], label=key, linewidth = 5)
        counter += 1
    ax.set_xlabel(x_title)
    ax.set_ylabel(y_title)
    ax.spines['right'].set_visible(False)
    ax.spines['top'].set_visible(False)
    custom_lines = []
    for i in range(0, len(attacks)):
        custom_lines.append(Line2D([0], [0], linestyle = styles[i%len(styles)], lw=4))

    ax.legend(custom_lines, attacks)

    #plt.tight_layout()
    #plt.savefig('./output/'+ x_title + y_title +'.png')



def run(stats):
    #for attack in ['none']:
    for attack in ['none', 'spam', 'topic_popular', 'topic_unpopular']:
        filename = generate_input_file(attack)
        table = DiversityTable(capacity, ad_lifetime, occupancy_power = occupancy_power, ip_id_power = ip_id_power, topic_power = topic_power, base_multiplier = base_multiplier)
        table.load(filename)
        stats['attack'].append(attack)
        stats['malicious_rate'].append(malicious_rate)
        stats['attacker_ip_num'].append(attacker_ip_num)
        stats['attacker_id_num'].append(attacker_id_num)
        stats['base_multiplier'].append(base_multiplier)
        stats['input'].append(filename)
        table.add_stats(runtime-1, stats)
        table.run(runtime)

def run_single():
    filename = generate_input_file('spam')
    table = DiversityTable(capacity, ad_lifetime, occupancy_power = occupancy_power, ip_id_power = ip_id_power, topic_power = topic_power, base_multiplier = base_multiplier)
    table.load(filename)
    table.display(runtime - 1)
    table.run(runtime)
    plt.show()

counter = 0
def generate_input_file(attack):
    global size, counter
    filename = 'input' + str(counter) + '.csv'
    if(attack == 'none'):
        generate_regular(size = size, output_filename = filename)
    elif(attack == 'spam'):
        generate_spam_topic(size = size, attacker_ip_num = attacker_ip_num, attacker_id_num = attacker_id_num, rate_normal = 1.0, rate_attack = malicious_rate, output_filename = filename)
    elif(attack == 'topic_popular'):
        generate_attack_topic(size = size, topic_to_attack = 't1', attacker_ip_num = attacker_ip_num, attacker_id_num = attacker_id_num, rate_normal = 1.0, rate_attack = malicious_rate, output_filename = filename)
    elif(attack == 'topic_unpopular'):
        generate_attack_topic(size = size, topic_to_attack = 't12', attacker_ip_num = attacker_ip_num, attacker_id_num = attacker_id_num, rate_normal = 1.0, rate_attack = malicious_rate, output_filename = filename)
    else:
        print("Unknown attack", attack)
        quit(-1)
    counter += 1
    return filename

def run_all():
    stats = {}
    stats['table'] = []
    stats['malicious_rate'] = []
    stats['size'] = []
    stats['occupancy_total'] = []
    stats['malicious_occupancy_total'] = []
    stats['honest_occupancy_total'] = []
    stats['capacity'] = []
    stats['ad_lifetime'] = []
    stats['occupancy_power'] = []
    stats['ip_id_power'] = []
    stats['topic_power'] = []
    stats['attack'] = []
    stats['input'] = []
    stats['attacker_ip_num'] = []
    stats['attacker_id_num'] = []
    stats['base_multiplier'] = []

    capacities = [100, 200, 300, 400, 500, 1000, 2000, 3000]

    sizes = [100, 200, 400, 600, 800, 1000, 1500, 2000, 3000, 3500]
    occupancy_powers = [3, 4, 5, 6, 7, 8, 9, 10]
    ip_id_powers = [0.01, 0.05, 0.1, 0.2, 1]
    topic_powers = [1, 5, 10, 20, 50]
    attacker_ip_nums = [4, 10, 20, 30, 40, 50, 60, 70, 80, 90]
    attacker_id_nums = [4, 10, 20, 30, 40, 50, 60, 70, 80, 90]
    base_multipliers = [1, 5, 10, 20, 30, 40, 50]
    malicious_rates = [1, 3, 5, 7, 9, 10, 15, 20]


    restore_default()
    global capacity, size, occupancy_power, attacker_ip_num, attacker_id_num, ip_id_power, topic_power, base_multiplier, malicious_rate
    for i in sizes:
        size = i
        run(stats)


    restore_default()
    for i in capacities:
        capacity = i
        run(stats)
        
    restore_default()
    for i in malicious_rates:
        malicious_rate = i
        run(stats)

    restore_default()
    for i in occupancy_powers:
        break
        occupancy_power = i
        run(stats)

    restore_default()
    for i in attacker_ip_nums:
        attacker_ip_num = i
        run(stats)
    
    restore_default()
    for i in attacker_id_nums:
        attacker_id_num = i
        run(stats)

    restore_default()
    for i in attacker_id_nums:
        attacker_id_num = i
        attacker_ip_num = i
        run(stats)

    restore_default()
    for i in ip_id_powers:
        break
        ip_id_power = i
        run(stats)
    
    restore_default()
    for i in topic_powers:
        break
        topic_power = i
        run(stats)
    
    restore_default()
    for i in base_multipliers:
        break
        base_multiplier = i
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
    plot_feature(ax, df, 'size', 'occupancy_total', '#registrants', 'avg occupancy', color='b')
    plot_feature(ax, df, 'size', 'honest_occupancy_total', '#registrants', 'avg occupancy', '_honest', color='g')
    plot_feature(ax, df, 'size', 'malicious_occupancy_total', '#registrants', 'avg occupancy', '_malicious', color='r')

    fig, ax = plt.subplots(figsize=(10, 4))
    plot_feature(ax, df, 'capacity', 'occupancy_total', 'capacity', 'avg occupancy', color='b')
    plot_feature(ax, df, 'capacity', 'honest_occupancy_total', 'capacity', 'avg occupancy', color='g')
    plot_feature(ax, df, 'capacity', 'malicious_occupancy_total', 'capacity', 'avg occupancy', color='r')

    fig, ax = plt.subplots(figsize=(10, 4))
    plot_feature(ax, df, 'occupancy_power', 'occupancy_total', 'Occupancy power', 'avg occupancy', color='b')
    plot_feature(ax, df, 'occupancy_power', 'honest_occupancy_total', 'Occupancy power', 'avg occupancy', color='g')
    plot_feature(ax, df, 'occupancy_power', 'malicious_occupancy_total', 'Occupancy power', 'avg occupancy', color='r')

    fig, ax = plt.subplots(figsize=(10, 4))
    plot_feature(ax, df, 'attacker_id_num', 'occupancy_total', '#attacker IDs', 'avg occupancy', color='b')
    plot_feature(ax, df, 'attacker_id_num', 'honest_occupancy_total', '#attacker IDs', 'avg occupancy', '_honest', color='g')
    plot_feature(ax, df, 'attacker_id_num', 'malicious_occupancy_total', '#attacker IDs', 'avg occupancy', '_malicious', color='r')


    fig, ax = plt.subplots(figsize=(10, 4))
    plot_feature(ax, df, 'attacker_ip_num', 'occupancy_total', '#attacker IPs', 'avg occupancy', color='b')
    plot_feature(ax, df, 'attacker_ip_num', 'honest_occupancy_total', '#attacker IPs', 'avg occupancy', '_honest', color='g')
    plot_feature(ax, df, 'attacker_ip_num', 'malicious_occupancy_total', '#attacker IPs', 'avg occupancy', '_malicious', color='r')

    fig, ax = plt.subplots(figsize=(10, 4))
    plot_feature(ax, df, 'ip_id_power', 'occupancy_total', 'id_ip_power', 'avg occupancy', color='b')
    plot_feature(ax, df, 'ip_id_power', 'honest_occupancy_total', 'id_ip_power', 'avg occupancy', '_honest', color='g')
    plot_feature(ax, df, 'ip_id_power', 'malicious_occupancy_total', 'id_ip_power', 'avg occupancy', '_malicious', color='r')

    fig, ax = plt.subplots(figsize=(10, 4))
    plot_feature(ax, df, 'malicious_rate', 'occupancy_total', 'Malicious rate', 'avg occupancy', color='b')
    plot_feature(ax, df, 'malicious_rate', 'honest_occupancy_total', 'Malicious rate', 'avg occupancy', '_honest', color='g')
    plot_feature(ax, df, 'malicious_rate', 'malicious_occupancy_total', 'Malicious rate', 'avg occupancy', '_malicious', color='r')

    fig, ax = plt.subplots(figsize=(10, 4))
    plot_feature(ax, df, 'topic_power', 'occupancy_total', 'topic_power', 'avg occupancy', color='b')
    plot_feature(ax, df, 'topic_power', 'honest_occupancy_total', 'topic_power', 'avg occupancy', '_honest', color='g')
    plot_feature(ax, df, 'topic_power', 'malicious_occupancy_total', 'topic_power', 'avg occupancy', '_malicious', color='r')

    fig, ax = plt.subplots(figsize=(10, 4))
    plot_feature(ax, df, 'base_multiplier', 'occupancy_total', 'base_multiplier', 'avg occupancy', color='b')
    plot_feature(ax, df, 'base_multiplier', 'honest_occupancy_total', 'base_multiplier', 'avg occupancy', '_honest', color='g')
    plot_feature(ax, df, 'base_multiplier', 'malicious_occupancy_total', 'base_multiplier', 'avg occupancy', '_malicious', color='r')

    plt.show()

def special(input_file = 'dump.csv'):
    df = pd.read_csv(input_file)
    df.drop(df.columns[[0]], axis=1, inplace=True)
    df.drop_duplicates(inplace=True)

    y = {}
    attacker_id_nums = [4, 10, 20, 30, 40, 50, 60, 70, 80, 90]
    for attack in ['none', 'spam', 'topic_popular', 'topic_unpopular']:
        y[attack] = {}
        for feature in ['occupancy_total', 'honest_occupancy_total', 'malicious_occupancy_total']:
            y[attack][feature] = []
            for i in attacker_id_nums:
                print(attack, i)
                print(df.loc[(df['attack'] == attack) & (df['attacker_id_num'] == i) & (df['attacker_ip_num'] == i), feature])
                print(df.loc[(df['attack'] == attack) & (df['attacker_id_num'] == i) & (df['attacker_ip_num'] == i), feature].iat[0])
                y[attack][feature].append(df.loc[(df['attack'] == attack) & (df['attacker_id_num'] == i) & (df['attacker_ip_num'] == i), feature].iat[0])
    print(y)
    fig, ax = plt.subplots(figsize=(10, 4))
    styles = ['solid', 'dashed', 'dotted', 'dashdot']
    colors = ['b', 'g', 'r']
    counter_style = 0
    for attack in ['none', 'spam', 'topic_popular', 'topic_unpopular']:
        counter_feature = 0
        for feature in ['occupancy_total', 'honest_occupancy_total', 'malicious_occupancy_total']:
            ax.plot(attacker_id_nums, y[attack][feature], linestyle = styles[counter_style%len(styles)], linewidth = 5, c=colors[counter_feature%len(colors)])
            ax.scatter(attacker_id_nums, y[attack][feature], c = colors[counter_feature%len(colors)], linewidth = 5)
            counter_feature += 1
        counter_style +=1

    ax.set_xlabel("#Attacker IDs/IPs")
    ax.set_ylabel("Avg occupancy")
    ax.spines['right'].set_visible(False)
    ax.spines['top'].set_visible(False)
    custom_lines = []
    for i in range(0, len(['none', 'spam', 'topic_popular', 'topic_unpopular'])):
        custom_lines.append(Line2D([0], [20], linestyle = styles[i%len(styles)], lw=4))

    ax.legend(custom_lines, ['none(solid)', 'spam(dashed)', 'topic_popular(dotted)', 'topic_unpopular(dashdotted)'])
    plt.show()

runtime = 200000

restore_default()
#run_single()

run_all()
analyze()
#special('attacks.csv')