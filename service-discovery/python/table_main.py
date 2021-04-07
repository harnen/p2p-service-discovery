import simpy
from table import *

run_time = 1000
ad_lifetime = 300
capacity = 200

env = simpy.Environment()
table = SimpleTable(env, capacity, ad_lifetime)
table.load('topics.csv')
env.process(table.display(100))
env.run(until=run_time)

#tab0 = [1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 5, 6, 7, 8, 9, 9]
#tab1 = [1, 0, 1, 0, 1, 0, 1, 0]
#tab2 = [1, 1, 1, 1, 0, 0, 0, 0]
#tab3 = [1, 1, 1, 1, 1, 0, 0, 0, 0, 0]
#print("Entropy", get_entropy(tab0))
#print("Entropy", get_entropy(tab1))
#print("Entropy", get_entropy(tab2))
#print("Entropy", get_entropy(tab3))
#quit()
#tree = Tree()
#tree.add("127.0.0.1")
#tree.add("127.0.0.1")
#print("~~~~~~~~~~~~~~~~~~~~~~")