package dsm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import dsm.model.Message;
import dsm.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class MessageService {

    private final Connection connection;
    private final Channel publishChannel;
    private final Channel consumeChannel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public MessageService(String host) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);

        this.connection = factory.newConnection();
        this.publishChannel = connection.createChannel();
        this.consumeChannel = connection.createChannel();

    }
    public void startListening(int nodeId, Consumer<Message> handler) throws IOException {
        String queue = "node" + nodeId;

        consumeChannel.queueDeclare(queue, false, false, false, null);

        DeliverCallback callback = (tag, delivery) -> {
            try {
                System.out.println("RAW MESSAGE RECEIVED");
                Message msg = objectMapper.readValue(delivery.getBody(), Message.class);
                Log.info(nodeId, "RECV " + msg.getType() + "...");
                handler.accept(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }

        };

        consumeChannel.basicConsume(queue, true, callback, tag -> {});

        System.out.println("Node " + nodeId + " is now consuming the queue " + queue);
    }

    public void send(int fromNode, Message message) throws IOException {
        String queue = "node" + message.getTargetId();
        publishChannel.queueDeclare(queue, false, false, false, null);


        Log.info(fromNode, "Send " + message.getType() + " Node " + message.getTargetId() +
                " (addr=" + message.getAddress() + ", val=" + message.getValue() +
                ", req=" + message.getRequestId() + ")");

        String json = objectMapper.writeValueAsString(message);
        publishChannel.basicPublish("", queue, null, json.getBytes(StandardCharsets.UTF_8));

    }
}