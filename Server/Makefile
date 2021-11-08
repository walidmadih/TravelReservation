all: java.policy compile-server-rmi

java.policy: FORCE
	@echo "Creating server java policy"
	@echo "grant codebase \"file:`pwd`/\" {" > java.policy
	@echo "permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

RMIInterface.jar: Server/Interface/* Server/Common/RMHashMap.java Server/LockManager/TransactionLockObject.java
	@echo "Compiling RMI server interface"
	javac Server/Interface/*.java
	javac Server/Common/RMHashMap.java
	javac Server/LockManager/TransactionLockObject.java
	jar cvf RMIInterface.jar Server/Interface/*.class Server/Common/RMHashMap.class 'Server/LockManager/TransactionLockObject$$LockType.class'

compile-server-rmi: Server/RMI/*.java Server/Interface/* Server/Common/*.java Server/LockManager/*.java
	javac Server/RMI/*.java Server/Interface/*.java Server/Common/*.java Server/LockManager/*.java

clean:
	rm -f Server/Interface/*.class Server/Common/*.class Server/RMI/*.class Server/LockManager/*.class
	rm -f RMIInterface.jar
	rm -f java.policy 
