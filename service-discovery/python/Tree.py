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



def get_entropy(labels, base=2):
  value,counts = np.unique(labels, return_counts=True)
  #print("Counts:", counts)
  #print("value", value, "counts", counts, "Max entropy", entropy([1]*len(counts), base=base))
  #efficiency - entropy, deviced by max entropy
  return entropy(counts, base=base)

class Tree:	
    
    def __init__(self,  exp=False):
        self.comparators = [128, 64, 32, 16, 8, 4, 2, 1]
        self.root = TreeNode()
        self.max_score = 0
        self.exp = exp

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
        #if(balanced_score == 0 or (math.log(score/balanced_score, 10)) < 1):
        #    return 1
        #else:
        #    return (math.log(score/balanced_score, 10))
            

    def add(self, addr):
        result = self.addRecursive(self.root, addr, 0)
        self.root = result[0]
        score = result[1]
        highest_score = result[2]

        #if(highest_score > self.max_score):
        #    self.max_score = highest_score

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
        #print("Depth", depth, "Score", score)
        #current.increment()
        #print("Increment counter to ", current.getCounter())
	    
        if(depth < 32):
            #print("Octet: ",  addr.split('.')[int(depth/8)])
            octet = int(addr.split('.')[int(depth/8)])
            comparator = self.comparators[int(depth % 8)]
            result = None
            if((octet & comparator) == 0):
                #print("Going towards 0")
                result = self.tryAddRecursive(current.zero, addr, depth + 1)
                #current.zero = result[0]
            else:
                #print("Going towards 1")
                result = self.tryAddRecursive(current.one, addr, depth + 1)
                #current.one = result[0]; 

            score += result[1]
        else:
            pass
            #print("Reached depth ", depth, " going back.")
        
        return (current, score)
	    
    def addRecursive(self, current, addr, depth):
        if (current == None):
            current = TreeNode()
        
        if(self.exp == True):
            score = current.getCounter() * pow(2, depth)
        else:
            score = current.getCounter()
        #print("Depth", depth, "Score", score)
        current.increment()
        highest_score = current.getCounter() * pow(2, depth)
        #print("Increment counter to ", current.getCounter())
	    
        if(depth < 32):
            #print("Octet: ",  addr.split('.')[int(depth/8)])
            octet = int(addr.split('.')[int(depth/8)])
            comparator = self.comparators[int(depth % 8)]
            result = None
            if((octet & comparator) == 0):
                #print("Going towards 0")
                result = self.addRecursive(current.zero, addr, depth + 1)
                current.zero = result[0]
            else:
                #print("Going towards 1")
                result = self.addRecursive(current.one, addr, depth + 1)
                current.one = result[0]; 

            score += result[1]
            highest_score += result[2]
        else:
            pass
            #print("Reached depth ", depth, " going back.")
        
        return (current, score, highest_score)

    def removeRecursive(self, current, addr, depth):
        if (current == None):
            current = TreeNode()
        
        if(self.exp == True):
            score = current.getCounter() * pow(2, depth)
        else:
            score = current.getCounter()
        #print("Depth", depth, "Score", score)
        current.decrement()
        #print("Increment counter to ", current.getCounter())
	    
        if(depth < 32):
            #print("Octet: ",  addr.split('.')[int(depth/8)])
            octet = int(addr.split('.')[int(depth/8)])
            comparator = self.comparators[int(depth % 8)]
            result = None
            if((octet & comparator) == 0):
                #print("Going towards 0")
                result = self.removeRecursive(current.zero, addr, depth + 1)
                current.zero = result[0]
            else:
                #print("Going towards 1")
                result = self.removeRecursive(current.one, addr, depth + 1)
                current.one = result[0]; 

            score += result[1]
        else:
            pass
            #print("Reached depth ", depth, " going back.")
        
        return (current, score)
    
