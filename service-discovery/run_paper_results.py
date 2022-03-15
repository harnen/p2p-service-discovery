import sys
from pprint import pformat
import os


def change_key(file, key, val):
    #make sure we don't overwrite an original config file
    assert(file not in config_files.values())
    regex = "\"s@^" + key + " .*@" + key + " " + str(val) + "@g\"" 
    result = os.system("sed -i " + regex + " " + file)
    #make sure the command succeeded
    assert(result == 0)

def run_sim(config_file):
    os.system("java -Xmx200000m -cp ./lib/djep-1.0.0.jar:lib/jep-2.3.0.jar:target/service-discovery-1.0-SNAPSHOT.jar:lib/gs-core-2.0.jar:lib/pherd-1.0.jar:lib/mbox2-1.0.jar:lib/gs-ui-swing-2.0.jar -ea peersim.Simulator " + config_file + "> /dev/null 2> /dev/null")

#turn a running config into a folder name
def params_to_dir(params):
    result = ""
    for param in params:
        result += "_" + param + "-" + str(params[param])
    return result
#set all the parameters in the config file
def set_params(config_file, out_dir, params):
    os.system("dos2unix " + config_file)
    change_key(config_file, "control.3.rangeExperiment", out_dir)
    for param in params:
        key = features[param]['keyword']
        value = params[param]
        change_key(config_file, key, value)


features = {'size': {'default': 3000, 'keyword': 'SIZE', 'vals':[1000, 2000, 3000,  4000, 5000]},
            'topic': {'default': 20, 'keyword': 'control.0traffic.maxtopicnum', 'vals':[1, 20, 40,  60, 80]}}
#protocols to test
config_files = {'discv5': './config/discv5ticket.cfg', 
                'dhtticket': './config/discv5dhtticket.cfg', 
                'dhtnoticket': './config/discv5dhtnoticket.cfg', 
                'discv4' : './config/noattackdiscv4.cfg'}
    
result_dir = './python_logs'

def main() -> int:
    os.system('rm -rf ' + result_dir)

    for protocol in config_files.keys():
        print("Running ", protocol)
        in_config = config_files[protocol]
        #used to avoid running the same config multiple times
        already_run = set()
        params = {}
        for main_feature in features.keys():
            for val in features[main_feature]['vals']:
                params[main_feature] = val
                #set default values for the rest
                for feature in features.keys():
                    if feature != main_feature:
                        params[feature] = features[feature]['default']
                print("params:", params)
                #by default you can't have a set of dictionaries
                #pformat turn a dictionary into a string that can be added to the set
                if(pformat(params) not in already_run):
                    already_run.add(pformat(params))
                    out_dir = result_dir + "/" + protocol + "/" + params_to_dir(params) + "/"
                    os.system('mkdir -p ' + out_dir)
                    out_config = out_dir + 'config.txt'
                    os.system('cp ' + in_config + " " + out_config)
                    set_params(out_config, out_dir, params)
                    run_sim(out_config)

if __name__ == '__main__':
    sys.exit(main())  # next section explains the use of sys.exit