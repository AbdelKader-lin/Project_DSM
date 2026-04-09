public class Message {
    
    MessageType messageType;
    int senderID;
    int targetID;
    int address;
    int value;

    Message(int senderID, int targetID, int address, int value, MessageType m) {
        this.messageType = m;
        this.senderID = senderID;
        this.targetID = targetID;
        this.address = address;
        this.value = value;
    }

    public int get_SenderID() {
        return this.senderID;
    }

    public int get_TargetID() {
        return this.targetID;
    }

    public int get_Address() {
        return this.address;
    }

    public int get_Value() {
        return this.value;
    }

    public MessageType get_MessageType() {
        return this.messageType;
    }

    public String MessageToString() {
        return this.messageType + ":" + this.senderID + ":" + this.targetID + ":" + this.address + ":" + this.value;
    }

    public static Message StringToMessage(String s) {
        String[] parts = s.split(":");

        MessageType type = MessageType.valueOf(parts[0]);
        int senderID = Integer.parseInt(parts[1]);
        int targetID = Integer.parseInt(parts[2]);
        int address = Integer.parseInt(parts[3]);
        int value = Integer.parseInt(parts[4]);

        return new Message(senderID, targetID, address, value, type);
    }
}