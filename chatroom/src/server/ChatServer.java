
package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * 聊天室服务器主类
 * 负责监听客户端连接，管理所有客户端处理器
 */
public class ChatServer {
    private static final int PORT = 12345;                     // 服务器端口
    private static final String HISTORY_FILE = "chat_history.txt"; // 历史记录文件
    private static final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static final List<Message> chatHistory = Collections.synchronizedList(new ArrayList<>());
    private static int userCounter = 0;  // 用户计数器，用于生成唯一ID

    /**
     * 服务器主方法
     */
    public static void main(String[] args) {
        System.out.println("=== Chat Room Server Starting ===");
        System.out.println("Server listening on port " + PORT);

        // 加载聊天历史记录
        loadChatHistory();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started successfully! Waiting for clients...\n");

            // 主循环，持续接受客户端连接
            while (true) {
                Socket clientSocket = serverSocket.accept();
                String userId = generateUserId();
                System.out.println("New connection accepted, assigned User ID: " + userId);

                // 创建客户端处理器，但不立即添加到在线列表
                ClientHandler clientHandler = new ClientHandler(clientSocket, userId);
                new Thread(clientHandler).start();
            }

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 生成5位用户ID
     */
    private static String generateUserId() {
        userCounter++;
        return String.format("%05d", userCounter); // 格式化为5位数字，如00001
    }

    /**
     * 添加客户端到在线列表（由ClientHandler调用）
     */
    public static synchronized void addClient(String userId, ClientHandler handler) {
        if (handler == null || userId == null) {
            return;
        }
        clients.put(userId, handler);
        System.out.println("[Server] Added user " + handler.getUsername() + " (" + userId + ") to online list");
    }

    /**
     * 广播消息给所有在线用户（除了指定用户）
     */
    public static void broadcast(Message message, String excludeUserId) {
        if (message == null) {
            return;
        }

        // 保存文本消息到历史记录
        if (message.getType() == Message.MessageType.TEXT) {
            chatHistory.add(message);
            saveMessageToHistory(message);
        }

        // 广播给所有在线客户端
        List<String> disconnectedUsers = new ArrayList<>();

        synchronized (clients) {
            for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
                String clientId = entry.getKey();
                ClientHandler handler = entry.getValue();

                // 跳过排除的用户
                if (excludeUserId != null && clientId.equals(excludeUserId)) {
                    continue;
                }

                // 检查处理器是否正常运行
                if (handler == null || !handler.isRunning()) {
                    disconnectedUsers.add(clientId);
                    continue;
                }

                // 发送消息
                handler.sendMessage(message);
            }
        }

        // 清理断开连接的用户
        for (String disconnectedId : disconnectedUsers) {
            removeClient(disconnectedId);
        }
    }

    /**
     * 从在线列表中移除客户端
     */
    public static synchronized void removeClient(String userId) {
        if (userId == null) {
            return;
        }

        ClientHandler handler = clients.get(userId);
        if (handler != null) {
            String username = handler.getUsername();
            clients.remove(userId);
            System.out.println("[Server] User " + username + " (" + userId + ") removed from online list");
            System.out.println("[Server] Current online users: " + clients.size());
        }
    }

    /**
     * 检查客户端是否在线
     */
    public static boolean isClientConnected(String userId) {
        ClientHandler handler = clients.get(userId);
        return handler != null && handler.isRunning();
    }

    /**
     * 获取在线用户列表（排除指定用户）
     */
    public static List<String> getOnlineUsers(String excludeUserId) {
        List<String> userList = new ArrayList<>();

        synchronized (clients) {
            for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
                String userId = entry.getKey();
                ClientHandler handler = entry.getValue();

                // 跳过排除的用户和不正常的处理器
                if (excludeUserId != null && userId.equals(excludeUserId)) {
                    continue;
                }
                if (handler == null || !handler.isRunning()) {
                    continue;
                }

                userList.add(userId + ":" + handler.getUsername());
            }
        }

        return userList;
    }

    /**
     * 获取聊天历史记录
     */
    public static List<Message> getChatHistory() {
        return new ArrayList<>(chatHistory);
    }

    /**
     * 获取当前在线用户数
     */
    public static int getClientCount() {
        return clients.size();
    }

    /**
     * 加载聊天历史记录
     */
    private static void loadChatHistory() {
        File file = new File(HISTORY_FILE);
        if (!file.exists()) {
            System.out.println("[Server] No previous chat history found.");
            return;
        }

        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                count++;
                // 这里可以解析历史记录，简化处理只统计行数
            }
            System.out.println("[Server] Loaded " + count + " lines of chat history from " + HISTORY_FILE);

        } catch (IOException e) {
            System.err.println("[Server] Failed to load chat history: " + e.getMessage());
        }
    }

    /**
     * 保存消息到历史记录文件
     */
    private static void saveMessageToHistory(Message message) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(HISTORY_FILE, true))) {
            writer.write(message.toString());
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.err.println("[Server] Failed to save chat history: " + e.getMessage());
        }
    }
}