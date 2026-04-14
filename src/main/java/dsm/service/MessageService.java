package dsm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import dsm.model.Message;
import dsm.util.Log;

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
    public void startListening(int nodeId, Consumer<Message> handler) throws Exception {
        String queue = "node" + nodeId;

        consumeChannel.queueDeclare(queue, false, false, false, null);

        DeliverCallback callback = (tag, delivery) -> {
            Message msg = objectMapper.readValue(delivery.getBody(), Message.class);
            Log.info(nodeId, "RECV " + msg.getType() + " Node " + msg.getSenderId() + " (addr=" + msg.getAddress() + ", val=" + msg.getValue() + ", req=" + msg.getRequestId() + ")");
            handler.accept(msg);

        };

        consumeChannel.basicConsume(queue, true, callback, tag -> {});
    }

    public void send(int fromNode, Message message) throws Exception {
        String queue = "node" + message.getTargetId();

        Log.info(fromNode, "Send " + message.getType() + " Node " + message.getTargetId() +
                " (addr=" + message.getAddress() + ", val=" + message.getValue() +
                ", req=" + message.getRequestId() + ")");

        publishChannel.basicPublish("", queue, null, objectMapper.writeValueAsBytes(message));
    }
}