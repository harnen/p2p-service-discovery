class TreeNode:
    def __init__(self):
        self.counter = 0
        self.zero = None
        self.one = None
        self.bound = 0
        self.timestamp = 0  #timestamp for lower-bound state
    
    def getCounter(self):
        return self.counter

    def getBound(self):
        return self.bound

    def getTimestamp(self):
        return self.timestamp

    def increment(self):
        self.counter += 1
        return self.counter

	    
    def decrement(self):
        self.counter -= 1
        return self.counter

#a structure to calculate diversity between 1 and many IP addresses in O(1)
#score is a similarity metric between the IP being inserted and IPs already in the tree
#1 - the IP is exactly the same (shared all the bits) as all the IPs already in the table
#0 - the IP is completely different (doesn't share a single bit) from IPs already in the table
class Tree:	 
    def __init__(self,  exp=False):
        self.comparators = [128, 64, 32, 16, 8, 4, 2, 1]
        self.root = TreeNode()
        self.max_score = 0
        self.exp = exp
        self.currTime = 0 # current simulation time (used for lower bound calculation)
    
    #get score returned by add without modifying the tree
    def tryAdd(self, addr, time):
        self.currTime = time #update current time
        result = self.tryAddRecursive(self.root, addr, 0)
        score = result[1]
        effBound = result[2]
        if(self.exp is True):
            balanced_score = (self.root.getCounter()) * 32
            max_score = -(self.root.getCounter()) * (1 - pow(2, 33))
        else:
            balanced_score = self.root.getCounter()
            max_score = self.root.getCounter()*32
            score -= self.root.getCounter()
        print("TryAdd final score: ", score, " Balanced score: ", balanced_score, "Max score:", max_score)
        if max_score == 0:
            max_score = 1

        return (score/max_score, effBound)
    
    # find the node corresponding to the  most similar (i.e., longest-prefix match) 
    # ip address in the Trie and add the lower-bound state to that node.
    def updateBound(self, addr, bound, currTime):
        current = self.root
        prev = None
        for depth in range(0, 32):
            prev = current
            octet = int(addr.split('.')[int(depth/8)])
            comparator = self.comparators[int(depth % 8)]
            
            diff = self.currTime - current.timestamp 
            effBound = current.bound - diff
            if effBound < bound:
                # update lower-bound
                current.bound = effBound 
                current.timestamp = self.currTime

            if((octet & comparator) == 0):
                current = current.zero
            else:
                current = current.one
            
            if current is None:
                break

        if current is None:
            # prev is the longest-prefix match
            prev.bound = bound
            prev.timestamp = currTime
        else:
            current.bound = bound
            current.timestamp = currTime
        print('updating lower bound for ip: ', addr, ' with bound: ', bound, ' and time: ', currTime, ' current is ', current)

    #add an IP to the tree
    def add(self, addr):
        result = self.addRecursive(self.root, addr, 0)
        self.root = result[0]
        score = result[1]
        highest_score = result[2]

        # Onur: the returned score is never used by any caller (tryAdd() computes scores)
        # FIXME remove the below
        if(self.exp == True):
            balanced_score = (self.root.getCounter()) * 32
            max_score = -(self.root.getCounter()) * (1 - pow(2, 33))
        else:
            print("Hello")
            balanced_score = self.root.getCounter()
            max_score = self.root.getCounter()*32
        print("Add final score: ", score, " Balanced score: ", balanced_score, "Max score:", max_score)#, "New max score:", self.max_score)

        if(max_score == 0):
            return 0

        return score/max_score

    def remove(self, addr):
        result = self.removeRecursive(self.root, addr, 0)
        self.root = result[0]
        score = result[1]
        if(self.exp == True):
            balanced_score = (self.root.getCounter()) * 32
            max_score = -(self.root.getCounter()) * (1 - pow(2, 33))
        else:
            balanced_score = self.root.getCounter()
            max_score = self.root.getCounter()*32
        print("Add final score: ", score, " Balanced score: ", balanced_score, "Max score:", max_score)

        if(max_score == 0):
            return 0

        return score/max_score
            
	
    def tryAddRecursive(self, current, addr, depth):
        if (current == None):
            return (None, 0, None)

        if(self.exp == True):
            score = current.getCounter() * pow(2, depth)
        else:
            score = current.getCounter()
        bound = current.getBound()
        timestamp = self.currTime
        timeDiff = self.currTime - timestamp
        effectiveBound = max(0, bound - timeDiff)
	    #IPv4 address has 32 bits
        #would be 128 for IPv6
        if(depth < 32):
            octet = int(addr.split('.')[int(depth/8)])
            comparator = self.comparators[int(depth % 8)]
            result = None
            if((octet & comparator) == 0):
                result = self.tryAddRecursive(current.zero, addr, depth + 1)
            else:
                result = self.tryAddRecursive(current.one, addr, depth + 1)

            score += result[1]
            if result[2] is not None:
                effectiveBound = result[2]
        else:
            #Reached depth max deapth - going back.
            pass
        
        return (current, score, effectiveBound)
	    
    def addRecursive(self, current, addr, depth, bound=0, timestamp=0):
        if (current == None):
            current = TreeNode()
            # copy parent's lower bound and timestamp
            current.bound = bound
            current.timestamp = timestamp
        
        if(self.exp == True):
            score = current.getCounter() * pow(2, depth)
        else:
            score = current.getCounter()

        current.increment()
        highest_score = current.getCounter() * pow(2, depth)
	    #IPv4 address has 32 bits
        #would be 128 for IPv6
        if(depth < 32):
            octet = int(addr.split('.')[int(depth/8)])
            comparator = self.comparators[int(depth % 8)]
            result = None
            if((octet & comparator) == 0):
                result = self.addRecursive(current.zero, addr, depth + 1, current.bound, current.timestamp)
                current.zero = result[0]
            else:
                result = self.addRecursive(current.one, addr, depth + 1, current.bound, current.timestamp)
                current.one = result[0]; 

            score += result[1]
            highest_score += result[2]
        else:
            #Reached depth max deapth - going back.
            pass
        
        return (current, score, highest_score)

    def removeRecursive(self, current, addr, depth):
        if (current == None):
            current = TreeNode()
        
        if(self.exp == True):
            score = current.getCounter() * pow(2, depth)
        else:
            score = current.getCounter()
        current.decrement()
	    
        if(depth < 32):
            octet = int(addr.split('.')[int(depth/8)])
            comparator = self.comparators[int(depth % 8)]
            result = None
            if((octet & comparator) == 0):
                result = self.removeRecursive(current.zero, addr, depth + 1)
                current.zero = result[0]
            else:
                result = self.removeRecursive(current.one, addr, depth + 1)
                current.one = result[0]; 

            score += result[1]
        else:
            #Reached depth max deapth - going back.
            pass       
        
        return (current, score)
