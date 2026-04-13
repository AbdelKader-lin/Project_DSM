import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Scanner;


public class Node {

    private String queue_name; // Queue name this node listens on
    private int ID; // Node identifier.
    private DSM_implem dsm; // DSM implementation

    // for synch part :
    // As if we split @s into stripes, then give each stripe a lock
    private static final int NUM_STRIPES = 8 ;
    private final Object[] locks = new Object[ NUM_STRIPES ] ;

    Node( int id , String q , int totalNodes ) {
        this.queue_name = q ;
        this.ID = id ;
        this.dsm = new DSM_implem( id , totalNodes ) ;
        for ( int i = 0 ; i < NUM_STRIPES ; i++ ) {
            locks[ i ] = new Object() ;
        }
    }

    public int get_ID() {
        return this.ID ;
    }

    public String get_QName() {
        return this.queue_name ;
    }

    public int getOwner( long address ) {
        return dsm.partitionOwner( address ) ;
    }

    public void printLocalMemory() {
        System.out.println("Node " + ID + " local memory: " + dsm);
    }


    private int getStripe( long address ) {
        return ( int )( address % NUM_STRIPES );
    }

    // Command to send : channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
    // Command to receive : channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });

    //
    public static void main( String[] argv ) throws Exception {

        if ( argv.length < 4 ) {
            System.out.println( "Usage : java Node <listenQueue> <id> <totalNodes> <totalAddresses>" ) ;
            return ;
        }

        String qName = argv[ 0 ] ;
        int ID = Integer.parseInt( argv[ 1 ] ) ;
        int totalNodes = Integer.parseInt( argv[ 2 ] ) ;
        int totalAddresses = Integer.parseInt( argv[ 3 ] ) ;

        Node node = new Node( ID , qName , totalNodes ) ;
        node.initializeMemory( totalAddresses ) ;

        //
        // Establish connexion
        ConnectionFactory factory = new ConnectionFactory() ;
        factory.setHost( "localhost" ) ;

        Connection connection = factory.newConnection() ; 
        Channel channel = connection.createChannel() ;
        
            
        channel.queueDeclare( node.queue_name , false , false , false , null ) ; // We declare the node's queue

        System.out.println( "Node " + node.ID + " consumes from queue " + node.queue_name ) ;
        node.printLocalMemory(); /// added by kim
        //
        // DeliverCallback : What should I do when a message arrive
        DeliverCallback deliverCallback = ( consumerTag , delivery ) -> {

            // Decode raw bytes into your Message object.
            String msgB = new String(delivery.getBody(), StandardCharsets.UTF_8);
            Message msg = Message.StringToMessage(msgB);

            // Extract message fields.
            MessageType mType = msg.get_MessageType();
            int senderID = msg.get_SenderID();
            int targetID = msg.get_TargetID();
            long address = msg.get_Address();
            String payload = msg.get_Payload();

            // ignore messages not for this node
            if (targetID != node.get_ID()) {
                return;
            }
            // read messages logic : read request -> read local memory -> send response
            if (mType == MessageType.READ) {
                int stripe = node.getStripe(address);
                byte[] localValue;
                synchronized (node.locks[stripe]) {

                System.out.println("Thread " + Thread.currentThread().getName()+ " got lock of stripe " + stripe + " adr " + address);

                localValue = node.localMemory.getOrDefault(address, 0);

                try { Thread.sleep(3000); } catch (Exception e) {} // sleep 3 sec to see if actually works :||
                System.out.println("Thread " + Thread.currentThread().getName() +" released lock of stripe " + stripe + " adr " + address);}
                System.out.println("Node " + node.get_ID() + " : READ request from node " + senderID + " for address " + address);

                Message response = new Message(
                        node.get_ID(),
                        senderID,
                        address,
                        localValue,
                        MessageType.READ_RESPONSE
                );

                channel.basicPublish("", "node" + senderID, null,
                        response.MessageToString().getBytes(StandardCharsets.UTF_8));
            }

            // write logic remote write request -> write local memory -> send response
            else if (mType == MessageType.WRITE) {
                System.out.println("Node " + node.get_ID() +" : WRITE request from node " + senderID +" for address " + address + " value = " + value);

                // int stripe = node.getStripe(address);
                // synchronized (node.locks[stripe]) {
                //     node.localMemory.put(address, value);
                // }
                int stripe = node.getStripe(address);
                System.out.println("Thread " + Thread.currentThread().getName()+ " asks for lock stripe " + stripe + " addr " + address);
                synchronized (node.locks[stripe]) {
                    node.dsm.get_Mem().put(address, data);
                }

                Message response = new Message(
                        node.get_ID(),
                        senderID,
                        address,
                        value,
                        MessageType.WRITE_RESPONSE
                );

                channel.basicPublish("", "node" + senderID, null,
                        response.MessageToString().getBytes(StandardCharsets.UTF_8));
            }

            else if (mType == MessageType.READ_RESPONSE) {
                System.out.println("Node " + node.get_ID() + " : READ answer from node " + senderID +" -> address " + address + " = " + value);
            }

            else if (mType == MessageType.WRITE_RESPONSE) {
                System.out.println("Node " + node.get_ID() + " : WRITE answer from node " + senderID +" -> address " + address + " written with value " + value);
            }
        };

        channel.basicConsume( node.queue_name , true , deliverCallback , consumerTag -> {} ) ;
        
        // We wait for the user to press enter (why gives resource leak? : because the program never ends, the connection and channel are never closed, but we can ignore it for now since it's a test program)
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("Command (read <addr> | write <addr> <value> | print): ");
            String line = scanner.nextLine();
            String[] parts = line.split("\\s+");

            if (parts[0].equalsIgnoreCase("read") && parts.length == 2) {
                int address = Integer.parseInt(parts[1]);
                int owner = node.getOwner(address);

                // local reading 
                if (owner == node.get_ID()) {
                    int stripe = node.getStripe(address);
                    int localValue;
                    // synchronized (node.locks[stripe]) {
                    //     localValue = node.localMemory.getOrDefault(address, 0);
                    // }
                    System.out.println("Thread " + Thread.currentThread().getName()+ " asks for lock stripe " + stripe + " adr " + address);
                    synchronized (node.locks[stripe]) {
                    System.out.println("Thread " + Thread.currentThread().getName()+ " got lock of stripe " + stripe + " adr " + address);

                    localValue = node.localMemory.getOrDefault(address, 0);
                    try { Thread.sleep(1000); } catch (Exception e) {}

                    System.out.println("Thread " + Thread.currentThread().getName() + " released lock of stripe " + stripe + " adr " + address);
                    }

                    System.out.println("Node " + node.get_ID() + " : LOCAL READ address " + address + " = " + localValue);
                    } else {
                    Message readMsg = new Message(
                            node.get_ID(),
                            owner,
                            address,
                            0,
                            MessageType.READ
                    );

                    System.out.println("Node " + node.get_ID() + " : REMOTE READ address " + address + " from Node " + owner);

                    channel.basicPublish("", "node" + owner, null,
                            readMsg.MessageToString().getBytes(StandardCharsets.UTF_8));
                }
            }

            else if (parts[0].equalsIgnoreCase("write") && parts.length == 3) {
                int address = Integer.parseInt(parts[1]);
                int value = Integer.parseInt(parts[2]);
                int owner = node.getOwner(address);

                // local write
                if (owner == node.get_ID()) {
                    int stripe = node.getStripe(address);
                    // synchronized (node.locks[stripe]) {
                    //     node.localMemory.put(address, value);
                    // }
                    System.out.println("Thread " + Thread.currentThread().getName()+ " asks for lock stripe " + stripe + " adr " + address);
                    synchronized (node.locks[stripe]) {
                        System.out.println("Thread " + Thread.currentThread().getName() + " got lock of stripe " + stripe + " adr " + address);

                        node.localMemory.put(address, value);
                        try { Thread.sleep(1000); } catch (Exception e) {}

                        System.out.println("Thread " + Thread.currentThread().getName()+ " released lock of stripe " + stripe + " addr " + address);
                    }
                    System.out.println("Node " + node.get_ID() + " : LOCAL WRITE address " + address + " = " + value);
                } else {
                    Message writeMsg = new Message( 
                            node.get_ID(),
                            owner,
                            address,
                            value,
                            MessageType.WRITE
                    );

                    System.out.println("Node " + node.get_ID() + " : REMOTE WRITE address " + address + " = " + value + " to Node " + owner);

                    channel.basicPublish("", "node" + owner, null,
                            writeMsg.MessageToString().getBytes(StandardCharsets.UTF_8));
                }
            }

            else if (parts[0].equalsIgnoreCase("print")) {
                node.printLocalMemory();
            }

            else {
                System.out.println("Invalid command.");
            }
        }
    }
}
