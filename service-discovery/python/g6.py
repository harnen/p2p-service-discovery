import numpy as np
import seaborn as sn
import matplotlib.pyplot as plt
import sys


fig, ax = plt.subplots()
#x = [1, 100, 150, 200, 250, 300, 350, 400, 450, 500, 550, 600, 650, 700, 750, 800, 850, 900, 950, 1000]
x = [1, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000]
x_plot  = [i / 10 for i in x]
print(x_plot)

#log_g6_200.cfg
for dirname in sys.argv[1:]:
    print(dirname)
    y = []
    for i in x:
        sum = 0
        counter = 0
        filename = dirname+'log_g6_'+str(i)+'.cfg'
        print("filename", filename)
        
        registrations = i
        #print("registrations", registrations)
        with open(filename) as f:
            for line in f:
                if('after consulting' in line):
                    num = int(line.split(' ')[13])
                    #print("Found num:", num)
                    counter += 1
                    sum += num
        y.append(sum/counter)
        print("counter", counter, "sum", sum, "sum/counter", sum/counter)
    ax.plot(x, y)

ax.set_title("Topic lookup overhead")
ax.set_xlabel("% of nodes registering for the topic")
ax.set_ylabel("Average lookup hop count")
ax.set_ylim(0)
plt.show()
