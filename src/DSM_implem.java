import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DSM_implem implements DSM_itf {

    private int DSMsize ;
    private int partitionIdx ;

    private Map< Long , byte[] > Mem = new HashMap<>() ;

    public DSM_implem ( int myIndex , int DSMsize ) {
        this.partitionIdx = myIndex ;
        this.DSMsize = DSMsize ;
    }
    
    public int get_DSMsize() {
        return this.DSMsize ;
    }

    public int get_partitionIdx() {
        return this.partitionIdx ;
    }

    public Map< Long , byte[] > get_Mem () {
        return this.Mem ;
    }

    public int partitionOwner( long address ) {
        return Address.ownerIndex( address , this.get_DSMsize() ) ;
    }

    public byte[] read ( long address ){

        // Check to which partition does the @ belong
        int partOwner = partitionOwner( address ) ;

        byte[] toRead = null ;
        if ( partOwner == this.get_partitionIdx() ) { // My partition
            toRead = this.get_Mem().get( address ) ;
        } else{
            // Local-only version: remote partition read is not implemented yet.
            toRead = null ;
        }
        return toRead ;
    }

    public boolean write ( long address , int x ){
        String s = Integer.toString( x ) ;
        return writeBytes( address , s.getBytes( StandardCharsets.UTF_8 ) ) ;
    }

    public boolean write ( long address , char c ){
        String s = Character.toString( c ) ;
        return writeBytes( address , s.getBytes( StandardCharsets.UTF_8 ) ) ;
    }

    public boolean write ( long address , String s ){
        if ( s == null ) {
            return false ;
        }
        return writeBytes( address , s.getBytes( StandardCharsets.UTF_8 ) ) ;
    }

    public long allocate ( int size ){

        if ( size <= 0 ) {
            throw new IllegalArgumentException( "Size must be > 0" ) ;
        }

        // Find an @ in this partition.
        long address = Address.allocate() ;
        while ( partitionOwner(address) != this.get_partitionIdx() ) {
            address = Address.allocate() ;
        }

        // Reserve space for this address.
        this.get_Mem().put( address , new byte[ size ] ) ;
        return address ;
    }

    private boolean writeBytes( long address , byte[] data ) {

        if ( data == null ) {
            return false ;
        }

        int partOwner = partitionOwner( address ) ;
        if ( partOwner == this.get_partitionIdx() ) {
            this.get_Mem().put( address , data ) ;
            return true ;
        }

        // Remote partition write is not implemented yet.
        return false ;
    }

    // Used by Node when storing raw bytes received over the network.
    void writeRaw( long address , byte[] data ) {
        this.get_Mem().put( address , data ) ;
    }

}
