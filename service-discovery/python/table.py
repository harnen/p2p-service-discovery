#!/usr/bin/python3
import csv
from matplotlib import cm
import matplotlib.pyplot as plt
import math
import abc
import numpy as np
from scipy.stats import entropy
import copy

class Table(metaclass=abc.ABCMeta):
    def __init__(self, env, capacity, ad_lifetime, interval=1):
        self.table = {}
        self.workload = {}
        self.env = env
        self.capacity = capacity
        self.ad_lifetime = ad_lifetime
        self.ad_ids = 0
        self.admission_times = []
        self.occupancies = []
        self.occupancies_by_attackers = []
        self.returns = []
        self.interval = interval

    def load(self, file):
        counter = 0
        with open(file, newline='') as csvfile:
            reader = csv.DictReader(csvfile, delimiter=',', quotechar='|')
            for row in reader:
                if 'attack' in row.keys():
                    row['attack'] = int(row['attack'])
                if 'time' in row.keys():
                    row['arrived'] = float(row['time'])
                else:
                    row['arrived'] = counter
                row['returned'] = 0
                print(row)
                self.workload[counter] = row
                if 'time' in row.keys():
                    self.env.process(self.new_request(row, float(row['time'])))
                else:
                    self.env.process(self.new_request(row, counter * self.interval))
                counter += 1
        
    def run(self, runtime):
        self.env.process(self.report_occupancy())
        self.env.run(until=runtime)
        

    def report_occupancy(self):
        yield self.env.timeout(1)
        self.occupancies.append(len(self.table) / self.capacity)
        attacker_entries = [req for req in self.table.values() if req['attack'] == 1]
        self.occupancies_by_attackers.append(len(attacker_entries) / self.capacity)
        self.env.process(self.report_occupancy())

    def scatter(self, values, title, ax = None, color_map = None):
        if(ax == None):
            fig, ax = plt.subplots()
        cmap = cm.get_cmap('Spectral')
        vals = list(set(values))
        #print("vals:",   vals)
        #required for consistent coloring across graphs
        if(color_map == None):
            if(len(vals) > 0):
                step = 1/len(vals)
            color_map = {}
            for item in vals:
                color_map[item] = cmap(vals.index(item) * step)

        row_length = int(math.sqrt(len(values)))
        colors = []
        x = []
        y = []
        counter = 0
        for entry in values:
            colors.append(color_map[entry])
            x.append(counter % row_length)
            y.append(int(counter/row_length))
            counter += 1
        
        ax.scatter(x, y, c=colors)
        ax.set_title(title)
        print("Frequency of", title, {x:values.count(x) for x in values})
        return color_map

    def display(self, delay):
        print("display, env", self.env)
        self.env.process(self.display_body(delay))

    def display_body(self, delay):
        yield self.env.timeout(delay)
        figure, axis = plt.subplots(3, 3)
        ips_table = [x['ip'] for x in self.table.values()]
        ips_workload = [x['ip'] for x in self.workload.values()]
        ids_table = [x['id'] for x in self.table.values()]
        ids_workload = [x['id'] for x in self.workload.values()]
        topics_table = [x['topic'] for x in self.table.values()]
        topics_workload = [x['topic'] for x in self.workload.values()]
        
        color_map = self.scatter(ips_workload, "IPs in the workload" + str(delay), axis[1, 0])
        self.scatter(ips_table, "IPs in the table at" + str(delay), axis[0, 0], color_map)
        color_map = self.scatter(ids_workload, "IDs in the workload" + str(delay), axis[1, 1])
        self.scatter(ids_table, "IDs in the table" + str(delay), axis[0, 1], color_map)
        color_map = self.scatter(topics_workload, "Topics in the workload" + str(delay), axis[1, 2])
        self.scatter(topics_table, "Topics in the table" + str(delay), axis[0, 2], color_map)
        

        axis[2, 0].plot(range(0, len(self.admission_times)), self.admission_times)
        axis[2, 0].set_title("Waiting times")
        axis[2, 1].plot(range(0, len(self.occupancies)), self.occupancies, range(0, len(self.occupancies_by_attackers)), self.occupancies_by_attackers)
        axis[2, 1].set_title("Occupancy over time")
        axis[2, 2].plot(range(0, len(self.returns)), self.returns)
        axis[2, 2].set_title("Returns")
        figure.suptitle(type(self).__name__)
        print("Admission times:", self.admission_times)


    
    def log(self, *arg):
        print("[", self.env.now, "s]",  sep="", end='')
        print(*arg)
    
    @abc.abstractmethod
    def get_waiting_time(self, req):
        pass

    def remove_ad(self, ad_id, delay):
        yield self.env.timeout(delay)
        self.log("Removing", self.table[ad_id])
        self.table.pop(ad_id)

    def new_request(self, req, delay):
        yield self.env.timeout(delay)
        waiting_time = self.get_waiting_time(req)
        self.log("-> new request arrived:", req)
        print("waiting time:", waiting_time)
        waiting_time = int(waiting_time)
        if(waiting_time == 0):
            self.log("Admitting right away")
            req['expire'] = self.env.now + self.ad_lifetime
            self.table[self.ad_ids] = req
            self.env.process(self.remove_ad(self.ad_ids, self.ad_lifetime))
            self.ad_ids += 1
            self.admission_times.append(self.env.now - req['arrived'])
            self.returns.append(req['returned'])
            
            #may the registrant re-register after expiration time
            new_req = copy.deepcopy(req)
            new_req['expire'] = 0
            new_req['arrived'] = self.env.now + self.ad_lifetime
            new_req['returned'] = 0
            self.log("Will attempt to re-register at:", self.env.now + self.ad_lifetime)
            self.env.process(self.new_request(new_req, self.ad_lifetime))
        else:
            req['returned'] += 1
            self.log("Need to wait for", waiting_time)
            self.env.process(self.new_request(req, waiting_time))
    

