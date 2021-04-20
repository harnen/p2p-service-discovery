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

def generate_attack_topic(size = 100, zipf_distribution = 2, topic_to_attack = 't11', attacker_ip_num = 3, attacker_id_num=10, percentage_malicious = 0.9):
    output_filename = './workloads/attack_topic_size' + str(size) + '_dist' + str(zipf_distribution) + '.csv'
    #get ips/ids from ethereum repo
    ip_file = open('./workloads/ips.txt', "r")
    id_file = open('./workloads/ids.txt', "r")
    topics = random.zipf(a=zipf_distribution, size=size)#for topics

    honest_percentage = 1-percentage_malicious
    entry_counter =  int((honest_percentage/2) * size)
    exit_counter = int(entry_counter + (percentage_malicious * size))
    attacker_ips = []
    for i in range(0, attacker_ip_num):
        num = int(255/attacker_ip_num * i)
        ip = str(num) + "." + str(num) + "."+ str(num) + "."+ str(num)
        attacker_ips.append(ip)
    
    attacker_ids = []
    for i in range(0, attacker_id_num):
        attacker_ids.append(''.join([str(i)]*20))
    print("entry_counter:", entry_counter, "exit_counter:", exit_counter)
    print("attacker ips:", attacker_ips)
    print("attacker ids:", attacker_ids)

    with open(output_filename, 'w') as output_file:
        fieldnames = ['id', 'ip', 'topic']
        dict_writer = csv.DictWriter(output_file, fieldnames=fieldnames)
        dict_writer.writeheader()
        for i in range(0, size):
            record = {}
            if(i < entry_counter or  i >= exit_counter ):
                ip = ip_file.readline().rstrip()
                id = id_file.readline().rstrip()
                if(not ip or not id):
                    print("Not enough IPs/IDs in the files")
                    exit(1)
                topic = 't' + str(topics[i])
            else:
                ip = attacker_ips[i % attacker_ip_num]
                id = attacker_ids[i % attacker_id_num]
                topic = topic_to_attack

            record['id'] = id
            record['ip'] =ip
            record['topic'] = topic
            #print(record)
            dict_writer.writerow(record)
    print("Generated regular workload in", str(output_filename))


def generate_spam_topic(size = 100, zipf_distribution = 2, topic_to_attack = 't1', attacker_ip_num = 3, attacker_id_num=10, percentage_malicious = 0.9):
    output_filename = './workloads/spam_topic_size' + str(size) + '_dist' + str(zipf_distribution) + '.csv'
    #get ips/ids from ethereum repo
    ip_file = open('./workloads/ips.txt', "r")
    id_file = open('./workloads/ids.txt', "r")
    topics = random.zipf(a=zipf_distribution, size=size)#for topics

    honest_percentage = 1-percentage_malicious
    entry_counter =  int((honest_percentage/2) * size)
    exit_counter = int(entry_counter + (percentage_malicious * size))
    attacker_ips = []
    for i in range(0, attacker_ip_num):
        num = int(255/attacker_ip_num * i)
        ip = str(num) + "." + str(num) + "."+ str(num) + "."+ str(num)
        attacker_ips.append(ip)
    
    attacker_ids = []
    for i in range(0, attacker_id_num):
        attacker_ids.append(''.join([str(i)]*20))
    print("entry_counter:", entry_counter, "exit_counter:", exit_counter)
    print("attacker ips:", attacker_ips)
    print("attacker ids:", attacker_ids)

    with open(output_filename, 'w') as output_file:
        fieldnames = ['id', 'ip', 'topic']
        dict_writer = csv.DictWriter(output_file, fieldnames=fieldnames)
        dict_writer.writeheader()
        for i in range(0, size):
            record = {}
            if(i < entry_counter or  i >= exit_counter ):
                ip = ip_file.readline().rstrip()
                id = id_file.readline().rstrip()
                if(not ip or not id):
                    print("Not enough IPs/IDs in the files")
                    exit(1)
                topic = 't' + str(topics[i])
            else:
                ip = attacker_ips[i % attacker_ip_num]
                id = attacker_ids[i % attacker_id_num]
                topic = 't' + str(100+i)

            record['id'] = id
            record['ip'] =ip
            record['topic'] = topic
            #print(record)
            dict_writer.writerow(record)
    print("Generated regular workload in", str(output_filename))

zipf_distribution = 2
size = 10

#generate_attack_topic(size=1000)
#generate_spam_topic(size=1000)
generate_regular(size = size, zipf_distribution=zipf_distribution)

