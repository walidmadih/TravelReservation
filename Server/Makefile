all: java.policy compile-server-middleware

java.policy: FORCE
	@echo "Creating server java policy"
	@echo "grant codebase \"file:`pwd`/\" {" > java.policy
	@echo "permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

Interface.jar: Server/Interface/*.java
	@echo "Compiling TCP Interface"
	javac Server/Interface/*.java
	jar cvf Interface.jar Server/Interface/*.class

compile-server-middleware: Interface.jar Server/Middleware/*.java Server/Common/*.java
	javac Server/Middleware/*.java Server/Interface/*.java Server/Common/*.java

clean:
	rm -f Server/Interface/*.class Server/Common/*.class Server/Middleware/*.class
	rm -f Interface.jar
	rm -f java.policy 
