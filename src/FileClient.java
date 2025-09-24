import java.util.Scanner;

public class FileClient {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Need server IP and server port");
        }

        String serverIP = args[0];
        int serverPort = Integer.parseInt(args[1]);

        Scanner scnr = new Scanner(System.in);
        String message;

        do {
            message = scnr.nextLine().toLowerCase();

            switch (message) {
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

        } while (!message.equals("quit"));
    }
}
