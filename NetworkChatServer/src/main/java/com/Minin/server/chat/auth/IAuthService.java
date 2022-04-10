package com.Minin.server.chat.auth;

public interface IAuthService {

    default void start() {

    }

    String getUserNameByLoginAndPassword(String login, String password);

    default void stop() {

    }

//    void updateUsername(String currentUsername, String newUsername);
}