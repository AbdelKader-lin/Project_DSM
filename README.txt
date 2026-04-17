# Distributed Shared Memory

The code is organized as a Maven project. In order to build and run it locally, follow the steps below:

0. Run locally a RabbitMQ server
1. Compile:
    mvn clean package
2. Add all dependencies to a file (which will be used as classpath after):
   mvn dependency:build-classpath -Dmdep.outputFile=cp.txt
3. In order to run a node, execute in a new terminal: 
   java -cp "target/classes:$(cat cp.txt)" dsm.Main <nodeId> <totalNodes> <globalAddresses>
Node IDs start with 0.
4. Once all the <totalNodes> instances of the dsm.Main are launched, use the DSM node command line interface for read/write requests:
   "Use one of the following commands:
   r <address> (read <value> from <address>)
   w <address> <value> (write <value> to <address>)
   ra <address> (asynchronous read <value> from <address>)
   wa <address> <value> (asynchronous write <value> to <address>)
   print (print the local memory snapshot)
