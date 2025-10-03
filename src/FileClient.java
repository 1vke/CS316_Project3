import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class FileClient {
    private static final String DOWNLOAD_DIRECTORY = "client_downloads";

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: java FileClient <serverIP> <serverPort>");
            return;
        }

        String serverIP = args[0];
        int serverPort = Integer.parseInt(args[1]);
        new File(DOWNLOAD_DIRECTORY).mkdirs();

        Scanner scnr = new Scanner(System.in);
        List<String> commandArgs;
        String command = "";
        String userInput;

        do {
            System.out.print("Enter command (list, delete, rename, download, upload, quit): ");
            userInput = scnr.nextLine();
            if (userInput.equalsIgnoreCase("quit")) {
                command = "quit";
                continue;
            }

            try (SocketChannel channel = SocketChannel.open(new InetSocketAddress(serverIP, serverPort))) {
                System.out.println("Connected to server.");
                commandArgs = Arrays.stream(userInput.split("%")).toList();
                command = commandArgs.getFirst();

                ByteBuffer requestBuffer = ByteBuffer.wrap(userInput.getBytes(StandardCharsets.UTF_8));
                channel.write(requestBuffer);

                if (command.equalsIgnoreCase("upload")) {
                    handleUpload(channel, commandArgs);
                } else {
                    ByteBuffer responseBuffer = ByteBuffer.allocate(8192);
                    if (command.equalsIgnoreCase("download")) {
                        handleDownload(channel, responseBuffer, commandArgs);
                    } else {
                        int bytesRead = channel.read(responseBuffer);
                        if (bytesRead > 0) {
                            responseBuffer.flip();
                            String response = StandardCharsets.UTF_8.decode(responseBuffer).toString();
                            System.out.println("Server response:\n" + response);
                        } else {
                            System.out.println("Server did not send a response.");
                        }
                    }
                }

            } catch (Exception e) {
                System.err.printf("Client error: %s\n", e.getMessage());
                e.printStackTrace();
            }
        } while (!command.equalsIgnoreCase("quit"));

        System.out.println("Client shutting down.");
        scnr.close();
    }

    private static void handleUpload(SocketChannel channel, List<String> commandArgs) throws IOException {
        if (commandArgs.size() < 2) {
            System.out.println("Usage: upload%<local_filename>");
            return;
        }
        String localFilename = commandArgs.get(1);
        File file = new File(localFilename);
        if (!file.exists()) {
            System.out.println("Error: File not found locally: " + localFilename);

            return;
        }

        ByteBuffer readyBuffer = ByteBuffer.allocate(1024);
        int bytesRead = channel.read(readyBuffer);
        if (bytesRead <= 0) {
            System.out.println("Server did not respond to upload request.");
            return;
        }
        readyBuffer.flip();
        String readyResponse = StandardCharsets.UTF_8.decode(readyBuffer).toString().trim();

        if (!readyResponse.equals("READY")) {
            System.out.println("Server error: " + readyResponse);
            return;
        }

        System.out.println("Server is ready. Uploading file: " + localFilename);
        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             FileChannel fileChannel = raf.getChannel()) {
            ByteBuffer buffer = ByteBuffer.allocate(8192);
            while (fileChannel.read(buffer) > 0) {
                buffer.flip();
                while(buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                buffer.compact();
            }
        }
        channel.shutdownOutput();

        ByteBuffer finalResponseBuffer = ByteBuffer.allocate(1024);
        bytesRead = channel.read(finalResponseBuffer);
        if (bytesRead > 0) {
            finalResponseBuffer.flip();
            String finalResponse = StandardCharsets.UTF_8.decode(finalResponseBuffer).toString().trim();
            System.out.println("Server response: " + finalResponse);
        } else {
            System.out.println("Upload complete, but no final confirmation from server.");
        }
    }

    private static void handleDownload(SocketChannel channel, ByteBuffer buffer, List<String> commandArgs) throws IOException {
        if (commandArgs.size() < 2) {
            System.out.println("Usage: download%<server_filename>");
            return;
        }
        String filename = commandArgs.get(1);
        String localFilePath = DOWNLOAD_DIRECTORY + "/" + filename;

        int bytesRead = channel.read(buffer);
        if (bytesRead <= 0) {
            System.out.println("Server did not respond to download request.");
            return;
        }
        buffer.flip();
        String response = StandardCharsets.UTF_8.decode(buffer).toString();
        buffer.compact();

        if (response.startsWith("ERROR:")) {
            System.out.println("Server error: " + response.trim());
            return;
        } else if (!response.startsWith("SUCCESS")) {
            System.out.println("Unknown server response: " + response.trim());
            return;
        }

        try (RandomAccessFile file = new RandomAccessFile(localFilePath, "rw");
             FileChannel fileChannel = file.getChannel()) {
            System.out.println("Downloading file to: " + localFilePath);
            file.setLength(0);
            while ((bytesRead = channel.read(buffer)) > 0) {
                buffer.flip();
                fileChannel.write(buffer);
                buffer.compact();
            }
            System.out.println("Download complete.");
        }
    }
}
