package server;

import java.io.*;
import java.net.*;
import java.util.List;

/**
 * 客户端处理器类
 * 每个客户端连接对应一个ClientHandler实例，运行在独立线程中
 */
class ClientHandler implements Runnable {
    private Socket socket;          // 客户端套接字
    private String userId;          // 用户ID
    private String username;        // 用户名
    private ObjectOutputStream out; // 输出流
    private ObjectInputStream in;   // 输入流
    private volatile boolean running = true; // 运行状态标志

    /**
     * 构造函数
     */
    public ClientHandler(Socket socket, String userId) {
        this.socket = socket;
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public String getUserId() {
        return userId;
    }

    /**
     * 线程主方法
     */
    @Override
    public void run() {
        try {
            // 1. 建立输入输出流
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // 2. 发送用户ID给客户端
            out.writeObject(userId);
            out.flush();

            // 3. 接收用户名（等待客户端发送）
            Object usernameObj = in.readObject();
            if (usernameObj instanceof String) {
                username = ((String) usernameObj).trim();
                if (username.isEmpty()) {
                    username = "User" + userId;
                }
            } else {
                username = "User" + userId;
            }

            // 4. 添加到在线用户列表（重要：此时才添加）
            ChatServer.addClient(userId, this);

            // 5. 打印连接信息（此时统计准确）
            System.out.println("[Server] New user connected: " + username + " (" + userId + ")");
            System.out.println("[Server] Current online users: " + ChatServer.getClientCount());

            // 6. 发送欢迎信息和在线用户列表
            sendWelcomeMessage();

            // 7. 发送聊天历史记录
            sendChatHistory();

            // 8. 广播用户加入通知
            Message joinMessage = new Message(userId, username, "joined the chat room", Message.MessageType.JOIN);
            ChatServer.broadcast(joinMessage, userId);

            // 9. 主消息处理循环
            while (running) {
                try {
                    Message message = (Message) in.readObject();

                    if (message == null) {
                        continue;
                    }

                    String content = message.getContent();

                    // 处理退出命令
                    if (content.equalsIgnoreCase("/exit")) {
                        System.out.println("[Server] User " + username + " requested to exit");
                        break;
                    }

                    // 处理帮助命令
                    if (content.equalsIgnoreCase("/help")) {
                        sendHelpMessage();
                        continue;
                    }

                    // 处理用户列表命令
                    if (content.equalsIgnoreCase("/users")) {
                        sendUserList();
                        continue;
                    }

                    // 广播普通消息
                    ChatServer.broadcast(message, userId);

                } catch (EOFException e) {
                    // 客户端正常断开连接
                    System.out.println("[Server] Client " + username + " (" + userId + ") disconnected normally");
                    break;
                } catch (ClassNotFoundException e) {
                    System.err.println("[Server] Protocol error from user " + userId + ": " + e.getMessage());
                } catch (SocketException e) {
                    System.out.println("[Server] Client " + username + " (" + userId + ") connection lost: " + e.getMessage());
                    break;
                }
            }

        } catch (IOException e) {
            System.err.println("[Server] I/O error with client " + userId + ": " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("[Server] Protocol error with client " + userId + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[Server] Unexpected error with client " + userId + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    /**
     * 发送欢迎信息和在线用户列表
     */
    private void sendWelcomeMessage() throws IOException {
        List<String> users = ChatServer.getOnlineUsers(userId);

        if (users.isEmpty()) {
            Message welcomeMsg = new Message("SERVER", "System",
                    "Welcome to the chat room! You are the first user.\n" +
                            "Type /help for available commands.",
                    Message.MessageType.USER_LIST);
            sendMessage(welcomeMsg);
        } else {
            StringBuilder userListStr = new StringBuilder();
            userListStr.append("Welcome to the chat room!\n");
            userListStr.append("Online users (").append(users.size()).append("):\n");

            for (String user : users) {
                userListStr.append("  • ").append(user).append("\n");
            }

            userListStr.append("\nType /help for available commands.");

            Message userListMessage = new Message("SERVER", "System",
                    userListStr.toString(), Message.MessageType.USER_LIST);
            sendMessage(userListMessage);
        }
    }

    /**
     * 发送帮助信息
     */
    private void sendHelpMessage() throws IOException {
        String helpText =
                "\n=== Chat Room Commands ===\n" +
                        "/help    - Show this help message\n" +
                        "/users   - Show online users\n" +
                        "/exit    - Leave the chat room\n" +
                        "===========================\n";

        Message helpMessage = new Message("SERVER", "System", helpText, Message.MessageType.USER_LIST);
        sendMessage(helpMessage);
    }

    /**
     * 发送在线用户列表
     */
    private void sendUserList() throws IOException {
        List<String> users = ChatServer.getOnlineUsers(userId);

        if (users.isEmpty()) {
            Message noUsersMsg = new Message("SERVER", "System",
                    "You are the only user online.", Message.MessageType.USER_LIST);
            sendMessage(noUsersMsg);
        } else {
            StringBuilder userListStr = new StringBuilder();
            userListStr.append("Online users (").append(users.size()).append("):\n");

            for (String user : users) {
                userListStr.append("  • ").append(user).append("\n");
            }

            Message userListMessage = new Message("SERVER", "System",
                    userListStr.toString(), Message.MessageType.USER_LIST);
            sendMessage(userListMessage);
        }
    }

    /**
     * 发送聊天历史记录
     */
    private void sendChatHistory() throws IOException {
        List<Message> history = ChatServer.getChatHistory();
        if (!history.isEmpty()) {
            Message historyHeader = new Message("SERVER", "System",
                    "\n=== Previous Chat Messages ===\n", Message.MessageType.HISTORY);
            sendMessage(historyHeader);

            // 限制显示最近20条消息
            int startIndex = Math.max(0, history.size() - 20);
            for (int i = startIndex; i < history.size(); i++) {
                sendMessage(history.get(i));
            }

            if (history.size() > 20) {
                Message truncateMsg = new Message("SERVER", "System",
                        "... showing only the last 20 messages ...\n", Message.MessageType.HISTORY);
                sendMessage(truncateMsg);
            }

            Message historyFooter = new Message("SERVER", "System",
                    "=== End of History ===\n", Message.MessageType.HISTORY);
            sendMessage(historyFooter);
        }
    }

    /**
     * 发送消息给客户端
     */
    public void sendMessage(Message message) {
        if (!running || message == null || out == null) {
            return;
        }

        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            System.err.println("[Server] Failed to send message to user " + userId + ": " + e.getMessage());
            running = false;
        }
    }

    /**
     * 清理资源
     */
    private void cleanup() {
        running = false;

        try {
            // 广播离开通知（如果用户已注册）
            if (username != null && ChatServer.isClientConnected(userId)) {
                Message leaveMessage = new Message(userId, username, "left the chat room", Message.MessageType.LEAVE);
                ChatServer.broadcast(leaveMessage, userId);
            }

            // 从在线列表中移除
            ChatServer.removeClient(userId);

            // 关闭资源
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

            System.out.println("[Server] Cleaned up resources for user: " + username + " (" + userId + ")");

        } catch (Exception e) {
            System.err.println("[Server] Error during cleanup for user " + userId + ": " + e.getMessage());
        }
    }

    /**
     * 检查处理器是否在运行
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * 优雅停止处理器
     */
    public void stop() {
        running = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // 忽略关闭异常
        }
    }
}