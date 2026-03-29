public interface DSM_itf {

    public int get_DSMsize() ;

    public byte[] read ( long adress ) ;

    public boolean write ( long adress , int x ) ;

    public boolean write ( long adress , char c ) ;

    public boolean write ( long adress , String s ) ;
}
