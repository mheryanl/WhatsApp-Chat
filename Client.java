import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {

    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT)) {
            System.out.println("Connected to server!");

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Scanner scanner = new Scanner(System.in);

            new Thread(() -> {
                try {
                    String response;
                    while ((response = in.readLine()) != null) {
                        System.out.println("Server: " + response);
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            }).start();

            while (true) {
                String command = scanner.nextLine();
                if (command.equalsIgnoreCase("quit")) {
                    out.println("exit");
                    System.out.println("Disconnected from server.");
                    break;
                } else if (command.startsWith("SendFile")) {
                    handleFileTransfer(command, out, socket);
                } else {
                    out.println(command);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleFileTransfer(String command, PrintWriter out, Socket socket) {
        try {
            String[] parts = command.split("\\|");
            if (parts.length < 3) {
                System.out.println("Invalid command format. Use: SendFile|<GroupName>|<FilePath>");
                return;
            }

            String groupName = parts[1];
            String filePath = parts[2];
            File file = new File(filePath);

            if (!file.exists()) {
                System.out.println("File not found: " + filePath);
                return;
            }

            out.println("SendFile|" + groupName + "|" + file.getName());

            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
                 OutputStream os = socket.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
                out.println("FileTransferComplete|" + groupName);
            }

            System.out.println("File sent successfully: " + file.getName());

        } catch (IOException e) {
            System.out.println("Error sending file: " + e.getMessage());
        }
    }
}
