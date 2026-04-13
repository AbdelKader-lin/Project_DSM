
public class Address {

    // Shared monotonically increasing address generator.
    private static long nextAddress = 1 ;

    public static synchronized long allocate() {
        long allocated = nextAddress ;
        nextAddress++ ;
        return allocated ;
    }

    /*
     * Return which partition ( 0 ... n-1 ) owns this address.
     */
    public static int ownerIndex( long address , int n ) {
        return ( int ) Math.floorMod( address , ( long ) n ) ;
    }
}
