import numpy as np
from sympy import *
# define what is the variable

#print(fderivative)

# get a valua of the derivate for a specific x
# let's say f'(0)
#print(fderivative.evalf(subs= {x:0}))

results = 100
for power in range(2, 20):
    x = symbols('x')
    # define the function
    f = x**power+x-1000
    # find the first derivative
    fderivative = f.diff(x)
    xn = 1
    for i in range(100):
        xn = xn - np.float(f.evalf(subs= {x:xn})) / np.float(fderivative.evalf(subs= {x:xn}))
        #print(f'The {i+1} iteration xn is {xn:.2} and f(xn) is {np.float(f.evalf(subs= {x:xn})):.2}')
    print("pow", power, "->", xn)