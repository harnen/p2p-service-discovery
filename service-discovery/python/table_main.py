import simpy
from table import *
import matplotlib.pyplot as plt
from threshold import *


runtime = 100
ad_lifetime = 300
capacity = 200


tables = []
tables.append(SimpleTable(simpy.Environment(), capacity, ad_lifetime))
#tables.append(DiversityThreshold(simpy.Environment(), capacity, ad_lifetime, topicThresholds = {"t1" : 0.5, "t2" : 0.5},  ipThresholds = {8: capacity/4, 16: capacity/8, 24: capacity/16, 32 : capacity/32}, entropyLimit = 0.8))
tables.append(DiversityTable(simpy.Environment(), capacity, ad_lifetime))
for table in tables:
    table.load('./workloads/regular_size100_dist2.csv')
    table.display(99)
    table.run(runtime)
plt.show()
    #env.process(table.display(100))
    #env.run(until=run_time)
#quit()
#tab0 = [1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 5, 6, 7, 8, 9, 9]
#tab1 = [1, 0, 1, 0, 1, 0, 1, 0]
#tab2 = [1, 1, 1, 1, 0, 0, 0, 0, 2]
#tab3 = [1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0]
#print("Entropy", get_entropy(tab0))
#print("Entropy", get_entropy(tab1))
#print("Entropy", get_entropy(tab2))
#print("Entropy", get_entropy(tab3))
#quit()
#tree = Tree()
#in_file = open('./workloads/ips.txt', "r")
#counter = 0
#x = []
#y = []
#for line in in_file:
#    if counter > 100:
#        break
#    addr = line.rstrip()
#    addr_tab = addr.split('.')
#    print(">>>", addr, "->", '{0:08b}'.format(int(addr_tab[0])) + '.' + '{0:08b}'.format(int(addr_tab[1])) + '.' + '{0:08b}'.format(int(addr_tab[2])) + '.'+ '{0:08b}'.format(int(addr_tab[3])))
    #print("tryAppend:", tree.tryAdd(addr), "append", tree.add(addr))
#    y.append(tree.add(addr))
#    x.append(counter)
#    counter += 1
#print(y)
#fig, ax = plt.subplots()
#ax.plot(x, y)
#ax.axhline(y = 1, color = 'r', linestyle = '-')
#ax.set_yscale('log')
#print(sum(y)/len(y))
#plt.show()
#tree.add("127.0.0.1")
#tree.add("127.0.0.1")
#print("~~~~~~~~~~~~~~~~~~~~~~")
