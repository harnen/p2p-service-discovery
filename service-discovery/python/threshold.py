from table import *

class DiversityThreshold(Table):

    def __init__(self, env, capacity, ad_lifetime, topicThresholds=None, ipThresholds=None, entropyLimit=None):
        self.trie = {} # map prefix to request
        self.minEntropy = entropyLimit
        self.topicLimits = {}
        self.prefixLimits = {}
        
        self.setTopicThresholds(topicThresholds)
        self.setIpThresholds(ipThresholds)
        super().__init__(env, capacity, ad_lifetime)

    def setTopicThresholds(self, topicPercentages):
        if len(topicPercentages) == 0:
            self.topicLimits = []

        for topic in topicPercentages.keys():
            self.topicLimits[topic] = topicPercentages[topic]

    def setIpThresholds(self, ipThresholds):
        if len(ipThresholds) == 0:
            self.prefixLimits = []

        for prefixLen in ipThresholds:
            self.prefixLimits[prefixLen] = ipThresholds[prefixLen]

    def setEntropyLimit(self, limit):
        self.minEntropy = limit

    # remove expired registrations
    def update_trie(self, time):
        for prefix in self.trie.keys():
            reqs = self.trie[prefix] 
            deleted_reqs = []
            for req in reqs:
                if req['expire'] > self.env.now:
                    deleted_reqs.append(req)

            for req in deleted_reqs:
                reqs.remove(req)

    def get_topic_waiting_time(self, topic, time):
        if len(self.topicLimits) == 0:
            return 0

        topic_reqs = [req for req in self.table.values() if req['topic'] == topic]

        util = 1.0 * len(topic_reqs) / self.capacity
        if (util < self.topicLimits[topic]):
            return 0

        topic_expirations = [req['expire'] for req in self.table.values() if req['topic'] == topic]
        min_expiration = min(topic_expirations)

        return min_expiration - time

    def get_IP_waiting_time(self, iP, time):
        if len(self.prefixLimits) == 0:
            return 0

        self.update_trie(time)
        components = iP.split(".")
        prefixes = []
        prefix = ""
        # compute all the /8, /16, /24, /32 prefixes
        for c in components:
            if len(prefix) == 0:
                prefix += c
            else:
                prefix += "."
                prefix += c
            prefixes.append(prefix)


        #print('Prefixes = ', prefixes) 

        min_expiration = float('inf')
        prefixLen = 8
        for prefix in prefixes:
            reqs = []
            if prefix in self.trie:
                reqs = self.trie[prefix]
            if (len(reqs) >= self.prefixLimits[prefixLen]):
                expiration_times = [r['expire'] for r in reqs]
                min_r = min(expiration_times)
                if min_r < min_expiration:
                    min_expiration = min_r
            prefixLen += 8

        if min_expiration == float('inf'):
            return 0

        return min_expiration - time

    def get_nodeId_entropy_waiting_time(self, req, time):
        current_reqs = [req for req in self.table.values()]
        sorted_reqs = sorted(current_reqs, key=lambda k: k['expire']) 
        
        if len(sorted_reqs) == 0:
            return 0

        last_element = None

        while len(sorted_reqs) > 0:
            current_ids = [x['id'] for x in sorted_reqs]
            current_ids.append(req['id'])
            entropy = get_entropy(current_ids)

            #print('Sorted Reqs: ', sorted_reqs)
            #print('Entropy is', entropy)

            last_element = sorted_reqs.pop(0)
            
            if entropy > self.minEntropy:
                break

        return last_element['expire'] - time

    def get_waiting_time(self, req):
        iP = req['ip']
        iD = req['id']
        topic = req['topic']
        
        time = self.env.now
        ip_waiting_time = self.get_IP_waiting_time(iP, time)
        topic_waiting_time = self.get_topic_waiting_time(topic, time)
        nodeID_waiting_time = 0 # self.get_nodeId_entropy_waiting_time(req, time) XXX

        waiting_time = max([ip_waiting_time, topic_waiting_time, nodeID_waiting_time])

        if waiting_time > 0:
            print('Waiting time due to diversity: ', waiting_time + 1)
            return waiting_time + 1

        if len(self.table) > self.capacity:
            waiting_time = list(self.table.items())[0][1]['expire'] - time + 1 
            print('Waiting time due to capacity: ', waiting_time)
            return waiting_time
        else:
            self.add_req_to_trie(req)
            print("Accepted ticket")
            return 0


    def add_req_to_trie(self, req):
        iP = req['ip']
        components = iP.split(".")
        prefix = ""
        
        for c in components:
            if len(prefix) == 0:
                prefix += c
            else:
                prefix += "."
                prefix += c
            
            if prefix in self.trie.keys():
                reqs = self.trie[prefix]
                reqs.append(req)
            else:
                self.trie[prefix] = [req]



