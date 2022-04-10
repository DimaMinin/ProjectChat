package com.Minin.client.controllers;

import com.Minin.client.ClientChat;
import com.Minin.client.model.Network;
import com.Minin.client.model.ReadCommandListener;
import com.Minin.client.server.ChatHistory;
import com.Minin.clientserver.Command;
import com.Minin.clientserver.CommandType;
import com.Minin.clientserver.commands.ClientMessageCommandData;
import com.Minin.clientserver.commands.UpdateUserListCommandData;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;


public class ClientController {

    private static final int LAST_HISTORY_ROWS_NUMBER = 100;

    @FXML private TextArea textArea;
    @FXML private TextField textField;
    @FXML private Button sendButton;
    @FXML public ListView<String> userList;

    private ClientChat application;
    private ChatHistory chatHistoryService;

    public void sendMessage() {
        String message = textField.getText().trim();

        if (message.isEmpty()) {
            textField.clear();
            return;
        }

        String sender = null;
        if (!userList.getSelectionModel().isEmpty()) {
            sender = userList.getSelectionModel().getSelectedItem();
        }

        try {
            if (sender != null) {
                Network.getInstance().sendPrivateMessage(sender, message);
            } else {
                System.out.println("ClientController Network.getInstance().sendMessage(message);");
                Network.getInstance().sendMessage(message);
            }

        } catch (IOException e) {
            application.showErrorDialog("Ошибка передачи данных по сети");
        }

        appendMessageToChat("Я", message);
    }

    public void createChatHistory() {
        this.chatHistoryService = new ChatHistory(Network.getInstance().getCurrentUsername());
        chatHistoryService.init();
    }

    private void appendMessageToChat(String sender, String message) {
        String currentText = textArea.getText();

        textArea.appendText(DateFormat.getDateTimeInstance().format(new Date()));
        textArea.appendText(System.lineSeparator());

        if (sender != null) {
            textArea.appendText(sender + ":");
            textArea.appendText(System.lineSeparator());
        }

        textArea.appendText(message);
        textArea.appendText(System.lineSeparator());
        textArea.appendText(System.lineSeparator());
        textField.setFocusTraversable(true);
        textField.clear();

        String newMessage = textArea.getText(currentText.length(), textArea.getLength());
        chatHistoryService.appendText(newMessage);
    }


    public void setApplication(ClientChat application) {
        this.application = application;
    }

    public void initializeMessageHandler() {
        Network.getInstance().addReadMessageListener(new ReadCommandListener() {
            @Override
            public void processReceivedCommand(Command command) {
                if (chatHistoryService == null) {
                    createChatHistory();
                    loadChatHistory();
                }
                if (command.getType() == CommandType.CLIENT_MESSAGE) {
                    ClientMessageCommandData data = (ClientMessageCommandData) command.getData();
                    appendMessageToChat(data.getSender(), data.getMessage());
                } else if (command.getType() == CommandType.UPDATE_USER_LIST) {
                    UpdateUserListCommandData data = (UpdateUserListCommandData) command.getData();
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            userList.setItems(FXCollections.observableList(data.getUsers()));
                        }
                    });
                }
            }
        });
    }

    public void closeChat(ActionEvent actionEvent) {
        chatHistoryService.close();
        ClientChat.INSTANCE.getChatStage().close();
    }

    private void loadChatHistory() {
        String rows = chatHistoryService.loadLastRows2(LAST_HISTORY_ROWS_NUMBER);
        textArea.clear();
        textArea.setText(rows);
    }

}
