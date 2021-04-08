#!/usr/bin/python3
import csv
from matplotlib import cm
import matplotlib.pyplot as plt
import math
import abc
import numpy as np
from scipy.stats import entropy


class Table(metaclass=abc.ABCMeta):
    def __init__(self, env, capacity, ad_lifetime):
        self.table = {}
        self.workload = {}
        self.env = env
        self.capacity = capacity
        self.ad_lifetime = ad_lifetime
        self.ad_ids = 0

    def load(self, file):
        counter = 0
        with open(file, newline='') as csvfile:
            reader = csv.DictReader(csvfile, delimiter=',', quotechar='|')
            for row in reader:
                row['arrived'] = counter
                print(row)
                self.workload[counter] = row
                self.env.process(self.new_request(row, counter))
                counter += 1
    
    def scatter(self, values, title):
        fig, ax = plt.subplots()
        cmap = cm.get_cmap('Spectral')
        ips = list(set(values))
        ip_step = 1/len(ips)
        ip_row_length = int(math.sqrt(len(values)))
        ip_colors = []
        ip_x = []
        ip_y = []
        counter = 0
        for entry in values:
            ip_colors.append(cmap(ips.index(entry) * ip_step))
            ip_x.append(counter % ip_row_length)
            ip_y.append(int(counter/ip_row_length))
            counter += 1
        
        ax.scatter(ip_x, ip_y, c=ip_colors)
        ax.set_title(title)
        print("Frequency of", title, {x:values.count(x) for x in values})

    def display(self, delay):
        yield self.env.timeout(delay)
        self.scatter([x['ip'] for x in self.table.values()], "IPs in the table at" + str(delay))
        self.scatter([x['ip'] for x in self.workload.values()], "IPs in the workload" + str(delay))
        self.scatter([x['id'] for x in self.table.values()], "IDs in the table" + str(delay))
        self.scatter([x['id'] for x in self.workload.values()], "IDs in the workload" + str(delay))
        self.scatter([x['topic'] for x in self.table.values()], "Topics in the table" + str(delay))
        self.scatter([x['topic'] for x in self.workload.values()], "Topicss in the workload" + str(delay))
        plt.show()
    
    def log(self, *arg):
        print("[", self.env.now, "s]",  sep="", end='')
        print(*arg)
    
    @abc.abstractmethod
    def get_waiting_time(self, req, delay):
        pass

    def remove_ad(self, ad_id, delay):
        yield self.env.timeout(delay)
        self.log("Removing", self.table[ad_id])
        self.table.pop(ad_id)

    def new_request(self, req, delay):
        yield self.env.timeout(delay)
        self.log("new request arrived:", req)
        waiting_time = self.get_waiting_time(req)
        if(waiting_time == 0):
            req['expire'] = self.env.now + self.ad_lifetime
            self.table[self.ad_ids] = req
            self.env.process(self.remove_ad(self.ad_ids, self.ad_lifetime))
            self.ad_ids += 1
        else:
            self.env.process(self.new_request(req, waiting_time))
    

class SimpleTable(Table):
    def get_waiting_time(self, req):
        if(len(self.table) > self.capacity):
            return list(self.table.items())[0][1]['expire'] - self.env.now + 1
        else:
            return 0
    
    
class DiversityTable(Table):
    def get_waiting_time(self, req, delay):
        current_topics = [x['topic'] for x in self.table.values()]
        current_topic_entropy = get_entropy(current_topics)
        new_topic_entropy = get_entropy(current_topics.append(req['topic']))
        topic_change = (new_topic_entropy-current_topic_entropy)/current_topic_entropy
        
        current_ids = [x['id'] for x in self.table.values()]
        current_id_entropy = get_entropy(current_ids)
        new_id_entropy = get_entropy(current_ids.append(req['id']))
        id_change = (new_id_entropy-current_id_entropy)/current_id_entropy



class TreeNode:
    def __init__(self):
        self.counter = 0
        self.zero = None
        self.one = None

	    
    def getCounter(self):
        return self.counter

    def increment(self):
        self.counter += 1
        return self.counter

	    
    def decrement(self):
        self.counter -= 1
        return self.counter


def get_entropy(labels, base=2):
  value,counts = np.unique(labels, return_counts=True)
  #print("value", value, "counts", counts, "Max entropy", entropy([1]*len(counts), base=base))
  #efficiency - entropy, deviced by max entropy
  return entropy(counts, base=base)

class Tree:	
    
    def __init__(self):
        self.comparators = [128, 64, 32, 16, 8, 4, 2, 1]
        self.root = TreeNode()

    def add(self, addr):
        result = self.addRecursive(self.root, addr, 0)
        self.root = result[0]
        score = result[1]
        balanced_score = (self.root.getCounter()-1) * 32
        max_score = -(self.root.getCounter()-1) * (1 - pow(2, 33))
        print("Final score: ", score, " Balanced score: ", balanced_score, "Max score:", max_score)
        if(balanced_score > 0):
            return score/balanced_score
        else:
            return 0
	
	
    def addRecursive(self, current, addr, depth):
        if (current == None):
            current = TreeNode()
        
        score = current.getCounter() * pow(2, depth)
        #print("Depth", depth, "Score", score)
        current.increment()
        #print("Increment counter to ", current.getCounter())
	    
        if(depth < 32):
            #print("Octet: ",  addr.split('.')[int(depth/8)])
            octet = int(addr.split('.')[int(depth/8)])
            comparator = self.comparators[int(depth % 8)]
            result = None
            if((octet & comparator) == 0):
                #print("Going towards 0")
                result = self.addRecursive(current.zero, addr, depth + 1)
                current.zero = result[0]
            else:
                #print("Going towards 1")
                result = self.addRecursive(current.one, addr, depth + 1)
                current.one = result[0]; 

            score += result[1]
        else:
            pass
            #print("Reached depth ", depth, " going back.")
        
        return (current, score)
