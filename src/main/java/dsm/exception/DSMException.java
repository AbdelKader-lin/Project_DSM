package dsm.exception;

public class DSMException extends Exception {

    public DSMException(String message) {
        super(message);
    }

    public DSMException(String message, Throwable cause) {
        super(message, cause);
    }
}