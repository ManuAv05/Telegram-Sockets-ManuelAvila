package com.chat.server;

import com.chat.model.Message;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer extends WebSocketServer {

    private final Gson gson = new Gson();
    private final DatabaseManager db = new DatabaseManager();
    
    // Map para guardar a los usuarios por su sesión
    private final Map<WebSocket, String> userSessions = new ConcurrentHashMap<>();
    // Map para guardar el canal al que pertenece cada usuario (username -> channel)
    private final Map<String, String> userChannels = new ConcurrentHashMap<>();
    
    // Lista de canales por defecto
    private final Set<String> defaultChannels = new HashSet<>(Arrays.asList("general", "random", "dudas", "memes"));

    public ChatServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Nueva conexión entrante: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Conexión cerrada: " + conn.getRemoteSocketAddress());
        String username = userSessions.get(conn);
        if (username != null) {
            String channel = userChannels.get(username);
            userSessions.remove(conn);
            userChannels.remove(username);
            
            // Avisar a todos en el canal que se fue
            Message leaveMsg = new Message("LEAVE", "Server", channel, username + " se ha desconectado.");
            broadcastToChannel(channel, leaveMsg);
            broadcastUsersInChannel(channel);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            Message msg = gson.fromJson(message, Message.class);

            // Permitir AUTH a cualquiera, pero el resto exige estar en userSessions
            if (!msg.getType().equals("AUTH") && !userSessions.containsKey(conn)) {
                return; // Ignorar paquetes basura de no autenticados
            }

            switch (msg.getType()) {
                case "AUTH":
                    handleAuth(conn, msg);
                    break;
                case "JOIN":
                    handleJoin(conn, msg);
                    break;
                case "MESSAGE":
                    handleMessage(conn, msg);
                    break;
                case "LEAVE_CHANNEL":
                    handleLeaveChannel(conn, msg);
                    break;
                case "TYPING":
                    handleTyping(conn, msg);
                    break;
                case "PRIVATE_MESSAGE":
                    handlePrivateMessage(conn, msg);
                    break;
                case "DELETE_MESSAGE":
                    handleDeleteMessage(conn, msg);
                    break;
                default:
                    System.out.println("Tipo de mensaje desconocido: " + msg.getType());
            }
        } catch (Exception e) {
            System.err.println("Error procesando mensaje: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if(ex.getMessage() != null && !ex.getMessage().contains("Connection reset by peer")) {
            System.err.println("Un error ha ocurrido: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        System.out.println("Servidor WebSocket iniciado en puerto: " + getPort());
        setConnectionLostTimeout(100);
    }

    // --- LÓGICA DE EVENTOS ---
    
    private void handleAuth(WebSocket conn, Message msg) {
        String username = msg.getUsername();
        String password = msg.getContent(); 
        String avatar = msg.getImageBase64();
        
        boolean success = db.authenticate(username, password, avatar);
        if (success) {
            userSessions.put(conn, username);
            
            Message authOk = new Message("AUTH_SUCCESS", "Server", "", "Autenticado con éxito");
            conn.send(gson.toJson(authOk));
            
            // Sincronizar todos los avatares para todos en tiempo real
            Message syncMsg = new Message("SYNC_AVATARS", "Server", "", "");
            syncMsg.setUserAvatars(db.getAllAvatars());
            String synString = gson.toJson(syncMsg);
            for (WebSocket c : userSessions.keySet()) {
                c.send(synString);
            }
            
            sendChannels(conn);
        } else {
            Message authErr = new Message("AUTH_ERROR", "Server", "", "Contraseña incorrecta o usuario inválido");
            conn.send(gson.toJson(authErr));
        }
    }
    
    private void handleJoin(WebSocket conn, Message msg) {
        String username = msg.getUsername();
        String channel = msg.getChannel();
        
        // Si el usuario ya estaba en otro canal, avisamos que sale
        String oldChannel = userChannels.get(username);
        if(oldChannel != null && !oldChannel.equals(channel)) {
            Message leaveMsg = new Message("LEAVE", "Server", oldChannel, username + " cambió a otro canal.");
            broadcastToChannel(oldChannel, leaveMsg);
            userChannels.remove(username);
            broadcastUsersInChannel(oldChannel);
        }

        userChannels.put(username, channel);

        // Notificar que se unió
        Message joinResponse = new Message("JOIN", "Server", channel, username + " se ha unido al canal.");
        broadcastToChannel(channel, joinResponse);
        broadcastUsersInChannel(channel);
        
        // --- NOVEDAD FASE 2: Enviar historial almacenado a este usuario en concreto ---
        List<Message> history = db.getChannelHistory(channel, 50);
        for (Message historyMsg : history) {
            conn.send(gson.toJson(historyMsg));
        }
    }

    private void handleMessage(WebSocket conn, Message msg) {
        String username = userSessions.get(conn);
        String channel = userChannels.get(username);

        if (username != null && channel != null && channel.equals(msg.getChannel())) {
            // Guardar en SQLite (Fase 2)
            db.saveMessage(msg);
            // Reenviar mensaje a toda la sala
            broadcastToChannel(channel, msg);
        }
    }
    
    private void handleLeaveChannel(WebSocket conn, Message msg) {
        String username = userSessions.get(conn);
        if(username != null) {
            String channel = userChannels.get(username);
            Message leaveMsg = new Message("LEAVE", "Server", channel, username + " ha salido.");
            broadcastToChannel(channel, leaveMsg);
            userChannels.remove(username); 
            broadcastUsersInChannel(channel);
        }
    }

    private void handleTyping(WebSocket conn, Message msg) {
        String username = userSessions.get(conn);
        String channel = userChannels.get(username);

        if (username != null && channel != null && channel.equals(msg.getChannel())) {
            broadcastToChannel(channel, msg);
        }
    }

    private void handleDeleteMessage(WebSocket conn, Message msg) {
        String username = userSessions.get(conn);
        if (username != null && msg.getMessageId() != null) {
            boolean deleted = db.deleteMessage(msg.getMessageId(), username);
            if (deleted) {
                // Notificar eliminación a todo el canal
                String channel = msg.getChannel() != null ? msg.getChannel() : userChannels.get(username);
                Message delMsg = new Message("DELETE_MESSAGE", username, channel, "");
                delMsg.setMessageId(msg.getMessageId());
                broadcastToChannel(channel, delMsg);
            }
        }
    }

    private void handlePrivateMessage(WebSocket conn, Message msg) {
        String sender = userSessions.get(conn);
        String target = msg.getTargetUser();
        
        if (sender != null && target != null) {
            String jsonMsg = gson.toJson(msg);
            for (Map.Entry<WebSocket, String> entry : userSessions.entrySet()) {
                String u = entry.getValue();
                if (u.equals(sender) || u.equals(target)) {
                    entry.getKey().send(jsonMsg);
                }
            }
        }
    }

    private void broadcastToChannel(String channel, Message msg) {
        String jsonMsg = gson.toJson(msg);
        for (Map.Entry<WebSocket, String> entry : userSessions.entrySet()) {
            WebSocket conn = entry.getKey();
            String userConfChannel = userChannels.get(entry.getValue());
            if (userConfChannel != null && userConfChannel.equals(channel)) {
                conn.send(jsonMsg);
            }
        }
    }

    private void broadcastUsersInChannel(String channel) {
        List<String> usersInChannel = new ArrayList<>();
        for (Map.Entry<String, String> entry : userChannels.entrySet()) {
            if (entry.getValue().equals(channel)) {
                usersInChannel.add(entry.getKey());
            }
        }
        Message updateUsersMsg = new Message("USERS_UPDATE", "Server", channel, "");
        updateUsersMsg.setActiveUsers(usersInChannel);
        broadcastToChannel(channel, updateUsersMsg);
    }

    private void sendChannels(WebSocket conn) {
        Message channelsMsg = new Message("CHANNELS_UPDATE", "Server", "", "");
        channelsMsg.setAvailableChannels(new ArrayList<>(defaultChannels));
        conn.send(gson.toJson(channelsMsg));
    }

    public static void main(String[] args) {
        int port = 8081; 
        try {
            ChatServer server = new ChatServer(port);
            server.start();
            System.out.println("Servidor configurado en puerto " + port + ". Esperando clientes...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
