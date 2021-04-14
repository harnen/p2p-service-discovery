from numpy import random
import csv

def generate_regular(size = 100, zipf_distribution = 2):
    output_filename = './workloads/regular_size' + str(size) + '_dist' + str(zipf_distribution) + '.csv'
    #get ips/ids from ethereum repo
    ip_file = open('./workloads/ips.txt', "r")
    id_file = open('./workloads/ids.txt', "r")
    topics = random.zipf(a=zipf_distribution, size=size)#for topics
    with open(output_filename, 'w') as output_file:
        fieldnames = ['id', 'ip', 'topic']
        dict_writer = csv.DictWriter(output_file, fieldnames=fieldnames)
        dict_writer.writeheader()
        for i in range(0, size):
            record = {}
            ip = ip_file.readline().rstrip()
            id = id_file.readline().rstrip()
            if(not ip or not id):
                print("Not enough IPs/IDs in the files")
                exit(1)
            record['id'] = id
            record['ip'] =ip
            record['topic'] = 't' + str(topics[i])
            #print(record)
            dict_writer.writerow(record)
    print("Generated regular workload in", str(output_filename))



zipf_distribution = 2
size = 100

generate_regular(size = size, zipf_distribution=zipf_distribution)

