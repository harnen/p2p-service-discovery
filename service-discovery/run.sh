if [ "$#" -ne 1 ]; then
    echo "Provide the config file name to run"
    exit 1
fi

java -Xmx5000m -cp lib/djep-1.0.0.jar:lib/jep-2.3.0.jar:target/service-discovery-1.0-SNAPSHOT.jar:/home/harnen/.m2/repository/org/graphstream/gs-core/2.0/gs-core-2.0.jar:/home/harnen/.m2/repository/org/graphstream/pherd/1.0/pherd-1.0.jar:/home/harnen/.m2/repository/org/graphstream/mbox2/1.0/mbox2-1.0:/home/harnen/.m2/repository/org/graphstream/gs-ui-swing/2.0/gs-ui-swing-2.0.jar -ea peersim.Simulator $1
