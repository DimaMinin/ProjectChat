package com.Minin.server.chat;


import com.Minin.clientserver.Command;
import com.Minin.server.chat.auth.IAuthService;
import com.Minin.server.chat.auth.PersistentDbAuthService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyServer {

    private ExecutorService executorService;
    private final List<ClientHandler> clients = new ArrayList<>();
    private IAuthService authService;
    private static final Logger LOGGER = LogManager.getLogger(MyServer.class);

    public IAuthService getAuthService() {
        return authService;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void start(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            LOGGER.info("Server has been started");
            authService = createAuthService();
            authService.start();
            executorService = Executors.newCachedThreadPool();
            while (true) {
                waitAndProcessClientConnection(serverSocket);
            }

        } catch (IOException e) {
            LOGGER.error("Failed to bind port " + port);
            e.printStackTrace();
        } finally {
            if (authService != null) {
                authService.stop();
            }
            if (executorService != null) {
                executorService.shutdown();
            }
        }
    }

    private IAuthService createAuthService() {
        return new PersistentDbAuthService();
    }

    private void waitAndProcessClientConnection(ServerSocket serverSocket) throws IOException {
        LOGGER.info("Waiting for new client connection");
        Socket clientSocket = serverSocket.accept();
        LOGGER.info("Client has been connected");
        ClientHandler clientHandler = new ClientHandler(this, clientSocket);
        clientHandler.handle();
    }

    public synchronized boolean isUsernameBusy(String username) {
        for (ClientHandler client : clients) {
            if (client.getUserName().equals(username)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void broadcastMessage(String message, ClientHandler sender) throws IOException {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendCommand(Command.clientMessageCommand(sender.getUserName(), message));
            }
        }
    }

    public synchronized void sendPrivateMessage(ClientHandler sender, String recipient, String privateMessage) throws IOException {
        for (ClientHandler client : clients) {
            if (client != sender && client.getUserName().equals(recipient)) {
                client.sendCommand(Command.clientMessageCommand(sender.getUserName(), privateMessage));
                break;
            }
        }
    }

    public synchronized void subscribe(ClientHandler clientHandler) throws IOException {
        clients.add(clientHandler);
        notifyClientUserListUpdated();
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) throws IOException {
        clients.remove(clientHandler);
        notifyClientUserListUpdated();
    }

    public void notifyClientUserListUpdated() throws IOException {
        List<String> userListOnline = new ArrayList<>();

        for (ClientHandler client : clients) {
            userListOnline.add(client.getUserName());
        }

        for (ClientHandler client : clients) {
            client.sendCommand(Command.updateUserListCommand(userListOnline));
        }
    }
}
