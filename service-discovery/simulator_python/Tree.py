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
    
    def isLeaf(self):
        if (self.zero is None) and (self.one is None):
            return True
        return False
	    
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
    
    #get the score for an address without actually adding the addr
    def tryAdd(self, addr, time):
        self.currTime = time #update current time
        current = self.root
        score = 0
        effBound = 0
        max_score = 0
        balanced_score = 0 
        traversed = ''
        if self.root is not None:
            for depth in range(0, 32):
                parent = current
                if(self.exp == True):
                    score += current.getCounter() * pow(2, depth)
                else:
                    score += current.getCounter()
            
                octet = int(addr.split('.')[int(depth/8)])
                comparator = self.comparators[int(depth % 8)]
                if((octet & comparator) == 0):
                    current = current.zero
                    traversed += '0'
                else:
                    current = current.one
                    traversed += '1'
            
                if (current is None):
                    current = parent
                    break

            bound = current.getBound()
            print('Bound of current node: ', traversed, ' is ', bound) 
            diff = self.currTime - current.getTimestamp()
            effBound = max(0, bound - diff)
            if(self.exp is True):
                balanced_score = (self.root.getCounter()) * 32
                max_score = -(self.root.getCounter()) * (1 - pow(2, 33))
            else:
                balanced_score = self.root.getCounter()
                max_score = self.root.getCounter()*32
                # FIXME: why deduct the root counter ?
                score -= self.root.getCounter()
    
        print("TryAdd final score: ", score, " Balanced score: ", balanced_score, "Max score:", max_score)
        if max_score == 0:
            max_score = 1

        return (score/max_score, effBound)
    
    # find the node corresponding to the  most similar (i.e., longest-prefix match) 
    # ip address in the Trie and update/store the lower-bound state at that node.
    def updateBound(self, addr, bound, currTime):
        current = self.root
        prev = None
        traversed = ''
        self.currTime = currTime
        for depth in range(0, 32):
            prev = current
            octet = int(addr.split('.')[int(depth/8)])
            comparator = self.comparators[int(depth % 8)]
            if((octet & comparator) == 0):
                current = current.zero
                traversed += '0'
            else:
                current = current.one
                traversed += '1'
            
            if (current is None):
                current = prev
                break
        
        diff = self.currTime - current.getTimestamp()
        effBound = current.bound - diff
        if effBound < bound:
        # update lower-bound
            current.bound = bound
            current.timestamp = self.currTime
            print('updating lower bound for ip: ', addr, ' with bound: ', bound, ' and time: ', currTime, ' current eff bound is ', effBound, ' at current node: ', traversed)

    #add an IP to the tree
    def add(self, addr):
        current = self.root
        max_bound = 0
        score = 0
        for depth in range(0, 32):
            parent = current
            if(self.exp == True):
                score += current.getCounter() * pow(2, depth)
            else:
                score += current.getCounter()
            
            current.increment()
            octet = int(addr.split('.')[int(depth/8)])
            comparator = self.comparators[int(depth % 8)]
            if((octet & comparator) == 0):
                current = current.zero
                if (current is None):
                    current = TreeNode()
                    # propage lower-bound state to new child
                    current.bound = parent.bound
                    current.timestamp = parent.timestamp
                parent.zero = current
            else:
                current = current.one
                if (current is None):
                    current = TreeNode()
                    # propage lower-bound state to new child
                    current.bound = parent.bound
                    current.timestamp = parent.timestamp
                parent.one = current

        score += current.getCounter()
        current.increment()
        balanced_score = 0
        max_score = 0
        if(self.exp == True):
            balanced_score = (self.root.getCounter()) * 32
            max_score = -(self.root.getCounter()) * (1 - pow(2, 33))
        else:
            balanced_score = self.root.getCounter()
            max_score = self.root.getCounter()*32
        print("Add final score: ", score, " Balanced score: ", balanced_score, "Max score:", max_score)#, "New max score:", self.max_score)

        if(max_score == 0):
            return 0

        return score/max_score

    # remove the nodes with zero count and propagate their lower bound
    # state upwards and store at first node with count > 0
    def removeAndPropagateUp(self, addr, time):
        current = self.root
        parent = current
        delete = None
        depthToDelete = None
        deleteNode = None
        deleteNodeParent = None
        for depth in range(0, 32):
            current.decrement()
            if (delete is False) and (current.getCounter() == 0): # remove descendants
                delete = True
                depthToDelete = depth
                deleteNode = current
                deleteNodeParent = parent
            octet = int(addr.split('.')[int(depth/8)])
            comparator = self.comparators[int(depth % 8)]
            parent = current
            if((octet & comparator) == 0):
                current = current.zero
            else:
                current = current.one
        current.decrement()

        if delete is True and self.root.getCounter() != 0:
            maxEffBound = 0
            current = deleteNode
            # obtain the highest lower-bound state in the deleted subtree
            for depth in range(depthToDelete, 32):
                effBound = current.getBound() - (time - current.getTimestamp())
                if effBound > maxEffBound:
                    maxEffBound = effBound
                octet = int(addr.split('.')[int(depth/8)])
                comparator = self.comparators[int(depth % 8)]
                if((octet & comparator) == 0):
                    current = current.zero
                else:
                    current = current.one
                
            effBound = current.getBound() - (time - current.getTimestamp())
            if effBound > maxEffBound:
                maxEffBound = effBound

            # delete the subtree rooted at deleteNode
            if deleteNodeParent.one == deleteNode:
                deleteNodeParent.one = None
            elif deleteNodeParent.zero == deleteNode:
                deleteNodeParent.zero = None
            
            # propagate lower-bound state to deleted subtree's parent (if necessary)
            effBound = deleteNodeParent.getBound() - (time - deleteNodeParent.getTimestamp())
            if effBound < maxEffBound:
                deleteNodeParent.bound = maxEffBound
                deleteNodeParent.timestamp = time
