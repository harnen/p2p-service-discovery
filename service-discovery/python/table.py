#!/usr/bin/python3
import csv
from matplotlib import cm
import matplotlib.pyplot as plt
import math
import abc
import numpy as np
from scipy.stats import entropy
import copy
from random import randint
import simpy

class Table(metaclass=abc.ABCMeta):
    def __init__(self, capacity, ad_lifetime, interval=1):
        self.capacity = capacity
        self.ad_lifetime = ad_lifetime
        self.interval = interval

        self.table = {}
        self.workload = {}
        self.env = simpy.Environment()
        self.ad_ids = 0
        self.admission_times = []
        self.occupancies = {}
        self.occupancies_by_attackers = {}
        self.returns = []
        
        self.req_counter = 0
        self.pending_req = {}
        self.honest_count = 0
        self.malicious_count = 0
        self.malicious_in = {}
        self.honest_in = {}

    def load(self, file):
        counter = 0
        self.input_file = file
        with open(file, newline='') as csvfile:
            reader = csv.DictReader(csvfile, delimiter=',', quotechar='|')
            for row in reader:
                if 'attack' in row.keys():
                    row['attack'] = int(row['attack'])
                    if(row['attack'] == 0):
                        self.honest_count += 1
                    else:
                        self.malicious_count += 1

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
        #print("Honest", self.honest_count, "Malicious:", self.malicious_count)


        
    def run(self, runtime):
        self.report_occupancy()
        self.env.run(until=runtime)
        

    def report_occupancy(self):
        #yield self.env.timeout(1)
        if(len(self.occupancies) > 0):
            self.occupancies[self.env.now-1]  = list(self.occupancies.values())[-1]
        self.occupancies[self.env.now] = len(self.table) / self.capacity
        
        attacker_entries = [req for req in self.table.values() if req['attack'] == 1]
        honest_entries = [req for req in self.table.values() if req['attack'] == 0]
        if(len(self.occupancies_by_attackers) > 0):
            self.occupancies_by_attackers[self.env.now-1] = list(self.occupancies_by_attackers.values())[-1]
        self.occupancies_by_attackers[self.env.now] = len(attacker_entries) / self.capacity
        
        if(self.malicious_count > 0):
            if(len(self.malicious_in) > 0):
                self.malicious_in[self.env.now - 1] = list(self.malicious_in.values())[-1]
            self.malicious_in[self.env.now] = len(attacker_entries)/self.malicious_count
        
        if(self.honest_count > 0):
            if(len(self.honest_in) > 0):
                self.honest_in[self.env.now - 1] = list(self.honest_in.values())[-1]
            self.honest_in[self.env.now] = len(honest_entries)/self.honest_count
        #self.env.process(self.report_occupancy())

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
        #hack for the final report - remove later on as
        #it clashes with the code above
        colors = []
        for i in values:
            if i == 1:
                colors.append('r')
            else:
                colors.append('g')
        ax.scatter(x, y, c=colors)
        ax.set_title(title)
        #print("Frequency of", title, {x:values.count(x) for x in values})
        return color_map

    def display(self, delay):
        print("display, env", self.env)
        self.env.process(self.display_body(delay))

    def display_body(self, delay):
        yield self.env.timeout(delay)
        figure, axis = plt.subplots(2, 3)

        requests_table = [x['attack'] for x in self.table.values()]
        requests_workload = [x['attack'] for x in self.workload.values()]
        color_map = self.scatter(requests_workload, "Requests in the workload" , axis[1, 0])
        self.scatter(requests_table, "Requests in the table at", axis[0, 0], color_map)
        axis[0, 1].scatter([x[0] for x in self.admission_times], [x[1] for x in self.admission_times], c=get_colors([x[2] for x in self.admission_times]))
        axis[0, 1].set_title("Waiting times")
        #axis[2, 0].set_yscale('log')
        #print("!!!!!!!!!!!!!!!!!!", self.occupancies)
        axis[1, 1].plot(self.occupancies.keys(), self.occupancies.values(), color='b')
        #print("~~~~~~~~~~~~~~~~~~", self.occupancies_by_attackers)
        #quit()
        axis[1, 1].plot(self.occupancies_by_attackers.keys(), self.occupancies_by_attackers.values(), color='r')
        axis[1, 1].plot(self.occupancies.keys(), [x - y for x, y in zip(self.occupancies.values(), self.occupancies_by_attackers.values())], color='g')
        axis[1, 1].set_title("Occupancy over time")
        axis[0, 2].scatter([x[0] for x in self.returns], [x[1] for x in self.returns], c=get_colors([x[2] for x in self.returns]))
        axis[0, 2].set_title("Returns")


        axis[1, 2].plot(self.honest_in.keys(), self.honest_in.values(), c='g')
        axis[1, 2].plot(self.malicious_in.keys(), self.malicious_in.values(), c='r')
        axis[1, 2].set_title("Percentage of overal requests in the table")

        print("Occupancy_total", self.extract_total(self.occupancies))
        print("Malicious total", self.extract_total(self.occupancies_by_attackers))

    def add_stats(self, delay, stats):
        self.env.process(self.add_stats_body(delay, stats))

    def extract_total(self, dict):
        labels = []
        sizes = []
        for val in set(dict.values()):
            l = []
            for key in dict.keys():
                if(dict[key] == val):
                    l.append(key)
            interval = []
            for i in range(2, len(l)-1, 2):
                interval.append(l[i+1] - l[i])
            labels.append(val)
            sizes.append(sum(interval))
        total = 0
        for i in range(0, len(sizes)):
            total += labels[i] * (sizes[i] / sum(sizes))
        return total
    
    def add_stats_body(self, delay, stats):
        yield self.env.timeout(delay)
        total = self.extract_total(self.occupancies)
        total_malicious = self.extract_total(self.occupancies_by_attackers)
        total_honest = total - total_malicious

        stats['table'].append(type(self).__name__)
        #stats['malicious_count'].append(self.malicious_count)
        stats['size'].append(self.honest_count +self.malicious_count)
        stats['capacity'].append(self.capacity)
        stats['occupancy_total'].append(total)
        stats['malicious_occupancy_total'].append(total_malicious)
        stats['honest_occupancy_total'].append(total_honest)
        stats['ad_lifetime'].append(self.ad_lifetime)
        stats['occupancy_power'].append(self.occupancy_power)
        stats['ip_id_power'].append(self.ip_id_power)
        stats['topic_power'].append(self.topic_power)
        print("sum:", total)

    
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
        self.report_occupancy()

    def new_request(self, req, delay):
        yield self.env.timeout(delay)
        
        if('req_id' in req):
            self.log("-> old request arrived:", req)
        else:
            req['req_id'] = self.req_counter
            self.req_counter += 1
            self.log("-> new request arrived:", req)
            assert(req['req_id'] not in self.pending_req)
            self.pending_req[req['req_id']] = req

        waiting_time = self.get_waiting_time(req)
        waiting_time = int(waiting_time)
        print("waiting time:", waiting_time, len(self.table), "/", self.capacity)
        if(waiting_time == 0):
            self.log("Admitting right away")
            del self.pending_req[req['req_id']]
            req['expire'] = self.env.now + self.ad_lifetime
            self.table[self.ad_ids] = req
            self.env.process(self.remove_ad(self.ad_ids, self.ad_lifetime))
            self.ad_ids += 1
            self.admission_times.append((self.env.now, self.env.now - req['arrived'], req['attack']))
            self.returns.append((self.env.now, req['returned'], req['attack']))
            self.report_occupancy()
            
            #may the registrant re-register after expiration time
            rand_time = randint(0, 99) # add a random time btw. 0 and 99 milliseconds
            new_req = copy.deepcopy(req)
            del new_req['req_id']
            new_req['expire'] = 0
            new_req['arrived'] = self.env.now + self.ad_lifetime + rand_time
            new_req['returned'] = 0
            self.log("Will attempt to re-register at:", self.env.now + self.ad_lifetime)
            self.env.process(self.new_request(new_req, self.ad_lifetime + rand_time))
        else:
            req['returned'] += 1
            #self.log("Need to wait for", waiting_time)
            self.env.process(self.new_request(req, waiting_time))
    

