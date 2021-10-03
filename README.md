# comp512-project

To run the RMI resource manager:

```
cd Server/
./run_server.sh [<rmi_name>] [<port>]# starts a single ResourceManager
./run_servers.sh [<port>] # convenience script for starting multiple resource managers
./run_middleware.sh <server_1_host> <server_2_host> <server_3_host> [<port>]
```

To run the TCP client:

```
cd Client
./run_client.sh [<server_hostname> [<server_rmi_name>] [<server_port>]]
```
