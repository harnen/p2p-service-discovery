class TreeNode:
    def __init__(self):
        self.counter = 0
        self.zero = None
        self.one = None
    
    def getCounter(self):
        return self.counter

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
    
    #get score returned by add without modifying the tree
    def tryAdd(self, addr):
        result = self.tryAddRecursive(self.root, addr, 0)
        score = result[1]
        if(self.exp == True):
            balanced_score = (self.root.getCounter()) * 32
            max_score = -(self.root.getCounter()) * (1 - pow(2, 33))
        else:
            balanced_score = self.root.getCounter()
            max_score = self.root.getCounter()*32
            score -= self.root.getCounter()
        print("TryAdd final score: ", score, " Balanced score: ", balanced_score, "Max score:", max_score)
        return score/max_score
            
    #add an IP to the tree
    def add(self, addr):
        result = self.addRecursive(self.root, addr, 0)
        self.root = result[0]
        score = result[1]
        highest_score = result[2]

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
            current = TreeNode()
        if(self.exp == True):
            score = current.getCounter() * pow(2, depth)
        else:
            score = current.getCounter()
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
        else:
            #Reached depth max deapth - going back.
            pass
        
        return (current, score)
	    
    def addRecursive(self, current, addr, depth):
        if (current == None):
            current = TreeNode()
        
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
                result = self.addRecursive(current.zero, addr, depth + 1)
                current.zero = result[0]
            else:
                result = self.addRecursive(current.one, addr, depth + 1)
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
    