class SimpleTable(Table):
    def get_waiting_time(self, req):
        if(len(self.table) >= self.capacity):
            return list(self.table.items())[0][1]['expire'] - self.env.now + 1
        else:
            return 0
    
    
class DiversityTable(Table):
    def __init__(self, env, capacity, ad_lifetime):
        super().__init__(env, capacity, ad_lifetime)
        #self.tree = Tree()
        self.ip_modifiers = {}
        self.id_modifiers = {}
        self.topic_modifiers = {}
    
    def get_ip_modifier(self, ip):
        current_ips = [x['ip'] for x in self.table.values()]
        return current_ips.count(ip) + 1
        #return self.tree.tryAdd(ip)
    
    def get_id_modifier(self, id):
        current_ids = [x['id'] for x in self.table.values()]
        return current_ids.count(id) + 1

    def get_topic_modifier(self, topic):
        current_topics = [x['topic'] for x in self.table.values()]
        return current_topics.count(topic) + 1


    def get_waiting_time(self, req):
        base_waiting_time = self.ad_lifetime / self.capacity
        
        topic_modifier = self.get_topic_modifier(req['topic'])
        id_modifier = self.get_id_modifier(req['id'])
        ip_modifier = self.get_ip_modifier(req['ip'])

        needed_time = base_waiting_time * topic_modifier * id_modifier * ip_modifier
        returned_time = max(0, needed_time - (self.env.now - req['arrived']))
        #if(returned_time < 1):
        #    returned_time = 0
        #    self.tree.add(req['ip'])
        
        #just for stats
        self.ip_modifiers[self.env.now] = ip_modifier
        self.id_modifiers[self.env.now] = id_modifier
        self.topic_modifiers[self.env.now] = topic_modifier
        if (len(self.table) >= self.capacity):
            self.log("Table is full returning time", self.ad_lifetime)
            return self.ad_lifetime

        print("needed_time:", needed_time, "base:", base_waiting_time, "ip_modifier:", ip_modifier, "id_modifier:", id_modifier, "topic_modifier:", topic_modifier)
        print("returning:", returned_time, "now:", self.env.now, "arrived:", req['arrived'])
        return returned_time
        
        #return base_waiting_time * topic_modifier * id_modifier * ip_modifier
    def report_modifiers(self, delay):
        yield self.env.timeout(delay)
        figure, ax = plt.subplots()
        ax.plot(self.ip_modifiers.keys(), self.ip_modifiers.values(), label='ip_modifier')
        ax.plot(self.id_modifiers.keys(), self.id_modifiers.values(), label='id_modifier')
        ax.plot(self.topic_modifiers.keys(), self.topic_modifiers.values(), label='topic_modifier')
        ax.legend()
        ax.set_title("Diversity Table Modifiers")

    def display_body(self, delay):
        self.env.process(self.report_modifiers(delay))
        return super().display_body(delay)
    #    yield self.env.timeout(0)
    #    print("before super().display_body, delay:", delay)
        
    #    print("after super().display_body")
        #quit()


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
  #print("Counts:", counts)
  #print("value", value, "counts", counts, "Max entropy", entropy([1]*len(counts), base=base))
  #efficiency - entropy, deviced by max entropy
  return entropy(counts, base=base)

class Tree:	
    
    def __init__(self):
        self.comparators = [128, 64, 32, 16, 8, 4, 2, 1]
        self.root = TreeNode()

    def tryAdd(self, addr):
        result = self.tryAddRecursive(self.root, addr, 0)
        score = result[1]
        balanced_score = (self.root.getCounter()) * 32
        max_score = -(self.root.getCounter()) * (1 - pow(2, 33))
        #print("TryAdd final score: ", score, " Balanced score: ", balanced_score, "Max score:", max_score)
        if(balanced_score == 0 or (math.log(score/balanced_score, 10)) < 1):
            return 1
        else:
            return (math.log(score/balanced_score, 10))
            

    def add(self, addr):
        result = self.addRecursive(self.root, addr, 0)
        self.root = result[0]
        score = result[1]
        balanced_score = (self.root.getCounter()-1) * 32
        max_score = -(self.root.getCounter()-1) * (1 - pow(2, 33))
        print("Add final score: ", score, " Balanced score: ", balanced_score, "Max score:", max_score)
        #quit()
        return score
        #if(balanced_score == 0 or (math.log(score/balanced_score, 10)) < 1):
        #    return 1
        #else:
        #    return (math.log(score/balanced_score, 10))
            
	
    def tryAddRecursive(self, current, addr, depth):
        if (current == None):
            current = TreeNode()
        
        score = current.getCounter() * pow(2, depth)
        #print("Depth", depth, "Score", score)
        #current.increment()
        #print("Increment counter to ", current.getCounter())
	    
        if(depth < 32):
            #print("Octet: ",  addr.split('.')[int(depth/8)])
            octet = int(addr.split('.')[int(depth/8)])
            comparator = self.comparators[int(depth % 8)]
            result = None
            if((octet & comparator) == 0):
                #print("Going towards 0")
                result = self.tryAddRecursive(current.zero, addr, depth + 1)
                #current.zero = result[0]
            else:
                #print("Going towards 1")
                result = self.tryAddRecursive(current.one, addr, depth + 1)
                #current.one = result[0]; 

            score += result[1]
        else:
            pass
            #print("Reached depth ", depth, " going back.")
        
        return (current, score)
	    
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
