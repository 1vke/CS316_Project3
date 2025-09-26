import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class FileServer {
    public static void main(String[] args) {
        int port = 3000;

        try (ServerSocketChannel channel = ServerSocketChannel.open()) {
            channel.bind(new InetSocketAddress(port));

            while (true) {
                SocketChannel socketChannel = channel.accept();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
