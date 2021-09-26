# Usage: ./run_client.sh [<server_hostname> [<server_rmiobject>]]
java -cp ../Server/Interface.jar:. Client.TCPClient $1
