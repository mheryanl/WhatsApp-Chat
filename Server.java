import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    public static class GroupChatProtocol {
        public static final String ADD_GROUP = "AddGroup";
        public static final String JOIN_GROUP = "JoinGroup";
        public static final String SEND_MESSAGE = "SendMessage";
        public static final String LEAVE_GROUP = "LeaveGroup";
        public static final String REMOVE_GROUP = "RemoveGroup";
        public static final String SEND_FILE = "SendFile";
        public static final String FILE_TRANSFER_COMPLETE = "FileTransferComplete";
    }

    private static final int PORT = 12345;
    private static Map<String, Group> groups = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started. Listening on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class Group {
        String name;
        Set<PrintWriter> clients = Collections.synchronizedSet(new HashSet<>());

        Group(String name) {
            this.name = name;
        }
    }

    static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println("Connected to the server. Welcome!");

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("Received: " + inputLine);
                    if (inputLine.equalsIgnoreCase("exit")) {
                        System.out.println("Client disconnected: " + socket.getInetAddress());
                        handleDisconnection();
                        break;
                    } else if (inputLine.startsWith(GroupChatProtocol.SEND_FILE)) {
                        handleFileTransfer(inputLine);
                    } else if (inputLine.startsWith(GroupChatProtocol.FILE_TRANSFER_COMPLETE)) {
                        System.out.println("File transfer completed.");
                    } else {
                        handleCommand(inputLine);
                    }
                }
            } catch (IOException e) {
                System.out.println("Client disconnected unexpectedly: " + socket.getInetAddress());
                handleDisconnection();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Error closing socket: " + e.getMessage());
                }
            }
        }

        private void handleDisconnection() {
            // Remove the client from all groups
            for (Group group : groups.values()) {
                group.clients.remove(out);
            }
            System.out.println("Client removed from all groups.");
        }

        private void handleCommand(String input) {
            try {
                String[] parts = input.split("\\|");
                String command = parts[0];
                String groupName;

                switch (command) {
                    case GroupChatProtocol.ADD_GROUP:
                        groupName = parts[1];
                        groups.putIfAbsent(groupName, new Group(groupName));
                        out.println("Group " + groupName + " created.");
                        break;

                    case GroupChatProtocol.JOIN_GROUP:
                        groupName = parts[1];
                        Group group = groups.get(groupName);
                        if (group != null) {
                            group.clients.add(out);
                            out.println("Joined group " + groupName);
                            System.out.println("Client joined group: " + groupName);
                        } else {
                            out.println("Group does not exist.");
                        }
                        break;

                    case GroupChatProtocol.SEND_MESSAGE:
                        groupName = parts[1];
                        String message = parts[2];
                        broadcastMessage(groupName, "[" + groupName + "] " + message);
                        break;

                    case GroupChatProtocol.LEAVE_GROUP:
                        groupName = parts[1];
                        leaveGroup(groupName);
                        break;

                    case GroupChatProtocol.REMOVE_GROUP:
                        groupName = parts[1];
                        removeGroup(groupName);
                        break;

                    default:
                        out.println("Invalid command.");
                        System.out.println("Invalid command received: " + input);
                }
            } catch (Exception e) {
                System.out.println("Error handling command: " + input);
            }
        }

        private void leaveGroup(String groupName) {
            Group group = groups.get(groupName);
            if (group != null) {
                if (group.clients.remove(out)) {
                    out.println("You have left the group: " + groupName);
                    System.out.println("Client left group: " + groupName);
                } else {
                    out.println("You are not a member of this group.");
                }
            } else {
                out.println("Group does not exist.");
            }
        }

        private void removeGroup(String groupName) {
            Group group = groups.get(groupName);
            if (group != null) {
                if (group.clients.isEmpty()) {
                    groups.remove(groupName);
                    out.println("Group " + groupName + " has been removed.");
                    System.out.println("Group removed: " + groupName);
                } else {
                    out.println("Group " + groupName + " is not empty. Cannot remove.");
                }
            } else {
                out.println("Group does not exist.");
            }
        }

        private void handleFileTransfer(String input) {
            try {
                String[] parts = input.split("\\|");
                String groupName = parts[1];
                String fileName = parts[2];

                Group group = groups.get(groupName);
                if (group == null) {
                    out.println("Group does not exist.");
                    return;
                }

                out.println("Ready to receive file: " + fileName);

                // Receive file
                File file = new File("received_" + fileName);
                try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
                    InputStream is = socket.getInputStream();
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        bos.write(buffer, 0, bytesRead);
                    }
                }

                broadcastMessage(groupName, "File received: " + fileName);
                out.println("File successfully uploaded: " + fileName);

            } catch (IOException e) {
                out.println("Error while receiving file.");
                System.out.println("Error during file transfer: " + e.getMessage());
            }
        }

        private void broadcastMessage(String groupName, String message) {
            Group group = groups.get(groupName);
            if (group != null) {
                for (PrintWriter client : group.clients) {
                    client.println(message);
                }
                System.out.println("Broadcasted to group " + groupName + ": " + message);
            } else {
                System.out.println("Attempted to broadcast to non-existent group: " + groupName);
            }
        }
    }
}
