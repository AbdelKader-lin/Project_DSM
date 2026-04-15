package dsm.service;

import dsm.exception.DSMException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class MemoryStorage {

    private final int storageSize;

    // might be more practical to have an array instead?
    private final Map<Integer, Integer> memory = new ConcurrentHashMap<>();

    public MemoryStorage(int storageSize, int initValue) {
        this.storageSize = storageSize;

        for (int addr = 0; addr < storageSize; addr ++) {
            memory.put(addr, initValue);
        }
    }

    public Integer read(int address) throws DSMException {
        return read(address, 1).get(0);
    }

    public void write(int address, int value) throws DSMException {
        write(address, 1, List.of(value));
    }

    public List<Integer> read(int startAddress, int size) throws DSMException {
        int endAddress = startAddress + size - 1;
        if (!isRangeValid(startAddress, endAddress)) {
            String errorMsg;
            if (size == 1) {
                errorMsg = String.format("Invalid read address: %s, %s]", startAddress, endAddress);
            } else {
                errorMsg = String.format("Invalid read address range [%s, %s]", startAddress, endAddress);
            }
            throw new DSMException(errorMsg);
        }

        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            int addr = startAddress + i;
            result.add(memory.get(addr));
        }
        return result;
    }

    public void write(int startAddress, int size, List<Integer> values) throws DSMException {
        int endAddress = startAddress + size - 1;
        if (!isRangeValid(startAddress, endAddress) || Objects.isNull(values) || size > values.size()) {
            String errorMsg;
            if (size == 1) {
                errorMsg = String.format("Invalid write address: %s, %s]", startAddress, endAddress);
            } else {
                errorMsg = String.format("Invalid write address range [%s, %s]", startAddress, endAddress);
            }
            throw new DSMException(errorMsg);
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