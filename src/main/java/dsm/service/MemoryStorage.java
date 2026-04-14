package dsm.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class MemoryStorage {

    private final int storageSize;

    // might be more practical to have an array instead?
    private final Map<Integer, Integer> memory = new ConcurrentHashMap<>();

    public MemoryStorage(int storageSize) {
        this.storageSize = storageSize;

        for (int addr = 0; addr < storageSize; addr ++) {
            memory.put(addr, -10000);
        }
    }

    public Integer read(int address) {
        return read(address, 1).get(0);
    }

    public void write(int address, int value) {
        write(address, 1, List.of(1));
    }

    public List<Integer> read(int startAddress, int size) {
        int endAddress = startAddress + size - 1;
        if (!isRangeValid(startAddress, endAddress)) {
            throw new RuntimeException("Invalid local memory access");
        }

        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            int addr = startAddress + i;
            result.add(memory.get(addr));
        }
        return result;
    }

    public void write(int startAddress, int size, List<Integer> values) {
        int endAddress = startAddress + size - 1;
        if (!isRangeValid(startAddress, endAddress) || Objects.isNull(values) || size >= values.size()) {
            throw new RuntimeException("Invalid local memory access");
        }

        for (int i = 0; i < size; i++) {
            int addr = startAddress + i;
            memory.put(addr, values.get(i));
        }
    }

    public void printState(int nodeId) {
        System.out.println("Node " + nodeId + " local memory view: " + memory);
    }

    private boolean isRangeValid(int startAddress, int endAddress) {
        return (startAddress <= endAddress) && (startAddress >=0) && (endAddress < storageSize);
    }
}