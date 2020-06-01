# Peersim implementation of Service discovery

This archive is composed of:
- the doc/ directory containing the javadoc-generated documentation
- the src/ directory containing the simulator source
- the example/ directory containing example configuration files
- peersim-1.0.5.jar, a java archive containing the compiled bytecode
- jep-2.3.0.jar and djep-1.0.0.jar, a Java Mathematical Expression Parser 
  library from Singular Systems (http://www.singularsys.com/jep/).
  JEP is needed by the peersim core; it is distributed under
  the GNU General Public License (GPL)


java -cp "peersim-1.0.5.jar:jep-2.3.0.jar:djep-1.0.0.jar" peersim.Simulator example/config-example1.txt

(PeerSim website)[http://peersim.sourceforge.net/]

## Kademlia 
We've added a Kademlia implementation (`src/peersim/kademlia`). It should compile automatically when invoking `make`. 

To run a Kademlia example run `make run`.


