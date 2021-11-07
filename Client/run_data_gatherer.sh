# Usage: ./run_client.sh [<server_hostname> [<server_rmiobject> <server_port> <throughput> <client_count>]]

java -Djava.security.policy=java.policy -cp ../Server/RMIInterface.jar:. Client.DataGatherer.DataGatherer $1 $2 $3 $4 $5