class SimpleTable(Table):
    def get_waiting_time(self, req):
        if(len(self.table) >= self.capacity):
            return list(self.table.items())[0][1]['expire'] - self.env.now + 1
        else:
            return 0
    
    
class DiversityTable(Table):
    def __init__(self, capacity, ad_lifetime, amplify = 1, occupancy_power = 10, ip_id_power = 0.1, topic_power = 10, base_multiplier = 10):
        super().__init__(capacity, ad_lifetime)
        #self.tree = Tree()
        self.ip_modifiers = {}
        self.id_modifiers = {}
        self.topic_modifiers = {}
        self.amplify = amplify
        self.occupancy_power = occupancy_power
        self.ip_id_power = ip_id_power
        self.topic_power = topic_power
        self.base_multiplier = base_multiplier
    
    def get_ip_modifier(self, ip):
        current_ips = [x['ip'] for x in self.table.values()]
        return math.pow(((current_ips.count(ip))/len(self.table)), self.ip_id_power)
        #return self.tree.tryAdd(ip)
    
    def get_id_modifier(self, iD):
        current_ids = [x['id'] for x in self.table.values()]
        return math.pow(((current_ids.count(iD))/len(self.table)), self.ip_id_power)

    def get_topic_modifier(self, topic):
        current_topics = [x['topic'] for x in self.table.values()]
        return math.pow(((current_topics.count(topic))/len(self.table)), self.topic_power)


    def get_waiting_time(self, req):
        if(len(self.table) == 0):
            return 0
        waited_time = (self.env.now - req['arrived'])
        if((1-len(self.table)/self.capacity) == 0):
            print("Will be 0!")
            print("Occupancy:", len(self.table))
            print("Capacity:", self.capacity)
            quit()
        base_waiting_time = (self.base_multiplier*self.ad_lifetime)/math.pow(1-len(self.table)/self.capacity, self.occupancy_power)
        topic_modifier = self.get_topic_modifier(req['topic'])
        id_modifier = self.get_id_modifier(req['id'])
        ip_modifier = self.get_ip_modifier(req['ip'])
        self.ip_modifiers[self.env.now] = (self.env.now, ip_modifier, req['attack'])
        self.id_modifiers[self.env.now] = (self.env.now, id_modifier, req['attack'])
        self.topic_modifiers[self.env.now] = (self.env.now, topic_modifier, req['attack'])
        needed_time = base_waiting_time * max(sum([topic_modifier, id_modifier, ip_modifier]), 1/1000000)
        print("needed_time:", needed_time, "base:", base_waiting_time, "ip_modifier:", ip_modifier, "id_modifier:", id_modifier, "topic_modifier:", topic_modifier)
        print("self.occupancy_power:", self.occupancy_power, "self.base_multiplier:", self.base_multiplier, "self.ad_lifetime:", self.ad_lifetime)
        #needed_time = base_waiting_time
        return max(0, needed_time - waited_time)
    
    

    def report_modifiers(self, delay):
        yield self.env.timeout(delay)
        #print("self.topic_modifiers", self.topic_modifiers)
        #quit()
        figure, axis = plt.subplots(3, 1)
        axis[0].scatter([x[0] for x in list(self.ip_modifiers.values())], [x[1] for x in list(self.ip_modifiers.values())], c=get_colors([x[2] for x in list(self.ip_modifiers.values())]))
        axis[0].set_title("IP modifier")
        #axis[0].set_yscale('log')
        axis[1].scatter([x[0] for x in list(self.id_modifiers.values())], [x[1] for x in list(self.id_modifiers.values())], c=get_colors([x[2] for x in list(self.id_modifiers.values())]))
        axis[1].set_title("ID modifier")
        #axis[1].set_yscale('log')
        axis[2].scatter([x[0] for x in list(self.topic_modifiers.values())], [x[1] for x in list(self.topic_modifiers.values())], c=get_colors([x[2] for x in list(self.topic_modifiers.values())]))
        axis[2].set_title("Topic modifier")
        #axis[2].set_yscale('log')
        #print("Topic modifiers ys:", [x[1] for x in list(self.topic_modifiers.values())])
        #print("ip honest modifiers", list(filter(lambda x: x[2] == 0, self.ip_modifiers.values())))

        figure.suptitle("Diversity Table Modifiers")

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

#helper for bar charts
def get_widths(l):
    widths = []
    for i in range(0, len(l)-1):
        widths.append(l[i+1] - l[i])
    widths.append(widths[-1])
    return widths

def get_colors(l):
    colors = ['g', 'r', 'b']
    result = []
    for i in l:
        result.append(colors[i%len(colors)])
    return result

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

        return score
            
	
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
