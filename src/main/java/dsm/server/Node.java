package dsm.server;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;



public class Node {
    private final int nodeId;
    private final NodeStorage memory;

    public Node(int nodeId) {
        this.nodeId = nodeId;
        this.memory = new NodeStorage();
    }

    public void start() throws Exception {
        String queueName = "dsm.node." + nodeId;

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(queueName, false, false, false, null);

        System.out.println("DSMNode " + nodeId + " listening on " + queueName);

    }
}
