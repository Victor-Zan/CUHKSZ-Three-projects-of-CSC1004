
package client;

import server.Message;

import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * 聊天室客户端类
 * 提供命令行界面与服务器交互
 */
public class ChatClient {
    private Socket socket;              // 服务器套接字
    private ObjectOutputStream out;     // 输出流
    private ObjectInputStream in;       // 输入流
    private String userId;              // 用户ID
    private String username;            // 用户名
    private volatile boolean running = true; // 运行状态

    /**
     * 启动客户端
     */
    public void start() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Java Chat Room Client ===");
        System.out.println("Connecting to chat server...");

        try {
            // 1. 连接服务器
            System.out.print("Enter server address (default: localhost): ");
            String host = scanner.nextLine();
            if (host.isEmpty()) host = "localhost";

            System.out.print("Enter server port (default: 12345): ");
            String portStr = scanner.nextLine();
            int port = portStr.isEmpty() ? 12345 : Integer.parseInt(portStr);

            // 建立连接
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // 2. 接收服务器分配的用户ID
            userId = (String) in.readObject();
            System.out.println("Connected successfully! Your User ID: " + userId);

            // 3. 设置用户名
            System.out.print("Enter your username: ");
            username = scanner.nextLine();
            if (username.isEmpty()) username = "User" + userId;

            out.writeObject(username);
            out.flush();

            // 4. 启动消息接收线程
            Thread receiveThread = new Thread(this::receiveMessages);
            receiveThread.start();

            // 5. 显示欢迎信息和命令提示
            displayWelcomeMessage();

            // 6. 主循环：处理用户输入
            handleUserInput(scanner);

        } catch (UnknownHostException e) {
            System.err.println("Error: Unknown host. Please check the server address.");
        } catch (ConnectException e) {
            System.err.println("Error: Cannot connect to server. Make sure the server is running.");
        } catch (NumberFormatException e) {
            System.err.println("Error: Invalid port number.");
        } catch (IOException e) {
            System.err.println("Network error: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("Protocol error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
            scanner.close();
            System.out.println("\nDisconnected from chat server. Goodbye!");
        }
    }

    /**
     * 显示欢迎信息
     */
    private void displayWelcomeMessage() {
        System.out.println("\n=== Welcome to the Chat Room ===");
        System.out.println("You are connected as: " + username + " (" + userId + ")");
        System.out.println("\nAvailable commands:");
        System.out.println("  Type normally to send messages");
        System.out.println("  /help    - Show help");
        System.out.println("  /users   - Show online users");
        System.out.println("  /exit    - Leave the chat room");
        System.out.println("\n" + "=".repeat(40) + "\n");
    }

    /**
     * 处理用户输入
     */
    private void handleUserInput(Scanner scanner) {
        while (running) {
            System.out.print("> ");

            // 检查是否还有输入（防止阻塞）
            if (!scanner.hasNextLine()) {
                try {
                    Thread.sleep(100);
                    continue;
                } catch (InterruptedException e) {
                    break;
                }
            }

            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            // 处理退出命令
            if (input.equalsIgnoreCase("/exit")) {
                sendMessage(new Message(userId, username, "/exit", Message.MessageType.TEXT));
                running = false;
                break;
            }

            // 处理帮助命令（本地处理，不发送到服务器）
            if (input.equalsIgnoreCase("/help")) {
                System.out.println("\n=== Commands ===");
                System.out.println("/help    - Show this help");
                System.out.println("/users   - Request online user list");
                System.out.println("/exit    - Leave the chat room");
                System.out.println("Anything else will be sent as a chat message");
                System.out.println("================\n");
                continue;
            }

            // 发送普通消息
            Message message = new Message(userId, username, input, Message.MessageType.TEXT);
            sendMessage(message);
        }
    }

    /**
     * 接收消息的线程方法
     */
    private void receiveMessages() {
        try {
            while (running) {
                try {
                    Message message = (Message) in.readObject();

                    if (message == null) {
                        continue;
                    }

                    // 显示消息
                    System.out.println(message.toString());

                } catch (EOFException e) {
                    System.out.println("\nServer connection closed.");
                    running = false;
                    break;
                } catch (SocketException e) {
                    System.out.println("\nLost connection to server: " + e.getMessage());
                    running = false;
                    break;
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Error receiving message: " + e.getMessage());
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Protocol error: " + e.getMessage());
        }
    }

    /**
     * 发送消息到服务器
     */
    private void sendMessage(Message message) {
        if (!running || out == null || message == null) {
            return;
        }

        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            System.err.println("Failed to send message: " + e.getMessage());
            running = false;
        }
    }

    /**
     * 清理资源
     */
    private void cleanup() {
        running = false;

        try {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) { /* Ignore */ }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) { /* Ignore */ }
            }
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) { /* Ignore */ }
            }
        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }

    /**
     * 主方法
     */
    public static void main(String[] args) {
        ChatClient client = new ChatClient();
        client.start();
    }
}