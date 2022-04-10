package com.Minin.client.model;

import com.Minin.clientserver.Command;

public interface ReadCommandListener {

    void processReceivedCommand(Command command);

}