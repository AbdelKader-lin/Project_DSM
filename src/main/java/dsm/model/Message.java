package dsm.model;

import java.io.Serializable;

public class Message implements Serializable {

    private final MessageType type;
    private final int senderId;
    private final int targetId;
    private final int address;
    private final Integer value;
    private final long requestId;

    public Message(int senderId, int targetId, int address, Integer value, MessageType type, long requestId) {
        this.senderId = senderId;
        this.targetId = targetId;
        this.address = address;
        this.value = value;
        this.type = type;
        this.requestId = requestId;
    }
    public MessageType getType() {
        return type;
    }
    public int getSenderId() {
        return senderId;
    }
    public int getTargetId() {
        return targetId;
    }
    public int getAddress() {
        return address;
    }
    public Integer getValue() {
        return value;
    }
    public long getRequestId() {
        return requestId;
    }
}