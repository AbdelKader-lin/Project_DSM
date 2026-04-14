package dsm;

import dsm.service.MessageService;
import dsm.service.Node;

import java.util.Scanner;



public class Main {

    public static void main(String[] args) throws Exception {

        int nodeId = Integer.parseInt(args[0]);
        int totalNodes = Integer.parseInt(args[1]);
        int globalAddressesCnt = Integer.parseInt(args[2]);

        MessageService messageService = new MessageService("localhost");

        Node node = new Node(nodeId, totalNodes, globalAddressesCnt, messageService);
        node.start();

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("> ");
            String[] input = scanner.nextLine().split(" ");

            switch (input[0]) {

                case "r" -> {
                    int addr = Integer.parseInt(input[1]);
                    int res = node.read(addr);
                    System.out.println("Read result: " + res);
                }

                case "ra" -> {
                    int addr = Integer.parseInt(input[1]);

                    node.readAsync(addr).thenAccept(val ->
                            System.out.println("Asynchronous read result: " + val));
                }

                case "w" -> {
                    int addr = Integer.parseInt(input[1]);
                    int val = Integer.parseInt(input[2]);

                    node.write(addr, val);
                    System.out.println("Write done!");
                }

                case "wa" -> {
                    int addr = Integer.parseInt(input[1]);
                    int val = Integer.parseInt(input[2]);

                    node.writeAsync(addr, val).thenRun(() -> System.out.println("Asynchronous write done!"));
                }

                case "print" -> node.printMemory();
                default ->
                    System.out.println("""
                            Use one of the following commands:
                            r <address> (read <value> from <address>)
                            w <address> <value> (write <value> to <address>)
                            ra <address> (asynchronous read <value> from <address>)
                            wa <address> <value> (asynchronous write <value> to <address>)
                            print (print the local memory snapshot)""");

            }
        }
    }
}