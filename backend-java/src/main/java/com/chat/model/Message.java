package com.chat.model;

import java.util.List;
import java.util.Map;

public class Message {
    private String type; // EVENTOS: AUTH, JOIN, LEAVE, MESSAGE, CHANNELS_UPDATE, TYPING, PRIVATE_MESSAGE, DELETE_MESSAGE, SYNC_AVATARS
    private String username;
    private String channel;
    private String content;
    private List<String> activeUsers;
    private List<String> availableChannels;
    private String targetUser;
    private String imageBase64;
    private String messageId;
    private Map<String, String> userAvatars;
    private long timestamp;

    public Message() {}

    public Message(String type, String username, String channel, String content) {
        this.type = type;
        this.username = username;
        this.channel = channel;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public Map<String, String> getUserAvatars() { return userAvatars; }
    public void setUserAvatars(Map<String, String> userAvatars) { this.userAvatars = userAvatars; }

    public String getTargetUser() { return targetUser; }
    public void setTargetUser(String targetUser) { this.targetUser = targetUser; }

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public List<String> getActiveUsers() { return activeUsers; }
    public void setActiveUsers(List<String> activeUsers) { this.activeUsers = activeUsers; }

    public List<String> getAvailableChannels() { return availableChannels; }
    public void setAvailableChannels(List<String> availableChannels) { this.availableChannels = availableChannels; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
