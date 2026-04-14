package dsm.util;

public class Log {
    public static void info(int nodeId, String message) {
        System.out.println("[*] Node " + nodeId + ": " + message);
    }

}