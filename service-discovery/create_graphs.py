import sys
from python.nsdi_plots import *

INDIR_BASE = './python_logs/'
OUTDIR_BASE = './plots/'

INDIR_BASE = os.path.abspath(INDIR_BASE)
OUTDIR_BASE = os.path.abspath(OUTDIR_BASE)
#if len(sys.argv) < 2:
#    print('Usage: ', sys.argv[0], ' <Path_to_Log_files> <OPTIONAL: Path_to_output_dir>' )
#    sys.exit(1) 

#LOGDIR = sys.argv[1]
#if len(sys.argv) > 2:
#    OUTDIR = sys.argv[2]



#createPerLookupOperationStats(LOGDIR)
#plotPerLookupOperation()
for simulation_type in ['benign', 'attack']:
#for simulation_type in ['attack']:
    INDIR = INDIR_BASE + "/" + simulation_type + "/"
    OUTDIR = OUTDIR_BASE + "/" + simulation_type + "/"
    
    print("#######################" + simulation_type + "#######################")
    print('Will read logs from', INDIR)
    print('Plots will be saved in ', OUTDIR)

    if not os.path.exists(OUTDIR):
        os.makedirs(OUTDIR)

    os.chdir(INDIR)
    
    createPerNodeStats(".")
    if simulation_type == 'benign':
        plotPerNodeStats(OUTDIR, simulation_type)
    else :
        plotPerNodeStats(OUTDIR, simulation_type,graphType = GraphType.bar)

