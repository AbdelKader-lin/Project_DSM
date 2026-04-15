package dsm.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class Message {

    public enum MessageType {
        READ_REQUEST,
        READ_RESPONSE,
        WRITE_REQUEST,
        WRITE_RESPONSE
    }

    private final MessageType type;
    private final int senderId;
    private final int targetId;
    private final int address;
    private final Integer value;
    private final long requestId;
    private final String errorMessage;

    public Message(int senderId, int targetId, int address, Integer value, MessageType type, long requestId) {
        this(senderId, targetId, address, value, type, requestId, "");
    }
    @JsonCreator
    public Message(
            @JsonProperty("senderId") int senderId,
            @JsonProperty("targetId") int targetId,
            @JsonProperty("address") int address,
            @JsonProperty("value") Integer value,
            @JsonProperty("type") MessageType type,
            @JsonProperty("requestId") Long requestId,
            @JsonProperty("errorMessage") String errorMessage
    ) {
        this.senderId = senderId;
        this.targetId = targetId;
        this.address = address;
        this.value = value;
        this.type = type;
        this.requestId = requestId;
        this.errorMessage = errorMessage;

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

    public String getErrorMessage() {
        return errorMessage;
    }
}