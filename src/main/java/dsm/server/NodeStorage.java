package dsm.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class NodeStorage {
    private final Map<Long, byte[]> storage = new ConcurrentHashMap<>();

    public byte[] read(long address) {
        return storage.get(address);
    }

    public void write(long address, byte[] data) {
        storage.put(address, data);
    }
}
