if [ "$#" -ne 1 ]; then
    echo "Provide the config file name to run"
    exit 1
fi

java -Xmx500m -cp lib/djep-1.0.0.jar:lib/jep-2.3.0.jar:target/service-discovery-1.0-SNAPSHOT.jar -ea peersim.Simulator $1
