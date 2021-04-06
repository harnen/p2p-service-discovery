#!/usr/bin/python3
import csv
import simpy
import matplotlib
from matplotlib import cm
import matplotlib.pyplot as plt
import math


#yield env.timeout(self._initial_delay)
#env.process(self.run()) - new process
#env.process(self._nodes[peer].receive(advertisement, weight))

class Table:
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
                env.process(self.new_request(row, counter))
                counter += 1


    def get_waiting_time(self, req):
        if(len(self.table) > self.capacity):
            return list(self.table.items())[0][1]['expire'] - self.env.now + 1
        else:
            return 0

    def remove_ad(self, ad_id, delay):
        yield env.timeout(delay)
        log("Removing", self.table[ad_id])
        self.table.pop(ad_id)

    def new_request(self, req, delay):
        yield env.timeout(delay)
        log("new request arrived:", req)
        waiting_time = self.get_waiting_time(req)
        if(waiting_time == 0):
            req['expire'] = self.env.now + self.ad_lifetime
            self.table[self.ad_ids] = req
            self.env.process(self.remove_ad(self.ad_ids, self.ad_lifetime))
            self.ad_ids += 1
        else:
            self.env.process(self.new_request(req, waiting_time))
    
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

    
    def display(self):
        yield env.timeout(50)
        self.scatter([x['ip'] for x in self.table.values()], "IPs in the table")
        self.scatter([x['ip'] for x in self.workload.values()], "IPs in the workload")
        self.scatter([x['id'] for x in self.table.values()], "IDs in the table")
        self.scatter([x['id'] for x in self.workload.values()], "IDs in the workload")
        self.scatter([x['topic'] for x in self.table.values()], "Topics in the table")
        self.scatter([x['topic'] for x in self.workload.values()], "Topicss in the workload")
        #ids = set(x['id'] for x in self.table.values())
        #topics = set(x['topic'] for x in self.table.values())
        plt.show()
        quit()




def log(*arg):
        print("[", env.now, "s]",  sep="", end='')
        print(*arg)


    
run_time = 1000
ad_lifetime = 300
capacity = 200
env = simpy.Environment()
counter = 1
table = Table(env, capacity, ad_lifetime)
table.load('topics.csv')
env.process(table.display())



env.run(until=run_time)