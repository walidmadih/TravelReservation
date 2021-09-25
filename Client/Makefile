all: java.policy compile-client

java.policy: FORCE
	@echo "Creating client java policy"
	@echo "grant codebase \"file:`pwd`/\" {" > java.policy
	@echo "permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

../Server/Interface.jar: ../Server/Server/Interface/*.java
	make -C ../Server/ Interface.jar

compile-client: ../Server/Interface.jar
	javac -cp ../Server/Interface.jar Client/*.java

clean:
	rm -f Client/*.class
	rm -f ../Server/Interface.jar
	rm -f java.policy
