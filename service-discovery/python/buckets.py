import numpy as np
import seaborn as sn
import matplotlib.pyplot as plt

arr = [[0]*17]*17
stats = np.array(arr)
print(stats)


with open('./log_final') as f:
    for line in f:
        if('Asked node from' in line):
            host_bucket = int(line.split(':')[1])
            received_list = line.split('>')[1].split(',')
            #print(received_list, line)
            for received in received_list[:-1]:
                #print("host", host_bucket, "received:", received)
                stats[host_bucket-240][int(received)-240] += 1
                #print(stats)


print(stats)
map = sn.heatmap(stats)
plt.ylabel("Bucket of the node being asked")
plt.xlabel("Returned nodes buckets")
plt.show()
