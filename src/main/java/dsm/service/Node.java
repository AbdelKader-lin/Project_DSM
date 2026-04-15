package dsm.service;

import dsm.exception.DSMException;
import dsm.model.Message;
import dsm.model.Message.MessageType;
import dsm.util.Log;

import java.io.IOException;
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

    private static final int TIMEOUT_SECONDS = 30;

    public Node(int nodeId, int totalNodes, int globalAddressesCnt, MessageService messageService) {
        this.nodeId = nodeId;
        this.totalNodes = totalNodes;

        // all our chunk sizes are equal; simplified scenario
        this.chunkSize = (int) Math.ceil((double) globalAddressesCnt / (double) totalNodes);
        this.startAddressGlobal = nodeId * chunkSize;
        this.endAddressGlobal = Math.min(startAddressGlobal + chunkSize - 1, globalAddressesCnt - 1);
        this.globalAddressesCnt = globalAddressesCnt;

        this.storage = new MemoryStorage(chunkSize, nodeId * 1000);
        this.messageService = messageService;
    }

    private int getLocalAddress(int address) {
        return address % chunkSize;
    }

    private int getOwner(int address) throws DSMException {

        if (address < 0  || address >=  globalAddressesCnt) {
            throw new DSMException(String.format("Address <%s> is out of the global address space range", address));
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
                    String log = String.format("Processing READ at address=%s",
                            msg.getAddress());
                    Log.info(nodeId, log);

                    try {
                        Integer value = storage.read(msg.getAddress());

                        log = String.format("Done processing READ at address=%s -> %s",
                                msg.getAddress(), value);
                        Log.info(nodeId, log);

                        messageService.send(nodeId, new Message(
                                nodeId, msg.getSenderId(), msg.getAddress(),
                                value, MessageType.READ_RESPONSE, msg.getRequestId()));
                    } catch (DSMException e) {
                        log = String.format("Exception while processing READ at address=%s: %s",
                                msg.getAddress(), e.getMessage());
                        Log.info(nodeId, log);
                        messageService.send(nodeId, new Message(
                                nodeId, msg.getSenderId(), msg.getAddress(),
                                null, MessageType.READ_RESPONSE,
                                msg.getRequestId(), e.getMessage()));
                    }
                }

                case WRITE_REQUEST -> {
                    String log = String.format("Processing WRITE at address=%s value=%s",
                            msg.getAddress(), msg.getValue());
                    Log.info(nodeId, log);

                    try {
                        storage.write(msg.getAddress(), msg.getValue());

                        log = String.format("Done processing WRITE at address=%s value=%s",
                                msg.getAddress(), msg.getValue());
                        Log.info(nodeId, log);

                        messageService.send(nodeId, new Message(
                                nodeId, msg.getSenderId(),
                                msg.getAddress(), msg.getValue(),
                                MessageType.WRITE_RESPONSE, msg.getRequestId()
                        ));
                    } catch (DSMException e) {
                        log = String.format("Exception while processing WRITE at address=%s value=%s: %s",
                                msg.getAddress(), msg.getValue(), msg.getErrorMessage());
                        Log.info(nodeId, log);

                        messageService.send(nodeId, new Message(
                                nodeId, msg.getSenderId(), msg.getAddress(),
                                msg.getValue(), MessageType.WRITE_RESPONSE,
                                msg.getRequestId(), e.getMessage()));
                    }
                }

                case READ_RESPONSE -> {
                    if (!msg.getErrorMessage().isBlank()) {
                        handleErrorMessage(msg);
                    } else {
                        String valueString = Objects.isNull(msg.getValue()) ? "NULL" : msg.getValue().toString();
                        Log.info(nodeId, "Complete READ req=" + msg.getRequestId() + " -> " + valueString);

                        CompletableFuture<Integer> future =
                                awaitingRemoteReads.remove(msg.getRequestId());
                        if (future != null) {
                            future.complete(msg.getValue());
                        }
                    }
                }

                case WRITE_RESPONSE -> {

                    if (!msg.getErrorMessage().isBlank()) {
                        handleErrorMessage(msg);
                    } else {
                        Log.info(nodeId, "Complete WRITE req=" + msg.getRequestId());

                        CompletableFuture<Void> future = awaitingRemoteWrites.remove(msg.getRequestId());

                        if (future != null) {
                            future.complete(null);
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public CompletableFuture<Integer> readAsync(int address)  {
        int owner;
        int localAddress;
        try {
            owner = getOwner(address);
            localAddress = getLocalAddress(address);
            if (owner == nodeId) {
                Log.info(nodeId, "Local READ addr=" + localAddress);
                return CompletableFuture.completedFuture(storage.read(localAddress));

            }
        } catch (DSMException e) {
            return CompletableFuture.failedFuture(e);
        }

        long requestId = requestIdGenerator.incrementAndGet();
        Log.info(nodeId, "Prepare remote READ req=" + requestId + " addr=" + localAddress);

        try {
            CompletableFuture<Integer> future = new CompletableFuture<>();
            awaitingRemoteReads.put(requestId, future);

            messageService.send(nodeId, new Message(
                    nodeId, owner, localAddress, 0,
                    MessageType.READ_REQUEST, requestId));

            return future.orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .whenComplete((res, ex) -> awaitingRemoteReads.remove(requestId));
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<Void> writeAsync(int address, int value) {
        int owner;
        int localAddress;
        try {
            owner = getOwner(address);
            localAddress = getLocalAddress(address);

            if (owner == nodeId) {
                Log.info(nodeId, "Local WRITE addr=" + localAddress + " val=" + value);

                storage.write(localAddress, value);
                return CompletableFuture.completedFuture(null);
            }
        } catch (DSMException e) {
            return CompletableFuture.failedFuture(e);
        }

        long requestId = requestIdGenerator.incrementAndGet();
        Log.info(nodeId, "Prepare remote WRITE req=" + requestId + " addr=" + localAddress + " val=" + value);

        try {

            CompletableFuture<Void> future = new CompletableFuture<>();
            awaitingRemoteWrites.put(requestId, future);

            messageService.send(nodeId, new Message(
                    nodeId, owner, localAddress, value,
                    MessageType.WRITE_REQUEST, requestId
            ));

            return future.orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .whenComplete((res, ex) -> awaitingRemoteWrites.remove(requestId));
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    public Integer read(int address) throws DSMException {
        try {
            return readAsync(address).get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof DSMException dsmEx) {
                throw dsmEx;
            }
            throw new DSMException("Unexpected read error: " +e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DSMException("Read interrupted", e);
        }
    }

    public void write(int address, int value) throws DSMException {
        try {
            writeAsync(address, value).get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof DSMException dsmEx) {
                throw dsmEx;
            }
            throw new DSMException("Unexpected write error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DSMException("Write interrupted", e);
        }
    }
    public void printMemory() {
        Log.info(nodeId, "Print memory");
        storage.printState(nodeId);
    }

    private void handleErrorMessage(Message msg) {
        Log.info(nodeId, "Error req=" + msg.getRequestId() +
                ": " + msg.getErrorMessage());

        DSMException exception = new DSMException(msg.getErrorMessage());

        CompletableFuture<Integer> readFuture =
                awaitingRemoteReads.remove(msg.getRequestId());

        if (readFuture != null) {
            readFuture.completeExceptionally(exception);
            return;
        }

        CompletableFuture<Void> writeFuture =
                awaitingRemoteWrites.remove(msg.getRequestId());

        if (writeFuture != null) {
            writeFuture.completeExceptionally(exception);
        }
    }
}