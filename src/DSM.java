import java.io.*;
import java.net.*;
import java.util.*;

public class DSM implements DSM_itf {

    private int DSMsize ;
    
    public int get_DSMsize(){
        return this.DSMsize ;
    }

    public byte[] read ( long adress ){
        // TO IMPLEMENT
        return null ;
    }

    public boolean write ( long adress , int x ){
        // TO IMPLEMENT
        return true ;
    }

    public boolean write ( long adress , char c ){
        // TO IMPLEMENT
        return true ;
    }

    public boolean write ( long adress , String s ){
        // TO IMPLEMENT
        return true ;
    }


}
