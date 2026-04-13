import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Message {

    private MessageType messageType;
    private int senderID;
    private int targetID;
    private long address;
    private String payload;

    public Message( int senderID , int targetID , long address , String payload , MessageType type ) {
        this.messageType = type ;
        this.senderID = senderID ;
        this.targetID = targetID ;
        this.address = address ;
        this.payload = ( payload == null ) ? "" : payload ;
    }

    public int get_SenderID() {
        return this.senderID ;
    }

    public int get_TargetID() {
        return this.targetID ;
    }

    public long get_Address() {
        return this.address ;
    }

    public MessageType get_MessageType() {
        return this.messageType ;
    }

    public String get_Payload() {
        return this.payload ;
    }

    public byte[] get_PayloadBytes() {
        if ( this.payload.isEmpty() ) {
            return null ;
        }
        return Base64.getDecoder().decode( this.payload ) ;
    }

    public static String bytesToPayload( byte[] data ) {
        if ( data == null ) {
            return "" ;
        }
        return Base64.getEncoder().encodeToString( data ) ;
    }

    public static String stringToPayload( String s ) {
        if ( s == null ) {
            return "" ;
        }
        return bytesToPayload( s.getBytes( StandardCharsets.UTF_8 ) ) ;
    }

    public String MessageToString() {
        // Keep the simple "type:sender:target:address:payload" format from your friend's code.
        return this.messageType + ":" + this.senderID + ":" + this.targetID + ":" + this.address + ":" + this.payload;
    }

    public static Message StringToMessage( String s ) {
        String[] parts = s.split( ":" , 5 ) ;

        MessageType type = MessageType.valueOf( parts[ 0 ] ) ;
        int senderID = Integer.parseInt( parts[ 1 ] ) ;
        int targetID = Integer.parseInt( parts[ 2 ] ) ;
        long address = Long.parseLong( parts[ 3 ] ) ;
        String payload = ( parts.length >= 5 ) ? parts[ 4 ] : "" ;

        return new Message( senderID , targetID , address , payload , type ) ;
    }
}
