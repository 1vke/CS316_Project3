import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class FileClient {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Need server IP and server port");
        }

        String serverIP = args[0];
        int serverPort = Integer.parseInt(args[1]);

        Scanner scnr = new Scanner(System.in);
        List<String> commandArgs = new ArrayList<>();
        String command = "";

        do {
            try (SocketChannel channel = SocketChannel.open(new InetSocketAddress(serverIP, serverPort))) {
                commandArgs = Arrays.stream(scnr.nextLine().split(" ")).toList();
                command = commandArgs.getFirst();

//                ByteBuffer buffer = ByteBuffer.wrap(messageBytes);
//                buffer.put(messageBytes);
//                buffer.flip();
//
//                channel.write(buffer);
//                channel.shutdownOutput();

                switch (command) {
                    case "list":
                        break;
                    case "delete":
                        break;
                    case "rename":
                        break;
                    case "download":
                        break;
                    case "upload":
                        break;
                    default:
                        System.out.println("Invalid command");
                }
            } catch (Exception e) {
                System.out.printf("There was an error! Try again. \n %s\n", e.getMessage());
            }
        } while (!command.equalsIgnoreCase("quit"));
    }
}
