from table import get_entropy
import matplotlib.pyplot as plt
import math
import random
import pandas as pd

def plot_multi(data, cols=None, spacing=.1, **kwargs):

    from pandas import plotting

    # Get default color style from pandas - can be changed to any other color list
    if cols is None: cols = data.columns
    if len(cols) == 0: return
    #colors = getattr(getattr(plotting, '_style'), '_get_standard_colors')(num_colors=len(cols))
    colors = ['green', 'blue', 'orange', 'red']

    # First axis
    ax = data.loc[:, cols[0]].plot(label=cols[0], color=colors[0], style= '-', **kwargs)
    ax.set_ylabel(ylabel=cols[0])
    ax.set_xlabel('Items/Users')
    lines, labels = ax.get_legend_handles_labels()

    print(cols)
    counter = 1
    for col in cols[1:]:
        print("index")
        ax_new = ax.twinx()
        ax_new.spines['right'].set_position(('axes', counter))
        data.loc[:, cols[counter]].plot(ax=ax_new, label=cols[counter], style= '--',  color=colors[counter % len(colors)])
        ax_new.set_ylabel(ylabel=cols[counter])
        counter += 1
        line, label = ax_new.get_legend_handles_labels()
        lines += line
        labels += label

    
    #return ax
    # Multiple y-axes
    #ax_new = ax.twinx()
    #ax_new.spines['right'].set_position(('axes', 1))
    #data.loc[:, cols[0]].plot(ax=ax_new, label=cols[0], style= '--',  color=colors[0 % len(colors)])
    #ax_new.set_ylabel(ylabel=cols[0])

        # Proper legend position
    

    ax.legend(lines, labels, loc=0)

    return ax

def get_entropy_modifier(topics, topic):
        current_topic_entropy = get_entropy(topics)
        new_topics = topics + [topic]
        new_topic_entropy = get_entropy(new_topics)
        
        if((len(topics) - 1) == 0):
            topic_modifier = 1
        elif(new_topic_entropy == 0):
            topic_modifier = 2
        else:
            topic_modifier = current_topic_entropy/new_topic_entropy

        print("old->new_entropy:", current_topic_entropy, "->", new_topic_entropy, "new - old:", new_topic_entropy - current_topic_entropy, "new/old:", new_topic_entropy/current_topic_entropy, "modifier:", topic_modifier)
        topic_modifier = max(topic_modifier, 1)
        return topic_modifier

def get_polynomial_modifier(topics, topic):
    count = topics.count(topic)
    return math.pow(count, 1.2)

def get_random_modifier(topics, topic):
    return random.randint(3, 9)

def test_topic_modifier():
    input = ['t1', 't2','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t2','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1','t1']
    modifiers = {}
    modifiers['n'] = list(range(0, len(input)))
    modifiers['entropy'] = []
    modifiers['polynomial'] = []
    modifiers['random'] = []
    topics = []
    for item in input:
        modifiers['entropy'].append(get_entropy_modifier(topics, item))
        modifiers['polynomial'].append(get_polynomial_modifier(topics, item))
        modifiers['random'].append(get_random_modifier(topics, item))
        topics.append(item)
    print("Input:", input)
    print("Modifiers:", modifiers)
    
    df = pd.DataFrame(modifiers)
    df.set_index('n', inplace=True)
    print(df)
    plot_multi(df, figsize=(6, 3))
    plt.show()
    #figure, ax = plt.subplots()
    #ax.plot(range(0, len(modifiers['entropy'])), modifiers['entropy'], label='entropy')
    #ax.plot(range(0, len(modifiers['polynomial'])), modifiers['polynomial'], label='polynomial')
    #ax.legend()
    #ax.set_title("Modifiers")
    #plt.show()

test_topic_modifier()