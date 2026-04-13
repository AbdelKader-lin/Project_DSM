public interface DSM_itf {

    // Number of partitions in this DSM instance .
    public int get_DSMsize() ;

    public int partitionOwner( long address ) ;

    // Allocate an @ in the DSM .
    public long allocate( int size ) ;

    // Read raw bytes stored at @. Returns null if nothing was written there.
    public byte[] read( long adress ) ;

    public boolean write( long adress , int x ) ;

    public boolean write( long adress , char c ) ;

    public boolean write( long adress , String s ) ;
}
