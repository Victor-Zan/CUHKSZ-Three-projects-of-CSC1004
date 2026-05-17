
package server;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 消息类 - 聊天室中传递的消息对象
 * 实现Serializable接口以便通过网络传输
 */
public class Message implements Serializable {
    private String userId;          // 用户ID
    private String username;        // 用户名
    private String content;         // 消息内容
    private LocalDateTime timestamp;// 时间戳
    private MessageType type;       // 消息类型

    /**
     * 消息类型枚举
     */
    public enum MessageType {
        TEXT,       // 普通文本消息
        JOIN,       // 用户加入通知
        LEAVE,      // 用户离开通知
        USER_LIST,  // 用户列表
        HISTORY     // 历史消息
    }

    /**
     * 构造函数
     */
    public Message(String userId, String username, String content, MessageType type) {
        this.userId = userId;
        this.username = username;
        this.content = content;
        this.type = type;
        this.timestamp = LocalDateTime.now(); // 自动设置当前时间
    }

    // Getter方法
    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public MessageType getType() {
        return type;
    }

    /**
     * 格式化消息为字符串显示
     */
    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String time = timestamp.format(formatter);

        // 根据消息类型格式化输出
        switch (type) {
            case JOIN:
                return String.format("[%s] System: User %s (%s) joined the chat room",
                        time, username, userId);
            case LEAVE:
                return String.format("[%s] System: User %s (%s) left the chat room",
                        time, username, userId);
            case TEXT:
                return String.format("[%s] %s (%s): %s",
                        time, username, userId, content);
            case USER_LIST:
                return String.format("[%s] System: %s", time, content);
            case HISTORY:
                return content;
            default:
                return content;
        }
    }
}