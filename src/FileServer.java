import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

public class FileServer {
    private static final String FILE_DIRECTORY = "server_files";

    public static void main(String[] args) {
        int port = 3000;
        new File(FILE_DIRECTORY).mkdirs();

        try (ServerSocketChannel channel = ServerSocketChannel.open()) {
            channel.bind(new InetSocketAddress(port));
            System.out.println("Server started on port " + port);

            while (true) {
                SocketChannel socketChannel = channel.accept();
                System.out.println("Accepted connection from: " + socketChannel.getRemoteAddress());
                new Thread(new ClientHandler(socketChannel)).start();
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private final SocketChannel clientChannel;

        public ClientHandler(SocketChannel channel) {
            this.clientChannel = channel;
        }

        @Override
        public void run() {
            try (clientChannel) {
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                int bytesRead = clientChannel.read(buffer);
                if (bytesRead == -1) return;

                buffer.flip();
                String request = StandardCharsets.UTF_8.decode(buffer).toString().trim(); 
                String[] parts = request.split("%");
                String command = parts[0];

                switch (command) {
                    case "list" -> clientChannel.write(ByteBuffer.wrap(handleList().getBytes(StandardCharsets.UTF_8)));
                    case "delete" -> clientChannel.write(ByteBuffer.wrap(handleDelete(parts).getBytes(StandardCharsets.UTF_8)));
                    case "rename" -> clientChannel.write(ByteBuffer.wrap(handleRename(parts).getBytes(StandardCharsets.UTF_8)));
                    case "upload" -> handleUpload(parts);
                    case "download" -> handleDownload(parts);
                    default -> clientChannel.write(ByteBuffer.wrap("ERROR: Unknown command".getBytes(StandardCharsets.UTF_8)));
                }

            } catch (IOException e) {
                System.err.println("Error handling client: " + e.getMessage());
            } finally {
                System.out.println("Connection closed with client.");
            }
        }

        private String handleList() {
            File dir = new File(FILE_DIRECTORY);
            String[] files = dir.list((d, name) -> new File(d, name).isFile());
            if (files == null || files.length == 0) {
                return "No files on the server.";
            }
            return Arrays.stream(files).collect(Collectors.joining("\n"));
        }

        private String handleDelete(String[] parts) {
            if (parts.length < 2) return "ERROR: Missing filename for delete.";
            File file = new File(FILE_DIRECTORY, parts[1]);
            if (file.exists() && file.delete()) {
                return "SUCCESS: File deleted.";
            }
            return "ERROR: File not found or could not be deleted.";
        }

        private String handleRename(String[] parts) {
            if (parts.length < 3) return "ERROR: Missing original and new filenames for rename.";
            File oldFile = new File(FILE_DIRECTORY, parts[1]);
            File newFile = new File(FILE_DIRECTORY, parts[2]);
            if (oldFile.exists() && oldFile.renameTo(newFile)) {
                return "SUCCESS: File renamed.";
            }
            return "ERROR: File not found or could not be renamed.";
        }

        private void handleUpload(String[] parts) throws IOException {
            if (parts.length < 2) {
                clientChannel.write(ByteBuffer.wrap("ERROR: Missing filename for upload.".getBytes(StandardCharsets.UTF_8)));
                return;
            }
            String filename = parts[1];
            File fileToSave = new File(FILE_DIRECTORY, filename);

            clientChannel.write(ByteBuffer.wrap("READY".getBytes(StandardCharsets.UTF_8)));

            try (RandomAccessFile file = new RandomAccessFile(FILE_DIRECTORY + "/" + filename, "rw");
                 FileChannel fileChannel = file.getChannel()) {
                file.setLength(0);
                ByteBuffer buffer = ByteBuffer.allocate(8192);
                while ((clientChannel.read(buffer)) > 0) {
                    buffer.flip();
                    fileChannel.write(buffer);
                    buffer.compact();
                }
                clientChannel.write(ByteBuffer.wrap("SUCCESS: Upload complete.".getBytes(StandardCharsets.UTF_8)));
            } catch (IOException e) {
                clientChannel.write(ByteBuffer.wrap("ERROR: Server failed during upload.".getBytes(StandardCharsets.UTF_8)));
                fileToSave.delete();
            }
        }

        private void handleDownload(String[] parts) throws IOException {
            if (parts.length < 2) {
                clientChannel.write(ByteBuffer.wrap("ERROR: Missing filename for download.".getBytes(StandardCharsets.UTF_8)));
                return;
            }
            File file = new File(FILE_DIRECTORY, parts[1]);
            if (!file.exists()) {
                clientChannel.write(ByteBuffer.wrap("ERROR: File not found.".getBytes(StandardCharsets.UTF_8)));
                return;
            }

            clientChannel.write(ByteBuffer.wrap("SUCCESS".getBytes(StandardCharsets.UTF_8)));

            try (RandomAccessFile raf = new RandomAccessFile(file, "r");
                 FileChannel fileChannel = raf.getChannel()) {
                ByteBuffer buffer = ByteBuffer.allocate(8192);
                while (fileChannel.read(buffer) > 0) {
                    buffer.flip();
                    clientChannel.write(buffer);
                    buffer.compact();
                }
            }
        }
    }
}
