import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Node {

    private String queue_name;
    private int ID;
    private int totalNodes;
    private Map<Integer, Integer> localMemory;

    Node(int id, String q, int totalNodes) {
        this.queue_name = q;
        this.ID = id;
        this.totalNodes = totalNodes;
        this.localMemory = new HashMap<>();
    }

    public int get_ID() {
        return this.ID;
    }

    public String get_QName() {
        return this.queue_name;
    }

    //
    public int getOwner(int address) {
        return address % totalNodes;
    }

    public void initializeMemory(int totalAddresses) {
        for (int address = 0; address < totalAddresses; address++) {
            if (getOwner(address) == ID) {
                localMemory.put(address, address * 10); // nice visible default values
            }
        }
    }

    public void printLocalMemory() {
        System.out.println("Node " + ID + " local memory: " + localMemory);
    }

    // Command to send : channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
    // Command to receive : channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });

    //
    public static void main(String[] argv) throws Exception {

        if (argv.length < 4) {
            System.out.println("Usage : java Node <listenQueue> <id> <totalNodes> <totalAddresses>");
            return;
        }

        String qName = argv[0];
        int ID = Integer.parseInt(argv[1]);
        int totalNodes = Integer.parseInt(argv[2]);
        int totalAddresses = Integer.parseInt(argv[3]);

        Node node = new Node(ID, qName, totalNodes);
        node.initializeMemory(totalAddresses);

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
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {

            String msgB = new String(delivery.getBody(), StandardCharsets.UTF_8);
            Message msg = Message.StringToMessage(msgB);

            MessageType mType = msg.get_MessageType();
            int senderID = msg.get_SenderID();
            int targetID = msg.get_TargetID();
            int address = msg.get_Address();
            int value = msg.get_Value();

            // Ignore messages not intended for this node
            if (targetID != node.get_ID()) {
                return;
            }

            if (mType == MessageType.READ) {
                int localValue = node.localMemory.getOrDefault(address, 0);

                System.out.println("Node " + node.get_ID() + " : READ request from Node " + senderID + " for address " + address);

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

            else if (mType == MessageType.WRITE) {
                System.out.println("Node " + node.get_ID() + " : WRITE request from Node " + senderID +
                        " for address " + address + " value = " + value);

                node.localMemory.put(address, value);

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
                System.out.println("Node " + node.get_ID() + " : READ RESPONSE from Node " + senderID +
                        " -> address " + address + " = " + value);
            }

            else if (mType == MessageType.WRITE_RESPONSE) {
                System.out.println("Node " + node.get_ID() + " : WRITE RESPONSE from Node " + senderID +
                        " -> address " + address + " written with value " + value);
            }
        };

        channel.basicConsume( node.queue_name , true , deliverCallback , consumerTag -> {} ) ;
        
        // We wait for the user to press enter
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("Command (read <addr> | write <addr> <value> | print): ");
            String line = scanner.nextLine();
            String[] parts = line.split("\\s+");

            if (parts[0].equalsIgnoreCase("read") && parts.length == 2) {
                int address = Integer.parseInt(parts[1]);
                int owner = node.getOwner(address);

                if (owner == node.get_ID()) {
                    int localValue = node.localMemory.getOrDefault(address, 0);
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

                if (owner == node.get_ID()) {
                    node.localMemory.put(address, value);
                    System.out.println("Node " + node.get_ID() + " : LOCAL WRITE address " + address + " = " + value);
                } else {
                    Message writeMsg = new Message(
                            node.get_ID(),
                            owner,
                            address,
                            value,
                            MessageType.WRITE
                    );

                    System.out.println("Node " + node.get_ID() + " : REMOTE WRITE address " + address +
                            " = " + value + " to Node " + owner);

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

