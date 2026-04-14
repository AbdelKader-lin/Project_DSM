package dsm.service;

import dsm.model.Message;
import dsm.model.MessageType;
import dsm.util.Log;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class Node {

    private final int globalAddressesCnt;
    private final int chunkSize;
    private final int startAddressGlobal;
    private final int endAddressGlobal;
    private final int nodeId;
    private final int totalNodes;
    private final MemoryStorage storage;
    private final MessageService messageService;

    // as we want a node to reply to user requests coherently (read (addr, size) -> here's the list of sequentially placed values corresponding to your read),
    // we need to track read/write messages sent to remote nodes as unique requests
    // also there might be > 1 receiver nodes for range requests
    // the result of the request processing (what we get in response msg) from one target node  will be available in the ComplketeableFuture object
    private final Map<Long, CompletableFuture<Integer>> awaitingRemoteReads = new ConcurrentHashMap<>();
    private final Map<Long, CompletableFuture<Void>> awaitingRemoteWrites = new ConcurrentHashMap<>();

    private final AtomicLong requestIdGenerator = new AtomicLong();

    private static final int TIMEOUT_SECONDS = 3;

    public Node(int nodeId, int totalNodes, int globalAddressesCnt, MessageService messageService) {
        this.nodeId = nodeId;
        this.totalNodes = totalNodes;

        // all our chunk sizes are equal; simplified scenario
        this.chunkSize = (int) Math.ceil((double) globalAddressesCnt / (double) totalNodes);
        this.startAddressGlobal = nodeId * chunkSize;
        this.endAddressGlobal = Math.min(startAddressGlobal + chunkSize - 1, globalAddressesCnt - 1);
        this.globalAddressesCnt = globalAddressesCnt;

        this.storage = new MemoryStorage(chunkSize);
        this.messageService = messageService;
    }

    private int getOwner(int address) {

        if (address <= 0  || address >=  globalAddressesCnt) {
            throw new RuntimeException(String.format("Address <%s> is out of the global address space range", address));
        }

        return address / chunkSize;
    }

    public void start() throws Exception {
        messageService.startListening(nodeId, this::handleMessage);
        Log.info(nodeId, "Started!");
        storage.printState(nodeId);
    }
    private void handleMessage(Message msg) {
        try {
            switch (msg.getType()) {

                case READ_REQUEST -> {

                    Integer value = storage.read(msg.getAddress());

                    String log = String.format("Processing READ at address =%s -> %s",
                            msg.getAddress(), value);
                    Log.info(nodeId, log);

                    messageService.send(nodeId, new Message(
                            nodeId, msg.getSenderId(), msg.getAddress(),
                            value, MessageType.READ_RESPONSE, msg.getRequestId()));
                }

                case WRITE_REQUEST -> {
                    String log = String.format("Processing WRITE at address =%s value = %s",
                            msg.getAddress(), msg.getValue());
                    Log.info(nodeId, log);

                    storage.write(msg.getAddress(), msg.getValue());
                    messageService.send(nodeId, new Message(
                            nodeId,  msg.getSenderId(),
                            msg.getAddress(),  msg.getValue(),
                            MessageType.WRITE_RESPONSE,  msg.getRequestId()
                    ));
                }

                case READ_RESPONSE -> {
                    String valueString = Objects.isNull(msg.getValue()) ? "NULL" : msg.getValue().toString();
                    Log.info(nodeId, "Complete READ req=" + msg.getRequestId() + " -> " + valueString);

                    CompletableFuture<Integer> future =
                            awaitingRemoteReads.remove(msg.getSenderId());
                    if (future != null) {
                        future.complete(msg.getValue());
                    }

                }

                case WRITE_RESPONSE -> {
                    Log.info(nodeId, "Complete WRITE req=" + msg.getRequestId());

                    CompletableFuture<Void> future = awaitingRemoteWrites.remove(msg.getRequestId());

                    if (future != null) {
                        future.complete(null);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public CompletableFuture<Integer> readAsync(int address) throws Exception {
        int owner = getOwner(address);

        if (owner == nodeId) {
            Log.info(nodeId, "Local READ addr=" + address);
            return CompletableFuture.completedFuture(storage.read(address));
        }

        long requestId = requestIdGenerator.incrementAndGet();
        Log.info(nodeId, "Prepare remote READ req=" + requestId + " addr=" + address);


        CompletableFuture<Integer> future = new CompletableFuture<>();
        awaitingRemoteReads.put(requestId, future);
        messageService.send(nodeId, new Message(
                nodeId, owner, address, 0,
                MessageType.READ_REQUEST, requestId));

        return future.orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete((res, ex) -> awaitingRemoteReads.remove(requestId));
    }

    public CompletableFuture<Void> writeAsync(int address, int value) throws Exception {
        int owner = getOwner(address);

        if (owner == nodeId) {
            Log.info(nodeId, "Local WRITE addr=" + address + " val=" + value);
            storage.write(address, value);
            return CompletableFuture.completedFuture(null);
        }

        long requestId = requestIdGenerator.incrementAndGet();
        Log.info(nodeId, "Prepare remote WRITE req=" + requestId + " addr=" + address + " val=" + value);

        CompletableFuture<Void> future = new CompletableFuture<>();
        awaitingRemoteWrites.put(requestId, future);

        messageService.send(nodeId, new Message(
                nodeId, owner, address, value,
                MessageType.WRITE_REQUEST, requestId
        ));

        return future.orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete((res, ex) -> awaitingRemoteWrites.remove(requestId));
    }
    public Integer read(int address) throws Exception {
        return readAsync(address).get();
    }

    public void write(int address, int value) throws Exception {
        writeAsync(address, value).get();
    }
    public void printMemory() {
        Log.info(nodeId, "Print memory");
        storage.printState(nodeId);
    }
}